package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
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
    private final FhirClient fhirClient;
    private final FhirMapper fhirMapper;
    private final DateProvider dateProvider;
    private final CustomUserClient customUserService;
    private final DtoMapper dtoMapper;

    private UserContextProvider userContextProvider;
    @Value("${patientidp.api.url}")
    private String patientidpApiUrl;

    public CarePlanService(FhirClient fhirClient, FhirMapper fhirMapper, DateProvider dateProvider, AccessValidator accessValidator, DtoMapper dtoMapper, CustomUserClient customUserService) {
        super(accessValidator);

        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.dateProvider = dateProvider;
        this.dtoMapper = dtoMapper;
        this.customUserService = customUserService;
    }

    private static List<String> getIdsOfRemovedQuestionnaires(List<String> questionnaireIds, CarePlan carePlan) {
        return carePlan.getActivity().stream()
                .flatMap(carePlanActivityComponent -> carePlanActivityComponent.getDetail().getInstantiatesCanonical().stream())
                .map(PrimitiveType::getValue)
                .filter(currentQuestionnaireId -> !questionnaireIds.contains(currentQuestionnaireId))
                .toList();
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
                    updatedPatient.cpr(),
                    updatedPatient.familyName(),
                    updatedPatient.cpr(),
                    updatedPatient.contactDetails(),
                    updatedPrimaryContact,
                    updatedPatient.additionalRelativeContactDetails(),
                    updatedPatient.customUserId(),
                    updatedPatient.customUserName()
            );
        }

        if (patient.isPresent()) {
            String patientId = patient.get().getIdElement().toUnqualifiedVersionless().getValue();
            boolean onlyActiveCarePlans = true;
            var carePlanResult = fhirClient.lookupCarePlansByPatientId(patientId, onlyActiveCarePlans);

            if (!carePlanResult.getCarePlans().isEmpty()) {
                throw new ServiceException(String.format(
                        "Could not create careplan for cpr %s: Another active careplan already exists!", cpr),
                        ErrorKind.BAD_REQUEST,
                        ErrorDetails.CAREPLAN_EXISTS
                );
            }

            var newPatient = fhirMapper.mapPatientModel(updatedPatient);

            if (updatedPatient.primaryContact() != null) {
                newPatient.getContactFirstRep().setOrganization(new Reference(fhirClient.getOrganizationId()));
                var contacts = this.mergeContacts(patient.get().getContact(), newPatient.getContact());
                patient.get().setContact(contacts);
            }

            patient.get().setTelecom(newPatient.getTelecom());

            var orgId = fhirClient.getOrganizationId();
            updatedPatient = fhirMapper.mapPatient(patient.get(), orgId);
        }

        // Validate referenced questionnaires
        if (carePlan.questionnaires() != null && !carePlan.questionnaires().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(
                    carePlan.questionnaires().stream()
                            .map(qw -> qw.questionnaire().id().toString())
                            .toList()
            );
            validateAccess(lookupResult.getQuestionnaires());
        }

        // Validate referenced plan definitions
        if (carePlan.planDefinitions() != null && !carePlan.planDefinitions().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupPlanDefinitionsById(
                    carePlan.planDefinitions().stream()
                            .map(pd -> pd.id().toString())
                            .toList()
            );
            validateAccess(lookupResult.getPlanDefinitions());
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
                fhirClient.update(patient.get());
                return fhirClient.save(fhirMapper.mapCarePlanModel(updatedCarePlan));
            }

            if (patientidpApiUrl != null && !patientidpApiUrl.isEmpty()) {
                try {
                    Optional<CustomUserResponseDto> customUserResponse = customUserService
                            .createUser(dtoMapper.mapPatientModelToCustomUserRequest(updatedPatient));

                    if (customUserResponse.isPresent()) {
                        CustomUserResponseDto dto = customUserResponse.get();
                        updatedPatient = new PatientModel(
                                updatedPatient.id(),
                                updatedPatient.cpr(),
                                updatedPatient.familyName(),
                                updatedPatient.cpr(),
                                updatedPatient.contactDetails(),
                                updatedPatient.primaryContact(),
                                updatedPatient.additionalRelativeContactDetails(),
                                dto.getId(),
                                dto.getUsername()
                        );
                    }
                } catch (Exception e) {
                    throw new ServiceException(
                            String.format("Could not create customlogin for patient with id %s!", updatedPatient.id()),
                            ErrorKind.BAD_GATEWAY,
                            ErrorDetails.CUSTOMLOGIN_UNKNOWN_ERROR
                    );
                }
            }

            return fhirClient.saveCarePlan(
                    fhirMapper.mapCarePlanModel(updatedCarePlan),
                    fhirMapper.mapPatientModel(updatedPatient)
            );

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error saving CarePlan", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }


    public CarePlanModel completeCarePlan(String carePlanId) throws ServiceException {
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        FhirLookupResult lookupResult = fhirClient.lookupCarePlanById(qualifiedId);


        Optional<CarePlan> carePlan = lookupResult.getCarePlan(qualifiedId);

        if (carePlan.isEmpty()) {
            throw new ServiceException(String.format("Could not lookup careplan with id %s!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_DOES_NOT_EXIST);
        }

        var questionnaireResponsesStillNotExamined = fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId).getQuestionnaireResponses();
        if (!questionnaireResponsesStillNotExamined.isEmpty()) {
            throw new ServiceException(String.format("Careplan with id %s still has unhandled questionnaire-responses!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_HAS_UNHANDLED_QUESTIONNAIRERESPONSES);
        }

        if (ExtensionMapper.extractCarePlanSatisfiedUntil(carePlan.get().getExtension()).isBefore(Instant.now())) {
            throw new ServiceException(String.format("Careplan with id %s is missing scheduled responses!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_IS_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES);
        }

        CarePlan completedCarePlan = carePlan.get().setStatus(CarePlan.CarePlanStatus.COMPLETED);
        fhirClient.update(completedCarePlan);

        return fhirMapper.mapCarePlan(completedCarePlan, lookupResult, fhirClient.getOrganizationId()); // for auditlog
    }

    public List<CarePlanModel> getCarePlansWithFilters(boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        Instant pointInTime = dateProvider.now();
        FhirLookupResult lookupResult = fhirClient.lookupCarePlans(pointInTime, onlyActiveCarePlans, onlyUnSatisfied);
        if (lookupResult.getCarePlans().isEmpty()) {
            return List.of();
        }
        var orgId = fhirClient.getOrganizationId();
        // Map and sort the resources

        return lookupResult.getCarePlans().stream()
                .map(cp -> fhirMapper.mapCarePlan(cp, lookupResult, orgId))
                .sorted((careplan1, careplan2) -> {
                    String name1 = String.join(" ", careplan1.patient().givenName(), careplan1.patient().familyName());
                    String name2 = String.join(" ", careplan2.patient().givenName(), careplan2.patient().familyName());
                    return name1.compareTo(name2);
                })
                .toList();
    }

    public List<CarePlanModel> getCarePlansWithFilters(String cpr, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        Instant pointInTime = dateProvider.now();
        FhirLookupResult lookupResult = fhirClient.lookupCarePlans(cpr, pointInTime, onlyActiveCarePlans, onlyUnSatisfied);
        if (lookupResult.getCarePlans().isEmpty()) {
            return List.of();
        }
        var orgId = fhirClient.getOrganizationId();
        // Map and sort the resources

        return lookupResult.getCarePlans().stream()
                .map(cp -> fhirMapper.mapCarePlan(cp, lookupResult, orgId))
                .sorted((careplan1, careplan2) -> {
                    String name1 = String.join(" ", careplan1.patient().givenName(), careplan1.patient().familyName());
                    String name2 = String.join(" ", careplan2.patient().givenName(), careplan2.patient().familyName());
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
        FhirLookupResult lookupResult = fhirClient.lookupCarePlanById(qualifiedId);

        Optional<CarePlan> carePlan = lookupResult.getCarePlan(qualifiedId);
        if (carePlan.isEmpty()) {
            return Optional.empty();
        }

        // Validate that the user is allowed to access the careplan.
        validateAccess(carePlan.get());

        // Map the resource
        CarePlanModel mappedCarePlan = fhirMapper.mapCarePlan(carePlan.get(), lookupResult, fhirClient.getOrganizationId());
        return Optional.of(mappedCarePlan);
    }

    public CarePlanModel resolveAlarm(String carePlanId, String questionnaireId) throws ServiceException, AccessValidationException {
        // Get the careplan
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        FhirLookupResult carePlanResult = fhirClient.lookupCarePlanById(qualifiedId);
        if (carePlanResult.getCarePlan(qualifiedId).isEmpty()) {
            throw new ServiceException(String.format("Could not look up careplan by id %s", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_DOES_NOT_EXIST);
        }
        CarePlan carePlan = carePlanResult.getCarePlan(qualifiedId).get();

        System.out.println(fhirMapper.mapCarePlan(carePlan, carePlanResult, fhirClient.getOrganizationId()));

        // Validate access
        validateAccess(carePlan);

        // Check that the 'satisfiedUntil'-timestamp is indeed in the past, throw an exception if not.
        CarePlanModel carePlanModel = fhirMapper.mapCarePlan(carePlan, carePlanResult, fhirClient.getOrganizationId());
        var currentPointInTime = dateProvider.now();
        if (currentPointInTime.isBefore(carePlanModel.satisfiedUntil())) {
            throw new ServiceException(String.format("Could not resolve alarm for careplan %s! The satisfiedUntil-timestamp was in the future.", carePlanId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_ALREADY_FULFILLED);
        }

        // Recompute the 'satisfiedUntil'-timestamps
        String qualifiedQuestionnaireId = FhirUtils.qualifyId(questionnaireId, ResourceType.Questionnaire);


        carePlanModel = CarePlanModel.Builder
                .from(carePlanModel)
                .satisfiedUntil(recomputeFrequencyTimestamps(carePlanModel, qualifiedQuestionnaireId, currentPointInTime))
                .build();

        // Save the updated carePlan
        fhirClient.update(fhirMapper.mapCarePlanModel(carePlanModel));
        return carePlanModel; // for auditlog
    }

    public CarePlanModel updateCarePlan(String carePlanId, List<String> planDefinitionIds, List<String> questionnaireIds, Map<String, FrequencyModel> frequencies, PatientDetails patientDetails) throws ServiceException, AccessValidationException {
        // Look up the plan definitions to verify that they exist, throw an exception in case they don't.
        FhirLookupResult planDefinitionResult = fhirClient.lookupPlanDefinitionsById(planDefinitionIds);
        if (planDefinitionResult.getPlanDefinitions().size() != planDefinitionIds.size()) throw new ServiceException(
                "Could not look up plan definitions to update!",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.PLAN_DEFINITIONS_MISSING_FOR_CAREPLAN
        );

        // Validate that the client is allowed to reference the plan definitions.
        validateAccess(planDefinitionResult.getPlanDefinitions());

        // Look up the questionnaires to verify that they exist, throw an exception in case they don't.
        FhirLookupResult questionnaireResult = fhirClient.lookupQuestionnairesById(questionnaireIds);
        if (questionnaireResult.getQuestionnaires().size() != questionnaireIds.size()) throw new ServiceException(
                "Could not look up questionnaires to update!",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.QUESTIONNAIRES_MISSING_FOR_CAREPLAN
        );


        // Validate that the client is allowed to reference the questionnaires.
        validateAccess(questionnaireResult.getQuestionnaires());

        // Look up the CarePlan, throw an exception in case it does not exist.
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        FhirLookupResult careplanResult = fhirClient.lookupCarePlanById(qualifiedId);

        boolean emptyResult = careplanResult.getCarePlans().size() != 1 || careplanResult.getCarePlan(qualifiedId).isEmpty();

        if (emptyResult) {
            throw new ServiceException(
                    String.format("Could not lookup careplan with id %s!", qualifiedId),
                    ErrorKind.BAD_REQUEST,
                    ErrorDetails.CAREPLAN_DOES_NOT_EXIST
            );
        }

        CarePlan carePlan = careplanResult.getCarePlan(qualifiedId).get();

        // Validate that the client is allowed to update the carePlan.
        validateAccess(carePlan);

        // Check that every provided questionnaire is a part of (at least) one of the plan definitions.
        List<PlanDefinitionModel> planDefinitions = planDefinitionResult.getPlanDefinitions().stream().map(pd -> fhirMapper.mapPlanDefinitionResult(pd, planDefinitionResult)).toList();
        if (!questionnairesAllowedByPlanDefinitions(planDefinitions, questionnaireIds)) throw new ServiceException(
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
        CarePlanModel originalCareplan = fhirMapper.mapCarePlan(carePlan, careplanResult.merge(questionnaireResult), orgId);


        List<QuestionnaireWrapperModel> updatedQuestionnaires = planDefinitions.stream()
                .flatMap(pd -> pd.questionnaires().stream())
                .toList();


        // Update patient
        String patientId = originalCareplan.patient().id().toString();

        var oldPatient = careplanResult
                .getPatient(originalCareplan.patient().id().toString())
                .orElseThrow(() -> new IllegalStateException(String.format("Could not look up patient with id %s", patientId)));


        CarePlanModel updatedCarePlan = CarePlanModel.Builder
                .from(originalCareplan)
                .planDefinitions(planDefinitions)
                .questionnaires(buildQuestionnaireWrapperModels(originalCareplan, updatedQuestionnaires, frequencies))
                .satisfiedUntil(getRefreshedFrequencyTimestampForCarePlan(originalCareplan))
                .build();


        PatientModel patientModel = fhirMapper.mapPatient(oldPatient, orgId);


        var contactDetails = patientModel.primaryContact().contactDetails();


        if (patientDetails.primaryRelativePrimaryPhone() != null || patientDetails.primaryRelativeSecondaryPhone() != null) {
            if (patientModel.primaryContact().contactDetails() == null) {
                contactDetails = new ContactDetailsModel(
                        null,
                        null,
                        null,
                        null,
                        patientDetails.primaryRelativePrimaryPhone(),
                        patientDetails.primaryRelativeSecondaryPhone()
                );
            }
        }

        patientModel = PatientModel.Builder
                .from(patientModel)
                .contactDetails(new ContactDetailsModel(
                        patientModel.contactDetails().street(),
                        patientModel.contactDetails().postalCode(),
                        patientModel.contactDetails().country(),
                        patientModel.contactDetails().city(),
                        patientDetails.patientPrimaryPhone(),
                        patientDetails.patientSecondaryPhone()
                ))
                .primaryContact(new PrimaryContactModel(
                        contactDetails,
                        patientDetails.primaryRelativeName(),
                        patientDetails.primaryRelativeAffiliation(),
                        fhirClient.getOrganizationId())
                )
                .build();

        var newPatient = fhirMapper.mapPatientModel(patientModel);

        var mergedContacts = mergeContacts(oldPatient.getContact(), newPatient.getContact());

        // Without setting the contacts below, the old contacts for other departments/organisations will be discarded
        var updatedPatient = newPatient.setContact(mergedContacts);

        // Save the updated CarePlan
        fhirClient.updateCarePlan(fhirMapper.mapCarePlanModel(updatedCarePlan), updatedPatient);
        return updatedCarePlan; // for auditlogging
    }

    public List<QuestionnaireModel> getUnresolvedQuestionnaires(String carePlanId) throws AccessValidationException, ServiceException {

        Optional<CarePlanModel> optional = getCarePlanById(carePlanId);

        if (optional.isEmpty()) throw new ServiceException(
                "Careplan was not found",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.CAREPLAN_DOES_NOT_EXIST
        );

        CarePlanModel carePlanModel = optional.get();

        CarePlan carePlan = fhirMapper.mapCarePlanModel(carePlanModel);

        List<String> idsOfQuestionnairesContainingAlarm = getIdsOfQuestionnairesContainingAlarm(carePlanModel, carePlan);
        List<String> idsOfUnresolvedQuestionnaires = getIdsOfUnresolvedQuestionnaires(carePlanId);

        return carePlanModel.questionnaires().stream()
                .map(QuestionnaireWrapperModel::questionnaire)
                .filter(questionnaire -> {
                    String id = questionnaire.id().toString();
                    return idsOfUnresolvedQuestionnaires.contains(id) || idsOfQuestionnairesContainingAlarm.contains(id);
                }).toList();
    }

    public TimeType getDefaultDeadlineTime() throws ServiceException {
        Organization organization = fhirClient.getCurrentUsersOrganization();
        return ExtensionMapper.extractOrganizationDeadlineTimeDefault(organization.getExtension());
    }

    private boolean questionnaireHasExceededDeadline(CarePlan carePlan, List<String> questionnaireIds) {
        return carePlan.getActivity().stream()
                .filter(carePlanActivityComponent -> questionnaireIds.contains(carePlanActivityComponent.getDetail().getInstantiatesCanonical().getFirst().getValue()))
                .anyMatch(carePlanActivityComponent -> ExtensionMapper.extractActivitySatisfiedUntil(carePlanActivityComponent.getDetail().getExtension()).isBefore(dateProvider.now()));
    }

    private boolean questionnaireHasUnexaminedResponses(String carePlanId, List<String> questionnaireIds) throws ServiceException {
        return fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId)
                .getQuestionnaireResponses().stream()
                .anyMatch(questionnaireResponse -> questionnaireIds.contains(questionnaireResponse.getQuestionnaire()));
    }

    private boolean questionnairesAllowedByPlanDefinitions(List<PlanDefinitionModel> planDefinitions, List<String> questionnaireIds) {
        var allowedQuestionnaires = planDefinitions.stream().flatMap(pd -> pd.questionnaires().stream().map(qw -> qw.questionnaire().id())).collect(Collectors.toSet());
        var actualQuestionnaires = questionnaireIds.stream().map(id -> new QualifiedId(FhirUtils.qualifyId(id, ResourceType.Questionnaire))).collect(Collectors.toSet());

        return allowedQuestionnaires.containsAll(actualQuestionnaires);
    }

    private List<QuestionnaireWrapperModel> buildQuestionnaireWrapperModels(CarePlanModel carePlan, List<QuestionnaireWrapperModel> updatedQuestionnaires, Map<String, FrequencyModel> updatedQuestionnaireIdFrequencies) {
        List<QuestionnaireWrapperModel> currentQuestionnaires = carePlan.questionnaires();
        return updatedQuestionnaires.stream().map(wrapper -> {
            Optional<QuestionnaireWrapperModel> currentQuestionnaire = currentQuestionnaires.stream()
                    .filter(q -> q.questionnaire().id().equals(wrapper.questionnaire().id()))
                    .findFirst();

            FrequencyModel updatedFrequency = updatedQuestionnaireIdFrequencies.get(wrapper.questionnaire().id().toString());


            if (currentQuestionnaire.isEmpty()) {
                // Initialize the 'satisfied-until' timestamp-
                FrequencyEnumerator frequencyEnumerator = new FrequencyEnumerator(wrapper.frequency());
                Instant satisfiedUntil = frequencyEnumerator.getSatisfiedUntilForInitialization(dateProvider.now());
                return QuestionnaireWrapperModel.Builder
                        .from(wrapper)
                        .frequency(updatedFrequency)
                        .satisfiedUntil(satisfiedUntil)
                        .build();
            }

            var builder = QuestionnaireWrapperModel.Builder
                    .from(wrapper)
                    .frequency(updatedFrequency);

            if (!updatedFrequency.equals(currentQuestionnaire.get().frequency())) {
                // re-Initialize the 'satisfied-until' timestamp-
                Instant currentSatisfiedUntil = currentQuestionnaire.get().satisfiedUntil();

                FrequencyEnumerator frequencyEnumerator = new FrequencyEnumerator(wrapper.frequency());
                Instant newSatisfiedUntil = frequencyEnumerator.getSatisfiedUntilForFrequencyChange(dateProvider.now());

                // if current satisfied-until > new, this means that the patient has already answered today
                // and in this case we want to keep this as 'SatisfiedUntil'
                if (currentSatisfiedUntil.isAfter(newSatisfiedUntil)) {
                    String questionnaireId = currentQuestionnaire.get().questionnaire().id().id();
                    FhirLookupResult lookupQuestionnaireResponses = fhirClient.lookupQuestionnaireResponses(carePlan.id().id(), List.of(questionnaireId));

                    ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("Europe/Copenhagen"));

                    boolean answerFromTodayExist = lookupQuestionnaireResponses.getQuestionnaireResponses().stream()
                            .anyMatch(questionnaireResponse -> {
                                ZonedDateTime answered = ZonedDateTime.ofInstant(questionnaireResponse.getAuthored().toInstant(), ZoneId.of("Europe/Copenhagen"));

                                // check if answer is from 'today and before deadline'
                                return answered.toLocalDate().equals(now.toLocalDate()) && answered.toLocalTime().isBefore(updatedFrequency.timeOfDay());
                            });

                    if (answerFromTodayExist) {
                        Instant nextSatisfiedUntil = frequencyEnumerator.getSatisfiedUntil(dateProvider.now(), false);
                        return builder.satisfiedUntil(nextSatisfiedUntil).build();
                    }

                    return builder.satisfiedUntil(newSatisfiedUntil).build();

                }
                return builder.satisfiedUntil(newSatisfiedUntil).build();
            }

            return builder.satisfiedUntil(currentQuestionnaire.get().satisfiedUntil()).build();
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
                .skip((long) (pagination.getOffset() - 1) * pagination.getLimit())
                .limit(pagination.getLimit())
                .toList();
    }

    private List<String> getIdsOfQuestionnairesContainingAlarm(CarePlanModel carePlanModel, CarePlan carePlan) {
        return carePlanModel.questionnaires()
                .stream()
                .map(QuestionnaireWrapperModel::questionnaire)
                .filter(questionnaire -> questionnaireHasExceededDeadline(carePlan, List.of(questionnaire.id().id())))
                .map(questionnaire -> questionnaire.id().id())
                .toList();
    }

    private List<String> getIdsOfUnresolvedQuestionnaires(String carePlanId) throws ServiceException {
        List<QuestionnaireResponse> responses = fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId).getQuestionnaireResponses();
        return responses.stream()
                .map(QuestionnaireResponse::getQuestionnaire)
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
