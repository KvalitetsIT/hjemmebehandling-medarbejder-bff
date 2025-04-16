package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.client.Client;
import dk.kvalitetsit.hjemmebehandling.fhir.ExtensionMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CarePlanService extends AccessValidatingService {

    private static final Logger logger = LoggerFactory.getLogger(CarePlanService.class);

    private final Client<
            CarePlanModel,
            PlanDefinitionModel,
            PractitionerModel,
            PatientModel,
            QuestionnaireModel,
            QuestionnaireResponseModel,
            Organization,
            CarePlanStatus> fhirClient;

    private final DateProvider dateProvider;
    private final CustomUserClient customUserService;


    private UserContextProvider userContextProvider;
    @Value("${patientidp.api.url}")
    private String patientidpApiUrl;

    public CarePlanService(
            Client<
                    CarePlanModel,
                    PlanDefinitionModel,
                    PractitionerModel,
                    PatientModel,
                    QuestionnaireModel,
                    QuestionnaireResponseModel,
                    Organization,
                    CarePlanStatus> fhirClient,
            DateProvider dateProvider,
            AccessValidator accessValidator,
            CustomUserClient customUserService
    ) {
        super(accessValidator);

        this.fhirClient = fhirClient;
        this.dateProvider = dateProvider;
        this.customUserService = customUserService;
    }

    private static List<String> getIdsOfRemovedQuestionnaires(List<String> questionnaireIds, CarePlanModel carePlan) {
        throw new NotImplementedException();
    }

    public String createCarePlan(CarePlanModel carePlan) throws ServiceException, AccessValidationException {
        String cpr = carePlan.patient().cpr();
        var patient = fhirClient.lookupPatientByCpr(cpr);

        PatientModel updatedPatient = carePlan.patient();

        if (updatedPatient.primaryContact() != null) {
            var updatedPrimaryContact = PrimaryContactModel.Builder
                    .from(updatedPatient.primaryContact())
                    .organisation(fhirClient.getOrganizationId())
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
            String patientId = patient.get().id().toString();
            boolean onlyActiveCarePlans = true;
            var carePlanResult = fhirClient.fetchCarePlansByPatientId(patientId, onlyActiveCarePlans);

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
            List<QuestionnaireModel> lookupResult = fhirClient.lookupQuestionnairesById(
                    carePlan.questionnaires().stream()
                            .map(qw -> qw.questionnaire().id().toString())
                            .toList()
            );
            validateAccess(lookupResult);
        }

        // Validate referenced plan definitions
        if (carePlan.planDefinitions() != null && !carePlan.planDefinitions().isEmpty()) {
            List<PlanDefinitionModel> lookupResult = fhirClient.lookupPlanDefinitionsById(
                    carePlan.planDefinitions().stream()
                            .map(pd -> pd.id().toString())
                            .toList()
            );
            validateAccess(lookupResult);
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
                fhirClient.updatePatient(patient.get());
                return fhirClient.save(updatedCarePlan);
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

            return fhirClient.saveCarePlan(updatedCarePlan, updatedPatient);

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error saving CarePlan", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }

    }

    public CarePlanModel completeCarePlan(String carePlanId) throws ServiceException {
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        Optional<CarePlanModel> result = fhirClient.fetch(qualifiedId);


        if (result.isEmpty()) {
            throw new ServiceException(String.format("Could not lookup careplan with id %s!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_DOES_NOT_EXIST);
        }

        var questionnaireResponsesStillNotExamined = fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId);
        if (!questionnaireResponsesStillNotExamined.isEmpty()) {
            throw new ServiceException(String.format("Careplan with id %s still has unhandled questionnaire-responses!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_HAS_UNHANDLED_QUESTIONNAIRERESPONSES);
        }

//        if (ExtensionMapper.extractCarePlanSatisfiedUntil(result.get().extension()).isBefore(Instant.now())) {
//            throw new ServiceException(String.format("Careplan with id %s is missing scheduled responses!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_IS_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES);
//        }

        CarePlanModel completedCarePlan = CarePlanModel.Builder
                .from(result.get())
                .status(CarePlanStatus.COMPLETED)
                .build();

        fhirClient.update(completedCarePlan);


        return completedCarePlan; // for auditlog
    }

    public List<CarePlanModel> getCarePlansWithFilters(boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        Instant pointInTime = dateProvider.now();
        List<CarePlanModel> lookupResult = fhirClient.fetchCarePlans(pointInTime, onlyActiveCarePlans, onlyUnSatisfied);
        if (lookupResult.isEmpty()) {
            return List.of();
        }
        var orgId = fhirClient.getOrganizationId();
        // Map and sort the resources

        return lookupResult.stream()
                .sorted((careplan1, careplan2) -> {
                    String name1 = String.join(" ", careplan1.patient().name().given().getFirst(), careplan1.patient().name().family());
                    String name2 = String.join(" ", careplan2.patient().name().given().getFirst(), careplan2.patient().name().family());
                    return name1.compareTo(name2);
                })
                .toList();
    }

    public List<CarePlanModel> getCarePlansWithFilters(String cpr, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        Instant pointInTime = dateProvider.now();
        List<CarePlanModel> lookupResult = fhirClient.lookupCarePlans(cpr, pointInTime, onlyActiveCarePlans, onlyUnSatisfied);
        if (lookupResult.isEmpty()) {
            return List.of();
        }
        var orgId = fhirClient.getOrganizationId();
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

    public List<CarePlanModel> getCarePlansWithFilters(String cpr, boolean onlyActiveCarePlans, boolean onlyUnSatisfied, Pagination pagination) throws ServiceException {
        return pageResponses(getCarePlansWithFilters(cpr, onlyActiveCarePlans, onlyUnSatisfied), pagination);
    }

    public Optional<CarePlanModel> getCarePlanById(String carePlanId) throws ServiceException, AccessValidationException {
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        return fhirClient.fetch(qualifiedId);
    }

    public CarePlanModel resolveAlarm(String carePlanId, String questionnaireId) throws ServiceException, AccessValidationException {
        // Get the careplan
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        Optional<CarePlanModel> carePlanResult = fhirClient.fetch(qualifiedId);
        if (carePlanResult.isEmpty()) {
            throw new ServiceException(String.format("Could not look up careplan by id %s", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_DOES_NOT_EXIST);
        }
        CarePlanModel carePlan = carePlanResult.get();

        // Validate access
        //validateAccess(carePlan);

        // Check that the 'satisfiedUntil'-timestamp is indeed in the past, throw an exception if not.
        // CarePlanModel carePlanModel = fhirMapper.mapCarePlan(carePlan,null, fhirClient.getOrganizationId());
        var currentPointInTime = dateProvider.now();
        if (currentPointInTime.isBefore(carePlan.satisfiedUntil())) {
            throw new ServiceException(String.format("Could not resolve alarm for careplan %s! The satisfiedUntil-timestamp was in the future.", carePlanId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_ALREADY_FULFILLED);
        }

        // Recompute the 'satisfiedUntil'-timestamps
        String qualifiedQuestionnaireId = FhirUtils.qualifyId(questionnaireId, ResourceType.Questionnaire);


        carePlan = CarePlanModel.Builder
                .from(carePlan)
                .satisfiedUntil(recomputeFrequencyTimestamps(carePlan, qualifiedQuestionnaireId, currentPointInTime))
                .build();

        // Save the updated carePlan
        fhirClient.update(carePlan);
        return carePlan; // for auditlog
    }

    public CarePlanModel updateCarePlan(String carePlanId, List<String> planDefinitionIds, List<String> questionnaireIds, Map<String, FrequencyModel> frequencies, PatientDetails patientDetails) throws ServiceException, AccessValidationException {
        // Look up the plan definitions to verify that they exist, throw an exception in case they don't.
        List<PlanDefinitionModel> planDefinitionResult = fhirClient.lookupPlanDefinitionsById(planDefinitionIds);
        if (planDefinitionResult.size() != planDefinitionIds.size()) throw new ServiceException(
                "Could not look up plan definitions to update!",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.PLAN_DEFINITIONS_MISSING_FOR_CAREPLAN
        );

        // Validate that the client is allowed to reference the plan definitions.
        //validateAccess(planDefinitionResult);

        // Look up the questionnaires to verify that they exist, throw an exception in case they don't.
        List<QuestionnaireModel> questionnaireResult = fhirClient.lookupQuestionnairesById(questionnaireIds);

        if (questionnaireResult.size() != questionnaireIds.size()) throw new ServiceException(
                "Could not look up questionnaires to update!",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.QUESTIONNAIRES_MISSING_FOR_CAREPLAN
        );


        // Validate that the client is allowed to reference the questionnaires.
        //validateAccess(questionnaireResult);

        // Look up the CarePlan, throw an exception in case it does not exist.
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        Optional<CarePlanModel> careplanResult = fhirClient.fetch(qualifiedId);

        if (careplanResult.isEmpty()) {
            throw new ServiceException(
                    String.format("Could not lookup careplan with id %s!", qualifiedId),
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
        List<String> removedQuestionnaireIds = getIdsOfRemovedQuestionnaires(questionnaireIds, carePlan);

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
                String.format("Careplan with id %s still has unhandled questionnaire-responses!", qualifiedId),
                ErrorKind.BAD_REQUEST,
                ErrorDetails.PLANDEFINITION_CONTAINS_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES
        );


        var orgId = fhirClient.getOrganizationId();
        // Update carePlan

        carePlan = updateCarePlanModel(carePlan, questionnaireIds, frequencies, planDefinitionResult);


        // Update patient
        String patientId = carePlan.patient().id().toString();

        var newPatient = carePlan.patient(); // .orElseThrow(() -> new IllegalStateException(String.format("Could not look up patient with id %s", patientId)));

        //var contactDetails = mergeContacts(oldPatient.getContact(), newPatient.getContact());

        newPatient = PatientModel.Builder.from(newPatient)
                .primaryContact(PrimaryContactModel.Builder
                        .from(newPatient.primaryContact())
                        .organisation(fhirClient.getOrganizationId())
                        .build()
                ).build();

        newPatient = updatePatientModel(newPatient, patientDetails);

        // Save the updated CarePlan
        fhirClient.updateCarePlan(carePlan, newPatient);
        return carePlan; // for auditlogging


    }

    private CarePlanModel updateCarePlanModel(CarePlanModel carePlanModel, List<String> questionnaireIds, Map<String, FrequencyModel> frequencies, List<PlanDefinitionModel> planDefinitions) {
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

    public List<QuestionnaireModel> getUnresolvedQuestionnaires(String carePlanId) throws AccessValidationException, ServiceException {

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
        Organization organization = fhirClient.getCurrentUsersOrganization();
        return ExtensionMapper.extractOrganizationDeadlineTimeDefault(organization.getExtension());
    }

    private boolean questionnaireHasExceededDeadline(CarePlanModel carePlan, List<String> questionnaireIds) {
        //return carePlan.getActivity().stream()
        //        .filter(carePlanActivityComponent -> questionnaireIds.contains(carePlanActivityComponent.getDetail().getInstantiatesCanonical().getFirst().getValue()))
        //        .anyMatch(carePlanActivityComponent -> ExtensionMapper.extractActivitySatisfiedUntil(carePlanActivityComponent.getDetail().getExtension()).isBefore(dateProvider.now()));
        throw new NotImplementedException();
    }

    private boolean questionnaireHasUnexaminedResponses(String carePlanId, List<String> questionnaireIds) throws ServiceException {
        return fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId)
                .stream()
                .anyMatch(questionnaireResponse -> questionnaireIds.contains(questionnaireResponse.questionnaireId()));
    }

    private boolean questionnairesAllowedByPlanDefinitions(List<PlanDefinitionModel> planDefinitions, List<String> questionnaireIds) {
        var allowedQuestionnaires = planDefinitions.stream().flatMap(pd -> pd.questionnaires().stream().map(qw -> qw.questionnaire().id())).collect(Collectors.toSet());
        var actualQuestionnaires = questionnaireIds.stream().map(id -> new QualifiedId(FhirUtils.qualifyId(id, ResourceType.Questionnaire))).collect(Collectors.toSet());

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
                    String questionnaireId = existing.questionnaire().id().id();
                    List<QuestionnaireResponseModel> result = fhirClient.lookupQuestionnaireResponses(carePlan.id().id(), List.of(questionnaireId));

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

    private Instant recomputeFrequencyTimestamps(CarePlanModel carePlanModel, String questionnaireId, Instant currentPointInTime) {
        List<QuestionnaireWrapperModel> updatedQuestionnaires = carePlanModel.questionnaires().stream()
                .map(wrapper -> {
                    if (questionnaireId.equals(wrapper.questionnaire().id().toString()) && wrapper.satisfiedUntil().isBefore(currentPointInTime)) {

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

    private List<String> getIdsOfUnresolvedQuestionnaires(String carePlanId) throws ServiceException {
        List<QuestionnaireResponseModel> responses = fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId);
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

}
