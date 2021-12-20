package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class CarePlanService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    private DateProvider dateProvider;

    public CarePlanService(FhirClient fhirClient, FhirMapper fhirMapper, DateProvider dateProvider, AccessValidator accessValidator) {
        super(accessValidator);

        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.dateProvider = dateProvider;
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

            // If we already knew the patient, replace the patient reference with the resource we just retrieved (to be able to map the careplan properly.)
            carePlan.setPatient(fhirMapper.mapPatient(patient.get()));
        }

        // Check that the referenced questionnaires and plandefinitions are valid for the client to access (and thus use).
        validateReferences(carePlan);

        // Initialize basic attributes for a new CarePlan: Id, status and so on.
        initializeAttributesForNewCarePlan(carePlan);

        try {
            // If the patient did not exist, create it along with the careplan. Otherwise just create the careplan.
            if(!patient.isPresent()) {
                return fhirClient.saveCarePlan(fhirMapper.mapCarePlanModel(carePlan), fhirMapper.mapPatientModel(carePlan.getPatient()));
            }
            else {
                return fhirClient.saveCarePlan(fhirMapper.mapCarePlanModel(carePlan));
            }
        }
        catch(Exception e) {
            throw new ServiceException("Error saving CarePlan", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }

    public void completeCarePlan(String carePlanId) throws ServiceException {
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        FhirLookupResult lookupResult = fhirClient.lookupCarePlanById(qualifiedId);

        Optional<CarePlan> carePlan = lookupResult.getCarePlan(qualifiedId);
        if(!carePlan.isPresent()) {
            throw new ServiceException(String.format("Could not lookup careplan with id %s!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_DOES_NOT_EXIST);
        }

        CarePlan completedCarePlan = carePlan.get().setStatus(CarePlan.CarePlanStatus.COMPLETED);
        fhirClient.updateCarePlan(completedCarePlan);
    }
    
    public List<CarePlanModel> getCarePlans(boolean onlyActiveCarePlans, PageDetails pageDetails) throws ServiceException {
        int offset = (pageDetails.getPageNumber() - 1) * pageDetails.getPageSize();
        int count = pageDetails.getPageSize();
        
        FhirLookupResult lookupResult = fhirClient.lookupCarePlans(onlyActiveCarePlans, offset,count);
        if(lookupResult.getCarePlans().isEmpty()) {
            return List.of();
        }

        // Map the resourecs
        return lookupResult.getCarePlans()
                .stream()
                .map(cp -> fhirMapper.mapCarePlan(cp, lookupResult))
                .collect(Collectors.toList());
    }

    
    public List<CarePlanModel> getCarePlansByCpr(String cpr, boolean onlyActiveCarePlans) throws ServiceException {
        // Look up the patient so that we may look up careplans by patientId.
        Optional<Patient> patient = fhirClient.lookupPatientByCpr(cpr);
        if(!patient.isPresent()) {
            throw new IllegalStateException(String.format("Could not look up patient by cpr %s!", cpr));
        }

        // Look up the careplans along with related resources needed for mapping.
        String patientId = patient.get().getIdElement().toUnqualifiedVersionless().toString();
        FhirLookupResult lookupResult = fhirClient.lookupCarePlansByPatientId(patientId, onlyActiveCarePlans);
        if(lookupResult.getCarePlans().isEmpty()) {
            return List.of();
        }

        // Map the resourecs
        return lookupResult.getCarePlans()
                .stream()
                .map(cp -> fhirMapper.mapCarePlan(cp, lookupResult))
                .collect(Collectors.toList());
    }

    public List<CarePlanModel> getCarePlansWithUnsatisfiedSchedules(boolean onlyActiveCarePlans, PageDetails pageDetails) throws ServiceException {
        Instant pointInTime = dateProvider.now();
        int offset = (pageDetails.getPageNumber() - 1) * pageDetails.getPageSize();
        int count = pageDetails.getPageSize();
        FhirLookupResult lookupResult = fhirClient.lookupCarePlansUnsatisfiedAt(pointInTime, onlyActiveCarePlans, offset, count);
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

    public void resolveAlarm(String carePlanId) throws ServiceException, AccessValidationException {
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
    }

    public void updateQuestionnaires(String carePlanId, List<String> planDefinitionIds, List<String> questionnaireIds, Map<String, FrequencyModel> frequencies) throws ServiceException, AccessValidationException {
        // Look up the plan definitions to verify that they exist, throw an exception in case they don't.
        FhirLookupResult planDefinitionResult = fhirClient.lookupPlanDefinitions(planDefinitionIds);
        if(planDefinitionResult.getPlanDefinitions().size() != planDefinitionIds.size()) {
            throw new ServiceException("Could not look up plan definitions to update!", ErrorKind.BAD_REQUEST, ErrorDetails.PLAN_DEFINITIONS_MISSING_FOR_CAREPLAN);
        }

        // Validate that the client is allowed to reference the plan definitions.
        validateAccess(planDefinitionResult.getPlanDefinitions());

        // Look up the questionnaires to verify that they exist, throw an exception in case they don't.
        FhirLookupResult questionnaireResult = fhirClient.lookupQuestionnaires(questionnaireIds);
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

        // Update the carePlan
        CarePlanModel carePlanModel = fhirMapper.mapCarePlan(carePlan, careplanResult.merge(questionnaireResult));
        carePlanModel.setPlanDefinitions(planDefinitions);
        carePlanModel.setQuestionnaires(buildQuestionnaireWrapperModels(questionnaireIds, frequencies, planDefinitions));

        // Save the updated CarePlan
        fhirClient.updateCarePlan(fhirMapper.mapCarePlanModel(carePlanModel));
    }

    private boolean questionnairesAllowedByPlanDefinitions(List<PlanDefinitionModel> planDefinitions, List<String> questionnaireIds) {
        var allowedQuestionnaires = planDefinitions.stream().flatMap(pd -> pd.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire().getId())).collect(Collectors.toSet());
        var actualQuestionnaires = questionnaireIds.stream().map(id -> new QualifiedId(FhirUtils.qualifyId(id, ResourceType.Questionnaire))).collect(Collectors.toSet());

        return allowedQuestionnaires.containsAll(actualQuestionnaires);
    }

    private List<QuestionnaireWrapperModel> buildQuestionnaireWrapperModels(List<String> questionnaireIds, Map<String, FrequencyModel> frequenciesById, List<PlanDefinitionModel> planDefinitions) {
        List<QuestionnaireWrapperModel> result = new ArrayList<>();

        Map<String, QuestionnaireModel> questionnairesById = new HashMap<>();
        for(var questionnaire : planDefinitions.stream().flatMap(pd -> pd.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire())).collect(Collectors.toSet())) {
            questionnairesById.put(questionnaire.getId().toString(), questionnaire);
        }

        Map<String, List<ThresholdModel>> thresholdsById = new HashMap<>();
        for(var planDefinition : planDefinitions) {
            for(var qw : planDefinition.getQuestionnaires()) {
                String questionnaireId = qw.getQuestionnaire().getId().toString();
                if(thresholdsById.containsKey(questionnaireId)) {
                    throw new IllegalStateException(String.format("Questionnaire %s specified by multiple referenced plan definitions!", questionnaireId));
                }
                thresholdsById.put(questionnaireId, qw.getThresholds());
            }
        }

        for(var questionnaireId : questionnaireIds) {
            QuestionnaireWrapperModel wrapper = new QuestionnaireWrapperModel();

            // Set the questionnaire
            var questionnaire = questionnairesById.get(questionnaireId);
            wrapper.setQuestionnaire(questionnaire);

            // Set the frequency
            wrapper.setFrequency(frequenciesById.get(questionnaireId));

            // Initialize the 'satisfied-until' timestamp-
            initializeFrequencyTimestamp(wrapper);

            // Transfer thresholds
            var thresholds = thresholdsById.get(questionnaireId);
            wrapper.setThresholds(thresholds);

            result.add(wrapper);
        }

        return result;
    }

    private void validateReferences(CarePlanModel carePlanModel) throws AccessValidationException {
        // Validate questionnaires
        if(carePlanModel.getQuestionnaires() != null && !carePlanModel.getQuestionnaires().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupQuestionnaires(carePlanModel.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire().getId().toString()).collect(Collectors.toList()));
            validateAccess(lookupResult.getQuestionnaires());
        }

        // Validate planDefinitions
        if(carePlanModel.getPlanDefinitions() != null && !carePlanModel.getPlanDefinitions().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupPlanDefinitions(carePlanModel.getPlanDefinitions().stream().map(pd -> pd.getId().toString()).collect(Collectors.toList()));
            validateAccess(lookupResult.getPlanDefinitions());
        }
    }

    private void initializeAttributesForNewCarePlan(CarePlanModel carePlanModel) {
        // Ensure that no id is present on the careplan - the FHIR server will generate that for us.
        carePlanModel.setId(null);

        carePlanModel.setStatus(CarePlanStatus.ACTIVE);

        initializeTimestamps(carePlanModel);

        initializeFrequencyTimestamps(carePlanModel);

        initializeThresholds(carePlanModel);
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
            initializeFrequencyTimestamp(questionnaireWrapper);
        }
        refreshFrequencyTimestampForCarePlan(carePlanModel);
    }

    private void initializeFrequencyTimestamp(QuestionnaireWrapperModel questionnaireWrapperModel) {
        // Invoke 'next' twice - we want a bit of legroom initially.
        var nextDeadline = new FrequencyEnumerator(dateProvider.now(), questionnaireWrapperModel.getFrequency()).next().next().getPointInTime();
        questionnaireWrapperModel.setSatisfiedUntil(nextDeadline);
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

    private void initializeThresholds(CarePlanModel carePlanModel) {
        // Transfer questionnaire thresholds from the careplan(s) that this careplan instantiates.

        // Get the planDefinitions
        List<String> planDefinitionIds = carePlanModel.getPlanDefinitions().stream().map(pd -> pd.getId().toString()).collect(Collectors.toList());
        FhirLookupResult planDefinitionResult = fhirClient.lookupPlanDefinitions(planDefinitionIds);
        if(planDefinitionResult.getPlanDefinitions().size() != planDefinitionIds.size()) {
            throw new IllegalStateException("Could not look up every provided PlanDefinition!");
        }

        // Map the planDefinitions
        List<PlanDefinitionModel> planDefinitions = planDefinitionResult.getPlanDefinitions().stream().map(pd -> fhirMapper.mapPlanDefinition(pd, planDefinitionResult)).collect(Collectors.toList());

        // Transfer thresholds
        var allQuestionnaireWrappers = planDefinitions.stream().flatMap(pd -> pd.getQuestionnaires().stream()).collect(Collectors.toList());
        for(var questionnaireWrapper : carePlanModel.getQuestionnaires()) {
            if(questionnaireWrapper.getThresholds() != null && !questionnaireWrapper.getThresholds().isEmpty()) {
                throw new IllegalStateException(String.format("Error creating CarePlan: Thresholds already populated for questionnaire %s", questionnaireWrapper.getQuestionnaire().getId()));
            }

            // Look for a matching questionnaire
            for(var wrapperFromPlanDefinition: allQuestionnaireWrappers) {
                if(questionnaireWrapper.getQuestionnaire().getId().equals(wrapperFromPlanDefinition.getQuestionnaire().getId())) {
                    questionnaireWrapper.setThresholds(wrapperFromPlanDefinition.getThresholds());
                    break;
                }
            }
            if(questionnaireWrapper.getThresholds() == null) {
                throw new IllegalStateException(String.format("Could not locate thresholds for questionnaire %s! PlanDefinitions were: [%s]", questionnaireWrapper.getQuestionnaire().getId(), String.join(", ", planDefinitionIds)));
            }
        }
    }


}
