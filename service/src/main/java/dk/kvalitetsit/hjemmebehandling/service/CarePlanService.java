package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.Option;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CarePlanService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    private FhirObjectBuilder fhirObjectBuilder;

    private DateProvider dateProvider;

    public CarePlanService(FhirClient fhirClient, FhirMapper fhirMapper, FhirObjectBuilder fhirObjectBuilder, DateProvider dateProvider, AccessValidator accessValidator) {
        super(accessValidator);

        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.fhirObjectBuilder = fhirObjectBuilder;
        this.dateProvider = dateProvider;
    }

    public String createCarePlan(CarePlanModel carePlan) throws ServiceException, AccessValidationException {
        // Try to look up the patient in the careplan
        String cpr = carePlan.getPatient().getCpr();
        var patient = fhirClient.lookupPatientByCpr_new(cpr);

        // TODO: More validations should be performed - possibly?
        // If the patient did exist, check that no existing careplan exists for the patient
        if(patient.isPresent()) {
            String patientId = patient.get().getIdElement().toUnqualifiedVersionless().getValue();
            var carePlanResult = fhirClient.lookupCarePlansByPatientId_new(patientId);
            for(CarePlan cp : carePlanResult.getCarePlans()) {
                var carePlanModel = fhirMapper.mapCarePlan(cp, carePlanResult);
                if(isActive(carePlanModel)) {
                    throw new IllegalStateException(String.format("Could not create careplan for cpr %s: Another active careplan already exists!", cpr));
                }
            }

            // If we already knew the patient, replace the patient reference with the resource we just retrieved (to be able to map the careplan properly.)
            carePlan.setPatient(fhirMapper.mapPatient(patient.get()));
        }

        // Check that the referenced questionnaires and plandefinitions are valid for the client to access (and thus use).
        validateReferences(carePlan);

        // Mark how far into the future the careplan is 'satisfied' (a careplan is satisfied at a given point in time if it has not had its frequencies violated)
        initializeFrequencyTimestamps(carePlan);

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
            throw new ServiceException("Error saving CarePlan", e, ErrorKind.INTERNAL_SERVER_ERROR);
        }
    }

    public List<CarePlanModel> getCarePlansByCpr(String cpr) throws ServiceException {
        // Look up the patient so that we may look up careplans by patientId.
        Optional<Patient> patient = fhirClient.lookupPatientByCpr_new(cpr);
        if(!patient.isPresent()) {
            throw new IllegalStateException(String.format("Could not look up patient by cpr %s!", cpr));
        }

        // Look up the careplans along with related resources needed for mapping.
        String patientId = patient.get().getIdElement().toUnqualifiedVersionless().toString();
        FhirLookupResult lookupResult = lookupCarePlansByPatientId(patientId);
        if(lookupResult.getCarePlans().isEmpty()) {
            return List.of();
        }

        // Map the resourecs
        List<CarePlanModel> mappedCarePlans = lookupResult.getCarePlans()
                .stream()
                .map(cp -> fhirMapper.mapCarePlan(cp, lookupResult))
                .collect(Collectors.toList());

        return decorateCarePlans(mappedCarePlans);
    }

    private FhirLookupResult lookupCarePlanById(String carePlanId) {
        FhirLookupResult carePlanResult = fhirClient.lookupCarePlanById_new(carePlanId);
        return augmentWithQuestionnaires(carePlanResult);
    }

    private FhirLookupResult lookupCarePlansByPatientId(String patientId) {
        FhirLookupResult carePlanResult = fhirClient.lookupCarePlansByPatientId_new(patientId);
        return augmentWithQuestionnaires(carePlanResult);
    }

    private FhirLookupResult lookupCarePlansUnsatisfiedAt(Instant pointInTime) {
        FhirLookupResult carePlanResult = fhirClient.lookupCarePlansUnsatisfiedAt_new(pointInTime);
        return augmentWithQuestionnaires(carePlanResult);
    }

    private FhirLookupResult augmentWithQuestionnaires(FhirLookupResult carePlanResult) {
        // The FhirLookupResult includes the patient- and plandefinition-resources that we need,
        // but due to limitations of the FHIR server,  not the questionnaire-resources. Se wo look up those in a separate call.
        if(carePlanResult.getCarePlans().isEmpty()) {
            return carePlanResult;
        }

        // Get the related questionnaire-resources
        List<String> questionnaireIds = getQuestionnaireIds(carePlanResult.getCarePlans());
        FhirLookupResult questionnaireResult = fhirClient.lookupQuestionnaires_new(questionnaireIds);

        // Merge the results
        return carePlanResult.merge(questionnaireResult);
    }

    public List<CarePlanModel> getCarePlansWithUnsatisfiedSchedules() throws ServiceException {
        Instant pointInTime = dateProvider.now();
        FhirLookupResult lookupResult = lookupCarePlansUnsatisfiedAt(pointInTime);
        if(lookupResult.getCarePlans().isEmpty()) {
            return List.of();
        }

        // Map the resourecs
        List<CarePlanModel> mappedCarePlans = lookupResult.getCarePlans()
                .stream()
                .map(cp -> fhirMapper.mapCarePlan(cp, lookupResult))
                .collect(Collectors.toList());

        return decorateCarePlans(mappedCarePlans);
    }

    private List<CarePlanModel> decorateCarePlans(List<CarePlanModel> carePlans) {
        // Populate 'exceededQuestionnaires' list
        for(var carePlanModel : carePlans) {
            decorateCarePlan(carePlanModel);
        }

        return carePlans;
    }

    private CarePlanModel decorateCarePlan(CarePlanModel carePlanModel) {
        if(carePlanModel.getQuestionnaires() != null) {
            carePlanModel.setQuestionnairesWithUnsatisfiedSchedule(carePlanModel.getQuestionnaires()
                    .stream()
                    .filter(qw -> qw.getSatisfiedUntil().isBefore(dateProvider.now()))
                    .map(qw -> qw.getQuestionnaire().getId())
                    .collect(Collectors.toList()));
        }

        return carePlanModel;
    }

    public Optional<CarePlanModel> getCarePlanById(String carePlanId) throws ServiceException, AccessValidationException {
        FhirLookupResult lookupResult = lookupCarePlanById(carePlanId);

        Optional<CarePlan> carePlan = lookupResult.getCarePlan(FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan));
        if(!carePlan.isPresent()) {
            return Optional.empty();
        }

        // Validate that the user is allowed to access the careplan.
        validateAccess(carePlan.get());

        // Map the resource
        CarePlanModel mappedCarePlan = fhirMapper.mapCarePlan(carePlan.get(), lookupResult);
        return Optional.of(decorateCarePlan(mappedCarePlan));
    }

    public void resolveAlarm(String carePlanId) throws ServiceException, AccessValidationException {
        // Get the careplan

        // Validate access

        // Check that the 'satisfiedUntil'-timestamp is indeed in the past, throw an exception if not.

        // Recompute the 'satisfiedUntil'-timestamps

        // Save the updated carePlan

        throw new UnsupportedOperationException();
    }

    public void updateQuestionnaires(String carePlanId, List<String> questionnaireIds, Map<String, FrequencyModel> frequencies) throws ServiceException, AccessValidationException {
        // Look up the questionnaires to verify that they exist, throw an exception in case they don't.
        //List<Questionnaire> questionnaires = fhirClient.lookupQuestionnaires(questionnaireIds);
        FhirLookupResult questionnaireResult = fhirClient.lookupQuestionnaires_new(questionnaireIds);
        if(questionnaireResult.getQuestionnaires().size() != questionnaireIds.size()) {
            throw new ServiceException("Could not look up questionnaires to update!", ErrorKind.BAD_REQUEST);
        }

        // Validate that the client is allowed to reference the questionnaires.
        validateAccess(questionnaireResult.getQuestionnaires());

        // Look up the CarePlan, throw an exception in case it does not exist.
        String qualifiedId = FhirUtils.qualifyId(carePlanId, ResourceType.CarePlan);
        FhirLookupResult careplanResult = lookupCarePlanById(qualifiedId);
        if(careplanResult.getCarePlans().size() != 1 || !careplanResult.getCarePlan(qualifiedId).isPresent()) {
            throw new ServiceException(String.format("Could not lookup careplan with id %s!", qualifiedId), ErrorKind.BAD_REQUEST);
        }
        CarePlan carePlan = careplanResult.getCarePlan(qualifiedId).get();

        // Validate that the client is allowed to update the carePlan.
        validateAccess(carePlan);

        // Update the carePlan
        CarePlanModel carePlanModel = fhirMapper.mapCarePlan(carePlan, careplanResult.merge(questionnaireResult));
        Set<String> existingQuestionnaireIds = carePlanModel.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire().getId()).collect(Collectors.toSet());
        for(var questionnaireId : questionnaireIds) {
            if(existingQuestionnaireIds.contains(questionnaireId)) {
                continue;
            }

            var wrapper = buildQuestionnaireWrapperModel(questionnaireId, frequencies, questionnaireResult);
            carePlanModel.getQuestionnaires().add(wrapper);
        }

        // Save the updated CarePlan
        fhirClient.updateCarePlan(fhirMapper.mapCarePlanModel(carePlanModel));
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel(String questionnaireId, Map<String, FrequencyModel> frequencies, FhirLookupResult lookupResult) {
        var wrapper = new QuestionnaireWrapperModel();

        // Set the questionnaire
        var questionnaire = lookupResult.getQuestionnaire(questionnaireId).orElseThrow();
        wrapper.setQuestionnaire(fhirMapper.mapQuestionnaire(questionnaire));

        // Set the frequency
        wrapper.setFrequency(frequencies.get(questionnaireId));

        // Initialize the 'satisfied-until' timestamp-
        initializeFrequencyTimestamp(wrapper);

        return wrapper;
    }

    private CarePlanModel mapCarePlan(CarePlan carePlan, Patient patient) {
        return mapCarePlans(List.of(carePlan), patient).get(0);
    }

    private List<CarePlanModel> mapCarePlans(List<CarePlan> carePlans, Patient patient) {
        Map<String, Patient> patientsByCarePlanId = carePlans.stream().collect(Collectors.toMap(cp -> cp.getIdElement().toUnqualifiedVersionless().getValue(), cp -> patient));
        return mapCarePlans(carePlans, patientsByCarePlanId, dateProvider.now());
    }

    private List<CarePlanModel> mapCarePlans(List<CarePlan> carePlans, Map<String, Patient> patientsByCarePlanId, Instant pointInTime) {
        if(carePlans.isEmpty()) {
            return List.of();
        }

        List<CarePlanModel> result = new ArrayList<>();

        // Look up the questionnaires and include them in the result.
        Map<String, List<QuestionnaireWrapperModel>> questionnairesByCarePlanId = getQuestionnairesByCarePlanId(carePlans);
        // Look up the plan definitions and include them in the result
        Map<String, List<PlanDefinitionModel>> planDefinitionsByCarePlanId = getPlanDefinitionsByCarePlanId(carePlans);
        for(var carePlan : carePlans) {
            var carePlanModel = fhirMapper.mapCarePlan(carePlan);

            if(!patientsByCarePlanId.containsKey(carePlanModel.getId())) {
                throw new IllegalStateException(String.format("Could not look up Patient for CarePlan %s!", carePlanModel.getId()));
            }
            Patient patient = patientsByCarePlanId.get(carePlanModel.getId());
            carePlanModel.setPatient(fhirMapper.mapPatient(patient));

            if(!questionnairesByCarePlanId.containsKey(carePlanModel.getId())) {
                // The Careplan simply may not have any questionnaires attached, so we continue.
                continue;
            }
            List<QuestionnaireWrapperModel> questionnaires = questionnairesByCarePlanId.get(carePlanModel.getId());
            carePlanModel.setQuestionnaires(questionnaires);

            List<PlanDefinitionModel> planDefinitions = planDefinitionsByCarePlanId.get(carePlanModel.getId());
            carePlanModel.setPlanDefinitions(planDefinitions);

            // Populate 'exceededQuestionnaires' list
            carePlanModel.setQuestionnairesWithUnsatisfiedSchedule(new ArrayList<>());
            for(var activity : carePlan.getActivity()) {
                // Check whether the 'activity-satisfied-until' extension has a value that is in the past.
                // If so, the activity has not been fulfilled before its deadline.
                var activitySatisfiedUntil = ExtensionMapper.extractActivitySatisfiedUntil(activity.getDetail().getExtension());
                if(activitySatisfiedUntil.isBefore(pointInTime)) {
                    String questionnaireId = activity.getDetail().getInstantiatesCanonical().get(0).getValue();
                    carePlanModel.getQuestionnairesWithUnsatisfiedSchedule().add(questionnaireId);
                }
            }

            result.add(carePlanModel);
        }

        return result;
    }

    private Map<String, List<QuestionnaireWrapperModel>> getQuestionnairesByCarePlanId(List<CarePlan> carePlans) {
        // We want a map from carePlanIds to their questionnaires, and we would like to make only one invocation of the FhirClient.

        // Get the questionnaireIds
        List<String> questionnaireIds = getQuestionnaireIds(carePlans);

        // Fetch the Questionnaires
        List<QuestionnaireModel> questionnaires = fhirClient.lookupQuestionnaires(questionnaireIds)
                .stream()
                .map(q -> fhirMapper.mapQuestionnaire(q))
                .collect(Collectors.toList());

        // Build the result
        return carePlans
                .stream()
                .collect(Collectors.toMap(cp -> cp.getIdElement().toUnqualifiedVersionless().getValue(), cp -> getQuestionnairesForCarePlan(cp, questionnaires)));
    }

    private List<String> getQuestionnaireIds(List<CarePlan> carePlans) {
        return carePlans
                .stream()
                .flatMap(cp -> cp.getActivity().stream().map(a -> getQuestionnaireId(a.getDetail())))
                .collect(Collectors.toList());
    }

    private List<QuestionnaireWrapperModel> getQuestionnairesForCarePlan(CarePlan carePlan, List<QuestionnaireModel> questionnaires) {
        // For each activity on the careplan, we need to find the corresponding questionnaire and frequency.
        List<QuestionnaireWrapperModel> result = new ArrayList<>();

        Map<String, QuestionnaireModel> questionnairesById = questionnaires.stream().collect(Collectors.toMap(q -> q.getId(), q -> q));

        for(var activity : carePlan.getActivity()) {
            var detail = activity.getDetail();
            String questionnaireId = getQuestionnaireId(detail);

            // Get the questionnaire
            if(!questionnairesById.containsKey(questionnaireId)) {
                throw new IllegalStateException(String.format("No questionnaire present for id %s!", questionnaireId));
            }
            QuestionnaireModel questionnaire = questionnairesById.get(questionnaireId);

            // Get the frequency
            FrequencyModel frequency = getFrequencyModel(detail);

            // Get the satisfied-timestamp
            Instant activitySatisfiedUntil = getActivitySatisfiedUntil(detail);

            result.add(new QuestionnaireWrapperModel(questionnaire, frequency, activitySatisfiedUntil));
        }

        return result;
    }

    private Map<String, List<PlanDefinitionModel>> getPlanDefinitionsByCarePlanId(List<CarePlan> carePlans) {
        // We want a map from carePlanIds to their planDefinitions, and we would like to make only one invocation of the FhirClient.

        // Get the planDefinitionIds
        List<String> planDefinitionIds = getPlanDefinitionIds(carePlans);

        // Fetch the Questionnaires
        List<PlanDefinitionModel> planDefinitions = fhirClient.lookupPlanDefinitions(planDefinitionIds)
                .stream()
                .map(pd -> fhirMapper.mapPlanDefinition(pd))
                .collect(Collectors.toList());

        // Build the result
        return carePlans
                .stream()
                .collect(Collectors.toMap(cp -> cp.getIdElement().toUnqualifiedVersionless().getValue(), cp -> getPlanDefinitionsForCarePlan(cp, planDefinitions)));
    }

    private List<String> getPlanDefinitionIds(List<CarePlan> carePlans) {
        return carePlans
                .stream()
                .flatMap(cp -> cp.getInstantiatesCanonical().stream().map(ic -> ic.getValue()))
                .collect(Collectors.toList());
    }

    private List<PlanDefinitionModel> getPlanDefinitionsForCarePlan(CarePlan carePlan, List<PlanDefinitionModel> planDefinitionModels) {
        List<String> planDefinitionIds = carePlan.getInstantiatesCanonical()
                .stream()
                .map(ct -> ct.getValue())
                .collect(Collectors.toList());

        return planDefinitionModels
                .stream()
                .filter(pd -> planDefinitionIds.contains(pd.getId()))
                .collect(Collectors.toList());
    }

    private Map<String, FrequencyModel> getFrequenciesById(CarePlan carePlan) {
        return carePlan
                .getActivity()
                .stream()
                .map(a -> a.getDetail())
                .collect(Collectors.toMap(d -> getQuestionnaireId(d), d -> getFrequencyModel(d)));
    }

    private Map<String, FrequencyModel> getFrequenciesByQuestionnaireId(List<CarePlan> carePlans) {
        Stream<CarePlan.CarePlanActivityDetailComponent> detailComponents = carePlans
                .stream()
                .flatMap(cp -> cp.getActivity().stream().map(a -> a.getDetail()));

        return detailComponents.collect(Collectors.toMap(d -> getQuestionnaireId(d), d -> getFrequencyModel(d)));
    }

    private String getQuestionnaireId(CarePlan.CarePlanActivityDetailComponent detail) {
        if(detail.getInstantiatesCanonical() == null || detail.getInstantiatesCanonical().size() != 1) {
            throw new IllegalStateException("Expected InstantiatesCanonical to be present, and to contain exactly one value!");
        }
        return detail.getInstantiatesCanonical().get(0).getValue();
    }

    private FrequencyModel getFrequencyModel(CarePlan.CarePlanActivityDetailComponent detail) {
        if(detail.getScheduled() == null || !(detail.getScheduled() instanceof Timing)) {
            throw new IllegalStateException("Expected Scheduled to be a Timing-object!");
        }
        return fhirMapper.mapTiming((Timing) detail.getScheduled());
    }

    private Instant getActivitySatisfiedUntil(CarePlan.CarePlanActivityDetailComponent detail) {
        return ExtensionMapper.extractActivitySatisfiedUntil(detail.getExtension());
    }

    private QuestionnaireWrapperModel wrapQuestionnaire(Questionnaire questionnaire, Map<String, FrequencyModel> frequenciesById) {
        QuestionnaireWrapperModel wrapper = new QuestionnaireWrapperModel();

        String questionnaireId = questionnaire.getIdElement().toUnqualifiedVersionless().toString();
        if(!frequenciesById.containsKey(questionnaireId)) {
            throw new IllegalStateException(String.format("No frequency present for questionnaireId %s!", questionnaireId));
        }

        wrapper.setQuestionnaire(fhirMapper.mapQuestionnaire(questionnaire));
        wrapper.setFrequency(frequenciesById.get(questionnaireId));

        return wrapper;
    }

    private boolean isActive(CarePlanModel carePlan) {
        return carePlan.getEndDate() == null;
    }

    private void validateReferences(CarePlanModel carePlanModel) throws AccessValidationException {
        // Validate questionnaires
        if(carePlanModel.getQuestionnaires() != null && !carePlanModel.getQuestionnaires().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupQuestionnaires_new(carePlanModel.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire().getId()).collect(Collectors.toList()));
            validateAccess(lookupResult.getQuestionnaires());
        }

        // Validate planDefinitions
        if(carePlanModel.getPlanDefinitions() != null && !carePlanModel.getPlanDefinitions().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupPlanDefinitions_new(carePlanModel.getPlanDefinitions().stream().map(pd -> pd.getId()).collect(Collectors.toList()));
            validateAccess(lookupResult.getPlanDefinitions());
        }
    }

    private void initializeFrequencyTimestamps(CarePlanModel carePlanModel) {
        // Mark how far into the future the careplan is 'satisfied' (a careplan is satisfied at a given point in time if it has not had its frequencies violated)
        for(var questionnaireWrapper : carePlanModel.getQuestionnaires()) {
            initializeFrequencyTimestamp(questionnaireWrapper);
        }
        var carePlanSatisfiedUntil = carePlanModel.getQuestionnaires().stream().map(qw -> qw.getSatisfiedUntil()).min(Comparator.naturalOrder()).orElse(Instant.MAX);
        carePlanModel.setSatisfiedUntil(carePlanSatisfiedUntil);
    }

    private void initializeFrequencyTimestamp(QuestionnaireWrapperModel questionnaireWrapperModel) {
        var nextDeadline = new FrequencyEnumerator(dateProvider.now(), questionnaireWrapperModel.getFrequency()).next().next().getPointInTime();
        questionnaireWrapperModel.setSatisfiedUntil(nextDeadline);
    }
}
