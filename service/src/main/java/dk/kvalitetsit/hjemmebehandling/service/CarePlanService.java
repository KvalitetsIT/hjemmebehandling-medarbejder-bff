package dk.kvalitetsit.hjemmebehandling.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.fhir.ExtensionMapper;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.ContactDetailsModel;
import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientDetails;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireWrapperModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;

public class CarePlanService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    private DateProvider dateProvider;

    private CustomUserClient customUserService;
    
    private DtoMapper dtoMapper;
    
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

    public String createCarePlan(CarePlanModel carePlan) throws ServiceException, AccessValidationException {
        // Try to look up the patient in the careplan
        String cpr = carePlan.getPatient().getCpr();
        var patient = fhirClient.lookupPatientByCpr(cpr);

        // TODO: More validations should be performed - possibly?
        // If the patient did exist, check that no existing careplan exists for the patient
        if(patient.isPresent()) {
            String patientId = patient.get().getIdElement().toUnqualifiedVersionless().getValue();
            boolean onlyActiveCarePlans = true;
            var carePlanResult = fhirClient.lookupCarePlansByPatientId(patientId, onlyActiveCarePlans);

            if(!carePlanResult.getCarePlans().isEmpty()) {
                throw new ServiceException(String.format("Could not create careplan for cpr %s: Another active careplan already exists!", cpr), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_EXISTS);
            }

            var newPatient = fhirMapper.mapPatientModel(carePlan.getPatient());
            if (newPatient != null) patient.get().setContact(newPatient.getContact());

            // If we already knew the patient, replace the patient reference with the resource we just retrieved (to be able to map the careplan properly.)
            carePlan.setPatient(fhirMapper.mapPatient(patient.get()));
        }

        // Check that the referenced questionnaires and plandefinitions are valid for the client to access (and thus use).
        validateReferences(carePlan);

        // Initialize basic attributes for a new CarePlan: Id, status and so on.
        initializeAttributesForNewCarePlan(carePlan);

        try {
            // If the patient did not exist, create it along with the careplan. Otherwise just create the careplan.
            if(patient.isPresent()) {
                fhirClient.updatePatient(patient.get());
                return fhirClient.saveCarePlan(fhirMapper.mapCarePlanModel(carePlan));
            }

            // create customLoginUser if the patient do not exist. Done if an apiurl is set.
            if(patientidpApiUrl!=null && !"".equals(patientidpApiUrl)) createCustomLogin(carePlan.getPatient());

            // create patient and careplan
            String careplanId = fhirClient.saveCarePlan(fhirMapper.mapCarePlanModel(carePlan), fhirMapper.mapPatientModel(carePlan.getPatient()));
            return careplanId;
        }
        catch(ServiceException e){
            throw e;
        }
        catch(Exception e) {
            throw new ServiceException("Error saving CarePlan", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }
    
    private void createCustomLogin(PatientModel patientModel) throws ServiceException {
        try{
            Optional<CustomUserResponseDto> customUserResponseDto = customUserService.createUser(dtoMapper.mapPatientModelToCustomUserRequest(patientModel));
            if(customUserResponseDto.isPresent()) {
                String customerUserLinkId = customUserResponseDto.get().getId();
                patientModel.setCustomUserId(customerUserLinkId);
                patientModel.setCustomUserName(customUserResponseDto.get().getUsername());
            }
        } catch(Exception e){
            throw new ServiceException(String.format("Could not create customlogin for patient with id %s!", patientModel.getId()), ErrorKind.BAD_GATEWAY, ErrorDetails.CUSTOMLOGIN_UNKNOWN_ERROR);
        }

    }

    public CarePlanModel completeCarePlan(String carePlanId) throws ServiceException {
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        FhirLookupResult lookupResult = fhirClient.lookupCarePlanById(qualifiedId);


        Optional<CarePlan> carePlan = lookupResult.getCarePlan(qualifiedId);

        if(!carePlan.isPresent()) {
            throw new ServiceException(String.format("Could not lookup careplan with id %s!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_DOES_NOT_EXIST);
        }

        var questionnaireResponsesStillNotExamined = fhirClient.lookupQuestionnaireResponsesByStatusAndCareplanId(List.of(ExaminationStatus.NOT_EXAMINED),carePlanId).getQuestionnaireResponses();
        if(questionnaireResponsesStillNotExamined.size() > 0){
            throw new ServiceException(String.format("Careplan with id %s still has unhandled questionnaire-responses!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_HAS_UNHANDLED_QUESTIONNAIRERESPONSES);
        }

        if (ExtensionMapper.extractCarePlanSatisfiedUntil(carePlan.get().getExtension()).isBefore(Instant.now())) {
            throw new ServiceException(String.format("Careplan with id %s is missing scheduled responses!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_IS_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES);
        }

        CarePlan completedCarePlan = carePlan.get().setStatus(CarePlan.CarePlanStatus.COMPLETED);
        fhirClient.updateCarePlan(completedCarePlan);
        return fhirMapper.mapCarePlan(completedCarePlan, lookupResult); // for auditlog
    }

    public List<CarePlanModel> getCarePlansWithFilters(Optional<String> cpr, boolean onlyActiveCarePlans, boolean onlyUnSatisfied, PageDetails pageDetails) throws ServiceException {
        Instant pointInTime = dateProvider.now();
        int offset = pageDetails.getOffset();
        int count = pageDetails.getPageSize();
        FhirLookupResult lookupResult = fhirClient.lookupCarePlans(cpr,pointInTime, onlyActiveCarePlans, onlyUnSatisfied, offset, count);
        if(lookupResult.getCarePlans().isEmpty()) {
            return List.of();
        }

        // Map the resources
        return lookupResult.getCarePlans()
                .stream()
                .map(cp -> fhirMapper.mapCarePlan(cp, lookupResult))
                .collect(Collectors.toList());
    }

    public Optional<CarePlanModel> getCarePlanById(String carePlanId) throws ServiceException, AccessValidationException {
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        FhirLookupResult lookupResult = fhirClient.lookupCarePlanById(qualifiedId);

        Optional<CarePlan> carePlan = lookupResult.getCarePlan(qualifiedId);
        if(!carePlan.isPresent()) {
            return Optional.empty();
        }

        // Validate that the user is allowed to access the careplan.
        validateAccess(carePlan.get());

        // Map the resource
        CarePlanModel mappedCarePlan = fhirMapper.mapCarePlan(carePlan.get(), lookupResult);
        return Optional.of(mappedCarePlan);
    }

    public CarePlanModel resolveAlarm(String carePlanId) throws ServiceException, AccessValidationException {
        // Get the careplan
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        FhirLookupResult carePlanResult = fhirClient.lookupCarePlanById(qualifiedId);
        if(!carePlanResult.getCarePlan(qualifiedId).isPresent()) {
            throw new ServiceException(String.format("Could not look up careplan by id %s", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_DOES_NOT_EXIST);
        }
        CarePlan carePlan = carePlanResult.getCarePlan(qualifiedId).get();

        // Validate access
        validateAccess(carePlan);

        // Check that the 'satisfiedUntil'-timestamp is indeed in the past, throw an exception if not.
        CarePlanModel carePlanModel = fhirMapper.mapCarePlan(carePlan, carePlanResult);
        var currentPointInTime = dateProvider.now();
        if(currentPointInTime.isBefore(carePlanModel.getSatisfiedUntil())) {
            throw new ServiceException(String.format("Could not resolve alarm for careplan %s! The satisfiedUntil-timestamp was in the future.", carePlanId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_ALREADY_FULFILLED);
        }

        // Recompute the 'satisfiedUntil'-timestamps
        recomputeFrequencyTimestamps(carePlanModel, currentPointInTime);

        // Save the updated carePlan
        fhirClient.updateCarePlan(fhirMapper.mapCarePlanModel(carePlanModel));
        return carePlanModel; // for auditlog
    }

    public CarePlanModel updateCarePlan(String carePlanId, List<String> planDefinitionIds, List<String> questionnaireIds, Map<String, FrequencyModel> frequencies, PatientDetails patientDetails) throws ServiceException, AccessValidationException {
        // Look up the plan definitions to verify that they exist, throw an exception in case they don't.
        FhirLookupResult planDefinitionResult = fhirClient.lookupPlanDefinitionsById(planDefinitionIds);
        if(planDefinitionResult.getPlanDefinitions().size() != planDefinitionIds.size()) {
            throw new ServiceException("Could not look up plan definitions to update!", ErrorKind.BAD_REQUEST, ErrorDetails.PLAN_DEFINITIONS_MISSING_FOR_CAREPLAN);
        }

        // Validate that the client is allowed to reference the plan definitions.
        validateAccess(planDefinitionResult.getPlanDefinitions());

        // Look up the questionnaires to verify that they exist, throw an exception in case they don't.
        FhirLookupResult questionnaireResult = fhirClient.lookupQuestionnairesById(questionnaireIds);
        if(questionnaireResult.getQuestionnaires().size() != questionnaireIds.size()) {
            throw new ServiceException("Could not look up questionnaires to update!", ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRES_MISSING_FOR_CAREPLAN);
        }

        // Validate that the client is allowed to reference the questionnaires.
        validateAccess(questionnaireResult.getQuestionnaires());

        // Look up the CarePlan, throw an exception in case it does not exist.
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        FhirLookupResult careplanResult = fhirClient.lookupCarePlanById(qualifiedId);
        if(careplanResult.getCarePlans().size() != 1 || !careplanResult.getCarePlan(qualifiedId).isPresent()) {
            throw new ServiceException(String.format("Could not lookup careplan with id %s!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_DOES_NOT_EXIST);
        }
        CarePlan carePlan = careplanResult.getCarePlan(qualifiedId).get();

        // Validate that the client is allowed to update the carePlan.
        validateAccess(carePlan);

        // Check that every provided questionnaire is a part of (at least) one of the plan definitions.
        List<PlanDefinitionModel> planDefinitions = planDefinitionResult.getPlanDefinitions().stream().map(pd -> fhirMapper.mapPlanDefinition(pd, planDefinitionResult)).collect(Collectors.toList());
        if(!questionnairesAllowedByPlanDefinitions(planDefinitions, questionnaireIds)) {
            throw new ServiceException("Not every questionnaireId could be found in the provided plan definitions.", ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRES_NOT_ALLOWED_FOR_CAREPLAN);
        }

        // Update carePlan
        CarePlanModel carePlanModel = fhirMapper.mapCarePlan(carePlan, careplanResult.merge(questionnaireResult));
        updateCarePlanModel(carePlanModel, questionnaireIds, frequencies, planDefinitions);

        // Update patient
        String patientId = carePlanModel.getPatient().getId().toString();
        PatientModel patientModel = fhirMapper.mapPatient(careplanResult.getPatient(carePlanModel.getPatient().getId().toString())
                .orElseThrow(() -> new IllegalStateException(String.format("Could not look up patient with id %s", patientId))));
        updatePatientModel(patientModel, patientDetails);

        // Save the updated CarePlan
        fhirClient.updateCarePlan(fhirMapper.mapCarePlanModel(carePlanModel), fhirMapper.mapPatientModel(patientModel));
        return carePlanModel; // for auditlogging
    }

    private boolean questionnairesAllowedByPlanDefinitions(List<PlanDefinitionModel> planDefinitions, List<String> questionnaireIds) {
        var allowedQuestionnaires = planDefinitions.stream().flatMap(pd -> pd.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire().getId())).collect(Collectors.toSet());
        var actualQuestionnaires = questionnaireIds.stream().map(id -> new QualifiedId(FhirUtils.qualifyId(id, ResourceType.Questionnaire))).collect(Collectors.toSet());

        return allowedQuestionnaires.containsAll(actualQuestionnaires);
    }

    private void updateCarePlanModel(CarePlanModel carePlanModel, List<String> questionnaireIds, Map<String, FrequencyModel> updatedFrequencies, List<PlanDefinitionModel> planDefinitions) {
        carePlanModel.setPlanDefinitions(planDefinitions);

        List<QuestionnaireWrapperModel> updatedQuestionnaires = planDefinitions.stream()
            .flatMap(pd -> pd.getQuestionnaires().stream())
            .collect(Collectors.toList());
        carePlanModel.setQuestionnaires(buildQuestionnaireWrapperModels(carePlanModel.getQuestionnaires(), updatedQuestionnaires, updatedFrequencies));
        refreshFrequencyTimestampForCarePlan(carePlanModel);
    }

    List<QuestionnaireWrapperModel> buildQuestionnaireWrapperModels(List<QuestionnaireWrapperModel> currentQuestionnaires, List<QuestionnaireWrapperModel> updatedQuestionnaires, Map<String, FrequencyModel> updatedQuestionnaireIdFrequencies) {
        List<QuestionnaireWrapperModel> result = new ArrayList<>();

        for(var wrapper : updatedQuestionnaires) {
            Optional<QuestionnaireWrapperModel> currentQuestionnaire = currentQuestionnaires.stream()
                .filter(q -> q.getQuestionnaire().getId().equals(wrapper.getQuestionnaire().getId()))
                .findFirst();
            FrequencyModel updatedFrequency = updatedQuestionnaireIdFrequencies.get(wrapper.getQuestionnaire().getId().toString());

            // Set the frequency
            wrapper.setFrequency(updatedFrequency);

            if (currentQuestionnaire.isEmpty()) {
                // Initialize the 'satisfied-until' timestamp-
                refreshFrequencyTimestamp(wrapper);
            }
            else {
                if (!updatedFrequency.equals(currentQuestionnaire.get().getFrequency())) {
                    // re-Initialize the 'satisfied-until' timestamp-
                    refreshFrequencyTimestamp(wrapper);
                }
                else {
                    wrapper.setSatisfiedUntil( currentQuestionnaire.get().getSatisfiedUntil() );
                }
            }

            result.add(wrapper);
        }
        return result;
    }

    private void updatePatientModel(PatientModel patientModel, PatientDetails patientDetails) {
        patientModel.getPatientContactDetails().setPrimaryPhone(patientDetails.getPatientPrimaryPhone());
        patientModel.getPatientContactDetails().setSecondaryPhone(patientDetails.getPatientSecondaryPhone());

        patientModel.setPrimaryRelativeName(patientDetails.getPrimaryRelativeName());
        patientModel.setPrimaryRelativeAffiliation(patientDetails.getPrimaryRelativeAffiliation());
        if(patientDetails.getPrimaryRelativePrimaryPhone() != null || patientDetails.getPrimaryRelativeSecondaryPhone() != null) {
            if(patientModel.getPrimaryRelativeContactDetails() == null) {
                patientModel.setPrimaryRelativeContactDetails(new ContactDetailsModel());
            }
            patientModel.getPrimaryRelativeContactDetails().setPrimaryPhone(patientDetails.getPrimaryRelativePrimaryPhone());
            patientModel.getPrimaryRelativeContactDetails().setSecondaryPhone(patientDetails.getPrimaryRelativeSecondaryPhone());
        }
    }

    private void validateReferences(CarePlanModel carePlanModel) throws AccessValidationException {
        // Validate questionnaires
        if(carePlanModel.getQuestionnaires() != null && !carePlanModel.getQuestionnaires().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(carePlanModel.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire().getId().toString()).collect(Collectors.toList()));
            validateAccess(lookupResult.getQuestionnaires());
        }

        // Validate planDefinitions
        if(carePlanModel.getPlanDefinitions() != null && !carePlanModel.getPlanDefinitions().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupPlanDefinitionsById(carePlanModel.getPlanDefinitions().stream().map(pd -> pd.getId().toString()).collect(Collectors.toList()));
            validateAccess(lookupResult.getPlanDefinitions());
        }
    }

    private void initializeAttributesForNewCarePlan(CarePlanModel carePlanModel) {
        // Ensure that no id is present on the careplan - the FHIR server will generate that for us.
        carePlanModel.setId(null);

        carePlanModel.setStatus(CarePlanStatus.ACTIVE);

        initializeTimestamps(carePlanModel);

        initializeFrequencyTimestamps(carePlanModel);
    }

    private void initializeTimestamps(CarePlanModel carePlanModel) {
        var today = dateProvider.today().toInstant();
        carePlanModel.setCreated(today);
        carePlanModel.setStartDate(today);
        carePlanModel.setEndDate(null);
    }

    private void initializeFrequencyTimestamps(CarePlanModel carePlanModel) {
        // Mark how far into the future the careplan is 'satisfied' (a careplan is satisfied at a given point in time if it has not had its frequencies violated)
        for(var questionnaireWrapper : carePlanModel.getQuestionnaires()) {

            //Only if changes were made
            refreshFrequencyTimestamp(questionnaireWrapper);
        }
        refreshFrequencyTimestampForCarePlan(carePlanModel);
    }

    private void recomputeFrequencyTimestamps(CarePlanModel carePlanModel, Instant currentPointInTime) {
        for(var questionnaireWrapper : carePlanModel.getQuestionnaires()) {


            // Only recompute the timestamp if its current value is in the past.
            if(questionnaireWrapper.getSatisfiedUntil().isBefore(currentPointInTime)) {
                refreshFrequencyTimestamp(questionnaireWrapper);
            }
        }
        refreshFrequencyTimestampForCarePlan(carePlanModel);
    }

    private void refreshFrequencyTimestamp(QuestionnaireWrapperModel questionnaireWrapperModel) {
        // Invoke 'next' once - get the next deadline.
        var nextDeadline = new FrequencyEnumerator(dateProvider.now(), questionnaireWrapperModel.getFrequency()).next().getPointInTime();
        questionnaireWrapperModel.setSatisfiedUntil(nextDeadline);
    }

    private void refreshFrequencyTimestampForCarePlan(CarePlanModel carePlanModel) {
        var carePlanSatisfiedUntil = carePlanModel.getQuestionnaires()
                .stream()
                .map(qw -> qw.getSatisfiedUntil())
                .min(Comparator.naturalOrder())
                .orElse(Instant.MAX);
        carePlanModel.setSatisfiedUntil(carePlanSatisfiedUntil);
    }
}
