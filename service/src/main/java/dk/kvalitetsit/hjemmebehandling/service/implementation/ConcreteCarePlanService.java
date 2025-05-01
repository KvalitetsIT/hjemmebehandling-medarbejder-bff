package dk.kvalitetsit.hjemmebehandling.service.implementation;

import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.ExtensionMapper;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.repository.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
import dk.kvalitetsit.hjemmebehandling.service.validation.AccessValidatingService;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ConcreteCarePlanService extends AccessValidatingService {

    private static final Logger logger = LoggerFactory.getLogger(ConcreteCarePlanService.class);

    private final CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository;
    private final PatientRepository<PatientModel, CarePlanStatus> patientRepository;
    private final QuestionnaireRepository<QuestionnaireModel> questionnaireRepository;
    private final PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository;
    private final QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository;
    private final OrganizationRepository<Organization> organizationRepository;

    private final DateProvider dateProvider;
    private final CustomUserClient customUserService;

    private UserContextProvider userContextProvider;
    @Value("${patientidp.api.url}")
    private String patientidpApiUrl;

    public ConcreteCarePlanService(
            DateProvider dateProvider,
            AccessValidator accessValidator,
            CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository,
            PatientRepository<PatientModel, CarePlanStatus> patientRepository,
            CustomUserClient customUserService,
            QuestionnaireRepository<QuestionnaireModel> questionnaireRepository,
            PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository, QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository, OrganizationRepository<Organization> organizationRepository
    ) {
        super(accessValidator);
        this.carePlanRepository = carePlanRepository;
        this.dateProvider = dateProvider;
        this.patientRepository = patientRepository;
        this.customUserService = customUserService;
        this.questionnaireRepository = questionnaireRepository;
        this.planDefinitionRepository = planDefinitionRepository;
        this.questionnaireResponseRepository = questionnaireResponseRepository;
        this.organizationRepository = organizationRepository;
    }

    private static List<QualifiedId.QuestionnaireId> getIdsOfRemovedQuestionnaires(List<QualifiedId.QuestionnaireId> questionnaireIds, CarePlanModel carePlan) {
        throw new NotImplementedException();
    }

    public QualifiedId.CarePlanId createCarePlan(CarePlanModel carePlan) throws ServiceException, AccessValidationException {
        CPR cpr = carePlan.patient().cpr();
        var patient = patientRepository.fetch(cpr);

        PatientModel updatedPatient = carePlan.patient();

        if (updatedPatient.primaryContact() != null) {
            var updatedPrimaryContact = PrimaryContactModel.Builder
                    .from(updatedPatient.primaryContact())
                    .organisation(organizationRepository.getOrganizationId())
                    .build();

            updatedPatient = new PatientModel(
                    updatedPatient.id(),
                    updatedPatient.name(),
                    updatedPatient.cpr(),
                    updatedPatient.contactDetails(),
                    updatedPrimaryContact,
                    updatedPatient.additionalRelativeContactDetails(),
                    updatedPatient.customUserId(),
                    updatedPatient.customUserName()
            );
        }

        if (patient.isPresent()) {
            QualifiedId.PatientId patientId = patient.get().id();
            boolean onlyActiveCarePlans = true;
            var carePlanResult = carePlanRepository.fetchCarePlansByPatientId(patientId, onlyActiveCarePlans);

            if (!carePlanResult.isEmpty()) {
                throw new ServiceException(String.format(
                        "Could not create careplan for cpr %s: Another active careplan already exists!", cpr),
                        ErrorKind.BAD_REQUEST,
                        ErrorDetails.CAREPLAN_EXISTS
                );
            }

        }

        // Validate referenced questionnaires
        if (carePlan.questionnaires() != null && !carePlan.questionnaires().isEmpty()) {

            List<QualifiedId.QuestionnaireId> questionnaireIds = carePlan.questionnaires().stream()
                    .map(qw -> qw.questionnaire().id())
                    .toList();

            List<QuestionnaireModel> questionnaires = questionnaireRepository.fetch(questionnaireIds);
            validateAccess(questionnaires);
        }

        // Validate referenced plan definitions
        if (carePlan.planDefinitions() != null && !carePlan.planDefinitions().isEmpty()) {
            List<QualifiedId.PlanDefinitionId> planDefinitionIds = carePlan.planDefinitions().stream()
                    .map(PlanDefinitionModel::id)
                    .toList();

            List<PlanDefinitionModel> planDefinitions = planDefinitionRepository.fetch(planDefinitionIds);
            validateAccess(planDefinitions);
        }

        var now = dateProvider.now();
        var today = dateProvider.today().toInstant();

        // Update satisfiedUntil for each questionnaire wrapper
        List<QuestionnaireWrapperModel> updatedQuestionnaires = carePlan.questionnaires().stream()
                .map(qw -> {
                    FrequencyEnumerator enumerator = new FrequencyEnumerator(qw.frequency());
                    Instant satisfiedUntil = enumerator.getSatisfiedUntilForInitialization(now);
                    return new QuestionnaireWrapperModel(
                            qw.questionnaire(),
                            qw.frequency(),
                            satisfiedUntil,
                            qw.thresholds()
                    );
                })
                .toList();

        CarePlanModel updatedCarePlan = new CarePlanModel(
                null, // id must be null for creation
                carePlan.organizationId(),
                carePlan.title(),
                CarePlanStatus.ACTIVE,
                today,
                today,
                null,
                updatedPatient,
                updatedQuestionnaires,
                carePlan.planDefinitions(),
                carePlan.departmentName(),
                null // will be computed below
        );

        Instant finalSatisfiedUntil = getRefreshedFrequencyTimestampForCarePlan(updatedCarePlan);
        updatedCarePlan = new CarePlanModel(
                updatedCarePlan.id(),
                updatedCarePlan.organizationId(),
                updatedCarePlan.title(),
                updatedCarePlan.status(),
                updatedCarePlan.created(),
                updatedCarePlan.startDate(),
                updatedCarePlan.endDate(),
                updatedCarePlan.patient(),
                updatedCarePlan.questionnaires(),
                updatedCarePlan.planDefinitions(),
                updatedCarePlan.departmentName(),
                finalSatisfiedUntil
        );

        try {
            if (patient.isPresent()) {
                patientRepository.update(patient.get());
                return carePlanRepository.save(updatedCarePlan);
            }

            if (patientidpApiUrl != null && !patientidpApiUrl.isEmpty()) {
                try {
                    Optional<CustomUserResponseDto> customUserResponse = customUserService
                            .createUser(updatedPatient);

                    if (customUserResponse.isPresent()) {
                        CustomUserResponseDto dto = customUserResponse.get();
                        updatedPatient = PatientModel.Builder.from(updatedPatient)
                                .customUserId(dto.getId())
                                .customUserName(dto.getUsername())
                                .build();
                    }
                } catch (Exception e) {
                    throw new ServiceException(
                            String.format("Could not create customlogin for patient with id %s!", updatedPatient.id()),
                            ErrorKind.BAD_GATEWAY,
                            ErrorDetails.CUSTOMLOGIN_UNKNOWN_ERROR
                    );
                }
            }

            return carePlanRepository.save(updatedCarePlan, updatedPatient);

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error saving CarePlan", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }

    }

    public CarePlanModel completeCarePlan(QualifiedId.CarePlanId carePlanId) throws ServiceException {
        Optional<CarePlanModel> result = carePlanRepository.fetch(carePlanId);

        if (result.isEmpty()) {
            throw new ServiceException(String.format("Could not lookup careplan with id %s!", carePlanId.toString()), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_DOES_NOT_EXIST);
        }

        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        var questionnaireResponsesStillNotExamined = questionnaireResponseRepository.fetch(statuses, carePlanId);
        if (!questionnaireResponsesStillNotExamined.isEmpty()) {
            throw new ServiceException(String.format("Careplan with id %s still has unhandled questionnaire-responses!", carePlanId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_HAS_UNHANDLED_QUESTIONNAIRERESPONSES);
        }

//        if (ExtensionMapper.extractCarePlanSatisfiedUntil(result.get().extension()).isBefore(Instant.now())) {
//            throw new ServiceException(String.format("Careplan with id %s is missing scheduled responses!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_IS_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES);
//        }

        CarePlanModel completedCarePlan = CarePlanModel.Builder
                .from(result.get())
                .status(CarePlanStatus.COMPLETED)
                .build();

        carePlanRepository.update(completedCarePlan);


        return completedCarePlan; // for auditlog
    }

    public List<CarePlanModel> getCarePlansWithFilters(boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        Instant pointInTime = dateProvider.now();
        List<CarePlanModel> careplans = carePlanRepository.fetch(pointInTime, onlyActiveCarePlans, onlyUnSatisfied);

        // Get the related questionnaire-resources
        List<QualifiedId.QuestionnaireId> questionnaireIds = new ArrayList<>();
        questionnaireIds.addAll(extractQuestionnaireIdsFromCarePlan(careplans));
        questionnaireIds.addAll(extractQuestionnaireIdsFromPlanDefinition(careplans.getFirst().planDefinitions()));
        List<QuestionnaireModel> questionnaireResult = questionnaireRepository.fetch(questionnaireIds);

        var orgId = organizationRepository.getOrganizationId();
        // Map and sort the resources

        return careplans.stream()
                .sorted((careplan1, careplan2) -> {
                    String name1 = String.join(" ", careplan1.patient().name().given().getFirst(), careplan1.patient().name().family());
                    String name2 = String.join(" ", careplan2.patient().name().given().getFirst(), careplan2.patient().name().family());
                    return name1.compareTo(name2);
                })
                .toList();
    }

    public List<CarePlanModel> getCarePlansWithFilters(CPR cpr, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        Instant pointInTime = dateProvider.now();

        Optional<PatientModel> patient = patientRepository.fetch(cpr);


        List<CarePlanModel> lookupResult = carePlanRepository.fetch(patient.get().id(), pointInTime, onlyActiveCarePlans, onlyUnSatisfied);
        if (lookupResult.isEmpty()) {
            return List.of();
        }
        var orgId = organizationRepository.getOrganizationId();
        // Map and sort the resources

        return lookupResult.stream()
                .sorted((careplan1, careplan2) -> {
                    String name1 = String.join(" ", careplan1.patient().name().given().getFirst(), careplan1.patient().name().family());
                    String name2 = String.join(" ", careplan2.patient().name().given().getFirst(), careplan2.patient().name().family());
                    return name1.compareTo(name2);
                })
                .toList();
    }

    public List<CarePlanModel> getCarePlansWithFilters(boolean onlyActiveCarePlans, boolean onlyUnSatisfied, Pagination pagination) throws ServiceException {
        return pageResponses(getCarePlansWithFilters(onlyActiveCarePlans, onlyUnSatisfied), pagination);
    }

    public List<CarePlanModel> getCarePlansWithFilters(CPR cpr, boolean onlyActiveCarePlans, boolean onlyUnSatisfied, Pagination pagination) throws ServiceException {
        return pageResponses(getCarePlansWithFilters(cpr, onlyActiveCarePlans, onlyUnSatisfied), pagination);
    }

    public Optional<CarePlanModel> getCarePlanById(QualifiedId.CarePlanId carePlanId) throws ServiceException, AccessValidationException {
        return carePlanRepository.fetch(carePlanId);
    }

    public CarePlanModel resolveAlarm(QualifiedId.CarePlanId carePlanId, QualifiedId.QuestionnaireId questionnaireId) throws ServiceException, AccessValidationException {
        // Get the careplan
        Optional<CarePlanModel> carePlanResult = carePlanRepository.fetch(carePlanId);

        CarePlanModel carePlan = carePlanResult.orElseThrow(() -> new ServiceException(String.format("Could not look up careplan by id %s", carePlanId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_DOES_NOT_EXIST));

        // Validate access
        //validateAccess(carePlan);

        // Check that the 'satisfiedUntil'-timestamp is indeed in the past, throw an exception if not.
        // CarePlanModel carePlanModel = fhirMapper.mapCarePlan(carePlan,null, fhirClient.getOrganizationId());
        var currentPointInTime = dateProvider.now();
        if (currentPointInTime.isBefore(carePlan.satisfiedUntil())) {
            throw new ServiceException(String.format("Could not resolve alarm for careplan %s! The satisfiedUntil-timestamp was in the future.", carePlanId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_ALREADY_FULFILLED);
        }

        // Recompute the 'satisfiedUntil'-timestamps
        carePlan = CarePlanModel.Builder
                .from(carePlan)
                .satisfiedUntil(recomputeFrequencyTimestamps(carePlan, questionnaireId, currentPointInTime))
                .build();

        // Save the updated carePlan
        carePlanRepository.update(carePlan);
        return carePlan; // for auditlog
    }

    public CarePlanModel updateCarePlan(QualifiedId.CarePlanId carePlanId,
                                        List<QualifiedId.PlanDefinitionId> planDefinitionIds,
                                        List<QualifiedId.QuestionnaireId> questionnaireIds,
                                        Map<String, FrequencyModel> frequencies,
                                        PatientDetails patientDetails) throws ServiceException, AccessValidationException {
        // Look up the plan definitions to verify that they exist, throw an exception in case they don't.
        List<PlanDefinitionModel> planDefinitionResult = planDefinitionRepository.fetch(planDefinitionIds);
        if (planDefinitionResult.size() != planDefinitionIds.size()) throw new ServiceException(
                "Could not look up plan definitions to update!",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.PLAN_DEFINITIONS_MISSING_FOR_CAREPLAN
        );

        // Validate that the client is allowed to reference the plan definitions.
        //validateAccess(planDefinitionResult);

        // Look up the questionnaires to verify that they exist, throw an exception in case they don't.
        List<QuestionnaireModel> questionnaireResult = questionnaireRepository.fetch(questionnaireIds);

        if (questionnaireResult.size() != questionnaireIds.size()) throw new ServiceException(
                "Could not look up questionnaires to update!",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.QUESTIONNAIRES_MISSING_FOR_CAREPLAN
        );


        // Validate that the client is allowed to reference the questionnaires.
        //validateAccess(questionnaireResult);

        // Look up the CarePlan, throw an exception in case it does not exist.
        Optional<CarePlanModel> careplanResult = carePlanRepository.fetch(carePlanId);

        if (careplanResult.isEmpty()) {
            throw new ServiceException(
                    String.format("Could not lookup careplan with id %s!", carePlanId),
                    ErrorKind.BAD_REQUEST,
                    ErrorDetails.CAREPLAN_DOES_NOT_EXIST
            );
        }

        var carePlan = careplanResult.get();

        // Validate that the client is allowed to update the carePlan.
        validateAccess(carePlan);


        if (!questionnairesAllowedByPlanDefinitions(planDefinitionResult, questionnaireIds)) throw new ServiceException(
                "Not every questionnaireId could be found in the provided plan definitions.",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.QUESTIONNAIRES_NOT_ALLOWED_FOR_CAREPLAN
        );


        // find evt. fjernede spørgeskemaer
        List<QualifiedId.QuestionnaireId> removedQuestionnaireIds = getIdsOfRemovedQuestionnaires(questionnaireIds, carePlan);

        // tjek om et fjernet spørgeskema har blå alarm
        if (!removedQuestionnaireIds.isEmpty()) {
            boolean removedQuestionnaireWithExceededDeadline = questionnaireHasExceededDeadline(carePlan, removedQuestionnaireIds);
            if (removedQuestionnaireWithExceededDeadline) throw new ServiceException(
                    "Not every questionnaireId could be found in the provided plan definitions.",
                    ErrorKind.BAD_REQUEST,
                    ErrorDetails.PLANDEFINITION_CONTAINS_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES
            );
        }

        // check om der er ubehandlede besvarelser relateret til fjernede spørgeskemaer
        var removedQuestionnaireWithNotExaminedResponses = questionnaireHasUnexaminedResponses(carePlanId, removedQuestionnaireIds);

        if (removedQuestionnaireWithNotExaminedResponses) throw new ServiceException(
                String.format("Careplan with id %s still has unhandled questionnaire-responses!", carePlanId),
                ErrorKind.BAD_REQUEST,
                ErrorDetails.PLANDEFINITION_CONTAINS_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES
        );


        var orgId = organizationRepository.getOrganizationId();
        // Update carePlan

        carePlan = updateCarePlanModel(carePlan, questionnaireIds, frequencies, planDefinitionResult);


        // Update patient
        String patientId = carePlan.patient().id().toString();

        var newPatient = carePlan.patient(); // .orElseThrow(() -> new IllegalStateException(String.format("Could not look up patient with id %s", patientId)));

        //var contactDetails = mergeContacts(oldPatient.getContact(), newPatient.getContact());

        newPatient = PatientModel.Builder.from(newPatient)
                .primaryContact(PrimaryContactModel.Builder
                        .from(newPatient.primaryContact())
                        .organisation(organizationRepository.getOrganizationId())
                        .build()
                ).build();

        newPatient = updatePatientModel(newPatient, patientDetails);

        // Save the updated CarePlan
        carePlanRepository.update(carePlan, newPatient);
        return carePlan; // for auditlogging
    }

    private CarePlanModel updateCarePlanModel(CarePlanModel carePlanModel, List<QualifiedId.QuestionnaireId> questionnaireIds, Map<String, FrequencyModel> frequencies, List<PlanDefinitionModel> planDefinitions) {
        var updatedQuestionnaires = planDefinitions.stream()
                .flatMap(pd -> pd.questionnaires().stream())
                .toList();

        carePlanModel = CarePlanModel.Builder
                .from(carePlanModel)
                .planDefinitions(planDefinitions)
                .questionnaires(buildQuestionnaireWrapperModels(carePlanModel, updatedQuestionnaires, frequencies))
                .satisfiedUntil(getRefreshedFrequencyTimestampForCarePlan(carePlanModel))
                .build();
        return carePlanModel;
    }

    private PatientModel updatePatientModel(PatientModel patientModel, PatientDetails patientDetails) {
        var primaryContactDetails = ContactDetailsModel.Builder
                .from(patientModel.primaryContact().contactDetails())
                .primaryPhone(patientDetails.primaryRelativePrimaryPhone() != null ? patientDetails.primaryRelativePrimaryPhone() : patientDetails.patientPrimaryPhone())
                .secondaryPhone(patientDetails.primaryRelativeSecondaryPhone() != null ? patientDetails.primaryRelativeSecondaryPhone() : patientDetails.patientSecondaryPhone())
                .build();

        var primaryContact = PrimaryContactModel.Builder
                .from(patientModel.primaryContact())
                .name(patientDetails.primaryRelativeName())
                .affiliation(patientDetails.primaryRelativeAffiliation())
                .contactDetails(primaryContactDetails);

        var contactDetails = ContactDetailsModel.Builder
                .from(patientModel.primaryContact().contactDetails())
                .primaryPhone(patientDetails.patientPrimaryPhone())
                .secondaryPhone(patientDetails.patientSecondaryPhone())
                .build();

        return PatientModel.Builder.from(patientModel)
                .contactDetails(contactDetails)
                .primaryContact(primaryContact.build())
                .build();
    }

    public List<QuestionnaireModel> getUnresolvedQuestionnaires(QualifiedId.CarePlanId carePlanId) throws AccessValidationException, ServiceException {
        Optional<CarePlanModel> optional = getCarePlanById(carePlanId);
        if (optional.isEmpty()) throw new ServiceException(
                "Careplan was not found",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.CAREPLAN_DOES_NOT_EXIST
        );
        CarePlanModel carePlanModel = optional.get();
        throw new NotImplementedException();
    }

    public TimeType getDefaultDeadlineTime() throws ServiceException {
        Organization organization = organizationRepository.getCurrentUsersOrganization();
        return ExtensionMapper.extractOrganizationDeadlineTimeDefault(organization.getExtension());
    }

    private boolean questionnaireHasExceededDeadline(CarePlanModel carePlan, List<QualifiedId.QuestionnaireId> questionnaireIds) {
        //return carePlan.getActivity().stream()
        //        .filter(carePlanActivityComponent -> questionnaireIds.contains(carePlanActivityComponent.getDetail().getInstantiatesCanonical().getFirst().getValue()))
        //        .anyMatch(carePlanActivityComponent -> ExtensionMapper.extractActivitySatisfiedUntil(carePlanActivityComponent.getDetail().getExtension()).isBefore(dateProvider.now()));
        throw new NotImplementedException();
    }

    private boolean questionnaireHasUnexaminedResponses(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds) throws ServiceException {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        return questionnaireResponseRepository.fetch(statuses, carePlanId)
                .stream()
                .anyMatch(questionnaireResponse -> questionnaireIds.contains(questionnaireResponse.questionnaireId()));
    }

    private boolean questionnairesAllowedByPlanDefinitions(List<PlanDefinitionModel> planDefinitions, List<QualifiedId.QuestionnaireId> questionnaireIds) {
        var allowedQuestionnaires = planDefinitions.stream().flatMap(pd -> pd.questionnaires().stream().map(qw -> qw.questionnaire().id())).collect(Collectors.toSet());
        var actualQuestionnaires = new HashSet<>(questionnaireIds);

        return allowedQuestionnaires.containsAll(actualQuestionnaires);
    }

    private List<QuestionnaireWrapperModel> buildQuestionnaireWrapperModels(
            CarePlanModel carePlan,
            List<QuestionnaireWrapperModel> updatedQuestionnaires,
            Map<String, FrequencyModel> updatedQuestionnaireIdFrequencies) {

        List<QuestionnaireWrapperModel> currentQuestionnaires = carePlan.questionnaires();

        return updatedQuestionnaires.stream().map(wrapper -> {
            Optional<QuestionnaireWrapperModel> currentQuestionnaire = currentQuestionnaires.stream()
                    .filter(q -> q.questionnaire().id().equals(wrapper.questionnaire().id()))
                    .findFirst();

            FrequencyModel updatedFrequency = updatedQuestionnaireIdFrequencies.get(wrapper.questionnaire().id().toString());
            FrequencyEnumerator frequencyEnumerator = new FrequencyEnumerator(updatedFrequency);

            var builder = QuestionnaireWrapperModel.Builder
                    .from(wrapper)
                    .frequency(updatedFrequency);

            if (currentQuestionnaire.isEmpty()) {
                // Questionnaire is new – initialize satisfiedUntil
                Instant satisfiedUntil = frequencyEnumerator.getSatisfiedUntilForInitialization(dateProvider.now());
                return builder.satisfiedUntil(satisfiedUntil).build();
            }

            // Questionnaire exists already – check if frequency changed
            QuestionnaireWrapperModel existing = currentQuestionnaire.get();
            if (!updatedFrequency.equals(existing.frequency())) {
                Instant currentSatisfiedUntil = existing.satisfiedUntil();
                Instant newSatisfiedUntil = frequencyEnumerator.getSatisfiedUntilForFrequencyChange(dateProvider.now());

                if (currentSatisfiedUntil.isAfter(newSatisfiedUntil)) {
                    QualifiedId.QuestionnaireId questionnaireId = existing.questionnaire().id();
                    List<QuestionnaireResponseModel> result = questionnaireResponseRepository.fetch(carePlan.id(), List.of(questionnaireId));

                    ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("Europe/Copenhagen"));

                    boolean answerFromTodayExists = result.stream()
                            .anyMatch(questionnaireResponse -> {
                                ZonedDateTime answered = ZonedDateTime.ofInstant(null, ZoneId.of("Europe/Copenhagen"));
                                return answered.toLocalDate().equals(now.toLocalDate())
                                        && answered.toLocalTime().isBefore(updatedFrequency.timeOfDay());
                            });

                    if (answerFromTodayExists) {
                        Instant nextSatisfiedUntil = frequencyEnumerator.getSatisfiedUntil(dateProvider.now(), false);
                        return builder.satisfiedUntil(nextSatisfiedUntil).build();
                    } else {
                        return builder.satisfiedUntil(newSatisfiedUntil).build();
                    }
                } else {
                    return builder.satisfiedUntil(newSatisfiedUntil).build();
                }
            }

            // Frequency has not changed – reuse current satisfiedUntil
            return builder.satisfiedUntil(existing.satisfiedUntil()).build();
        }).toList();
    }

    private Instant recomputeFrequencyTimestamps(CarePlanModel carePlanModel, QualifiedId.QuestionnaireId questionnaireId, Instant currentPointInTime) {
        List<QuestionnaireWrapperModel> updatedQuestionnaires = carePlanModel.questionnaires().stream()
                .map(wrapper -> {
                    if (questionnaireId.equals(wrapper.questionnaire().id()) && wrapper.satisfiedUntil().isBefore(currentPointInTime)) {

                        FrequencyEnumerator enumerator = new FrequencyEnumerator(wrapper.frequency());
                        Instant newSatisfiedUntil = enumerator.getSatisfiedUntilForAlarmRemoval(dateProvider.now());

                        return new QuestionnaireWrapperModel(
                                wrapper.questionnaire(),
                                wrapper.frequency(),
                                newSatisfiedUntil,
                                wrapper.thresholds()
                        );
                    }
                    return wrapper;
                })
                .toList();

        CarePlanModel updatedCarePlan = new CarePlanModel(
                carePlanModel.id(),
                carePlanModel.organizationId(),
                carePlanModel.title(),
                carePlanModel.status(),
                carePlanModel.created(),
                carePlanModel.startDate(),
                carePlanModel.endDate(),
                carePlanModel.patient(),
                updatedQuestionnaires,
                carePlanModel.planDefinitions(),
                carePlanModel.departmentName(),
                carePlanModel.satisfiedUntil()
        );

        return getRefreshedFrequencyTimestampForCarePlan(updatedCarePlan);
    }

    private Instant getRefreshedFrequencyTimestampForCarePlan(CarePlanModel carePlanModel) {
        return carePlanModel.questionnaires()
                .stream()
                .map(QuestionnaireWrapperModel::satisfiedUntil)
                .min(Comparator.naturalOrder())
                .orElse(Instant.MAX);

    }

    private List<CarePlanModel> pageResponses(List<CarePlanModel> responses, Pagination pagination) {
        return responses
                .stream()
                .skip((long) (pagination.offset() - 1) * pagination.limit())
                .limit(pagination.limit())
                .toList();
    }

    private List<String> getIdsOfQuestionnairesContainingAlarm(CarePlanModel carePlanModel, CarePlan carePlan) {
        throw new NotImplementedException();
    }

    private List<String> getIdsOfUnresolvedQuestionnaires(QualifiedId.CarePlanId carePlanId) throws ServiceException {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        List<QuestionnaireResponseModel> responses = questionnaireResponseRepository.fetch(statuses, carePlanId);
        return responses.stream()
                .map(QuestionnaireResponseModel::questionnaireId)
                .map(QualifiedId::toString)
                .toList();
    }

    private List<Patient.ContactComponent> mergeContacts(List<Patient.ContactComponent> oldContacts, List<Patient.ContactComponent> newContacts) {
        for (var newContact : newContacts) {
            boolean contactExists = false;
            for (int i = 0; i < oldContacts.size(); i++) {
                var oldContact = oldContacts.get(i);
                if (oldContact.getOrganization().getReference().equals(newContact.getOrganization().getReference())) {
                    oldContacts.set(i, newContact);
                    contactExists = true;
                    break;
                }
            }
            if (!contactExists) {
                oldContacts.add(newContact);
            }
        }
        return oldContacts;
    }


    private static List<QualifiedId.QuestionnaireId> extractQuestionnaireIdsFromCarePlan(List<CarePlanModel> carePlans) {
        return carePlans
                .stream()
                .flatMap(cp -> cp.questionnaires().stream().map(a -> a.questionnaire().id()))
                .toList();
    }

    private static List<QualifiedId.QuestionnaireId> extractQuestionnaireIdsFromPlanDefinition(List<PlanDefinitionModel> planDefinitions) {
        return planDefinitions
                .stream()
                .flatMap(pd -> pd.questionnaires().stream().map(a -> a.questionnaire().id()))
                .toList();
    }


}
