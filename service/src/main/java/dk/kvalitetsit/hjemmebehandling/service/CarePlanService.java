package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CarePlanService {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    private FhirObjectBuilder fhirObjectBuilder;

    public CarePlanService(FhirClient fhirClient, FhirMapper fhirMapper, FhirObjectBuilder fhirObjectBuilder) {
        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.fhirObjectBuilder = fhirObjectBuilder;
    }

    public String createCarePlan(String cpr, String planDefinitionId) throws ServiceException {
        // Look up the Patient identified by the cpr.
        Optional<Patient> patient = fhirClient.lookupPatientByCpr(cpr);
        if(!patient.isPresent()) {
            throw new IllegalStateException(String.format("Could not look up Patient by cpr %s!", cpr));
        }

        // Look up the PlanDefinition based on the planDefinitionId
        Optional<PlanDefinition> planDefinition = fhirClient.lookupPlanDefinition(planDefinitionId);
        if(!planDefinition.isPresent()) {
            throw new IllegalStateException(String.format("Could not look up PlanDefinition by id %s!", planDefinitionId));
        }

        // Based on that, build a CarePlan
        CarePlan carePlan = fhirObjectBuilder.buildCarePlan(patient.get(), planDefinition.get());

        // Save the carePlan, return the id.
        try {
            return fhirClient.saveCarePlan(carePlan);
        }
        catch(Exception e) {
            throw new ServiceException("Error saving CarePlan", e);
        }
    }

    public List<CarePlanModel> getCarePlansByCpr(String cpr) throws ServiceException {
        // Look up the patient so that we may look up careplans by patientId.
        Optional<Patient> patient = fhirClient.lookupPatientByCpr(cpr);
        if(!patient.isPresent()) {
            throw new IllegalStateException(String.format("Could not look up patient by cpr %s!", cpr));
        }

        List<CarePlan> carePlans = fhirClient.lookupCarePlansByPatientId(patient.get().getIdElement().toUnqualifiedVersionless().toString());
        if(carePlans.isEmpty()) {
            return List.of();
        }

        List<CarePlanModel> result = carePlans.stream().map(cp -> fhirMapper.mapCarePlan(cp)).collect(Collectors.toList());

        // Set patient on each careplan in the result.
        result.forEach(cp -> cp.setPatient(fhirMapper.mapPatient(patient.get())));

        // Look up the questionnaires and include them in the result.
        Map<String, List<QuestionnaireWrapperModel>> questionnairesByCarePlanId = getQuestionnairesByCarePlanId(carePlans);
        // Look up the plan definitions and include them in the result
        Map<String, List<PlanDefinitionModel>> planDefinitionsByCarePlanId = getPlanDefinitionsByCarePlanId(carePlans);
        for(CarePlanModel carePlanModel : result) {
            if(!questionnairesByCarePlanId.containsKey(carePlanModel.getId())) {
                // The Careplan simply may not have any questionnaires attached, so we continue.
                continue;
            }
            List<QuestionnaireWrapperModel> questionnaires = questionnairesByCarePlanId.get(carePlanModel.getId());
            carePlanModel.setQuestionnaires(questionnaires);

            List<PlanDefinitionModel> planDefinitions = planDefinitionsByCarePlanId.get(carePlanModel.getId());
            carePlanModel.setPlanDefinitions(planDefinitions);
        }

        return result;
    }

    public Optional<CarePlanModel> getCarePlanById(String carePlanId) {
        Optional<CarePlan> carePlan = fhirClient.lookupCarePlanById(carePlanId);

        if(!carePlan.isPresent()) {
            return Optional.empty();
        }

        CarePlanModel carePlanModel = fhirMapper.mapCarePlan(carePlan.get());

        // Look up the subject and include it in the result.
        Optional<Patient> patient = fhirClient.lookupPatientById(carePlan.get().getSubject().getReference());
        if(!patient.isPresent()) {
            throw new IllegalStateException(String.format("Could not look up subject for CarePlan %s!", carePlanId));
        }
        carePlanModel.setPatient(fhirMapper.mapPatient(patient.get()));

        // Look up the questionnaires and include them in the result.
        List<QuestionnaireWrapperModel> questionnaires = getQuestionnaires(carePlan.get());
        carePlanModel.setQuestionnaires(questionnaires);

        // Look up the plan definitions and include them in the result
        Map<String, List<PlanDefinitionModel>> planDefinitions = getPlanDefinitionsByCarePlanId(List.of(carePlan.get()));
        carePlanModel.setPlanDefinitions(planDefinitions.get(carePlan.get().getId()));

        return Optional.of(carePlanModel);
    }

    public void updateQuestionnaires(String carePlanId, List<String> questionnaireIds, Map<String, FrequencyModel> frequencies) throws ServiceException {
        // Look up the questionnaires to verify that they exist, throw an exception in case they don't.
        List<Questionnaire> questionnaires = fhirClient.lookupQuestionnaires(questionnaireIds);
        if(questionnaires == null || questionnaires.size() != questionnaireIds.size()) {
            throw new ServiceException("Could not look up questionnaires to update!");
        }

        // Look up the CarePlan, throw an exception in case it does not exist.
        Optional<CarePlan> carePlan = fhirClient.lookupCarePlanById(carePlanId);
        if(!carePlan.isPresent()) {
            throw new ServiceException(String.format("Could not lookup careplan with id %s!", carePlanId));
        }

        // Update the carePlan
        Map<String, Timing> timings = mapFrequencies(frequencies);
        fhirObjectBuilder.setQuestionnairesForCarePlan(carePlan.get(), questionnaires, timings);

        // Save the updated CarePlan
        fhirClient.updateCarePlan(carePlan.get());
    }

    private Map<String, Timing> mapFrequencies(Map<String, FrequencyModel> frequencies) {
        return frequencies
                .entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), fhirMapper.mapFrequencyModel(e.getValue())))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    private List<QuestionnaireWrapperModel> getQuestionnaires(CarePlan carePlan) {
        // We want to fetch all the questionnaires using one invocation of the FhirClient.

        // Build a map from id's to frequencies so that we may associate questionnaires to their frequency later on.
        final Map<String, FrequencyModel> frequenciesById = getFrequenciesById(carePlan);

        // Fetch the questionnaires
        List<String> questionnaireIds = frequenciesById.keySet().stream().collect(Collectors.toList());
        List<Questionnaire> questionnaires = fhirClient.lookupQuestionnaires(questionnaireIds);

        // Associate questionnaires with their frequencies.
        return questionnaires
                .stream()
                .map(q -> wrapQuestionnaire(q, frequenciesById))
                .collect(Collectors.toList());
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
                .collect(Collectors.toMap(cp -> cp.getId(), cp -> getQuestionnairesForCarePlan(cp, questionnaires)));
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
            String questionnaireId = getQuestionnaireId(activity.getDetail());

            // Get the questionnaire
            if(!questionnairesById.containsKey(questionnaireId)) {
                throw new IllegalStateException(String.format("No questionnaire present for id %s!", questionnaireId));
            }
            QuestionnaireModel questionnaire = questionnairesById.get(questionnaireId);

            // Get the frequency
            FrequencyModel frequency = getFrequencyModel(activity.getDetail());

            result.add(new QuestionnaireWrapperModel(questionnaire, frequency));
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
                .collect(Collectors.toMap(cp -> cp.getId(), cp -> getPlanDefinitionsForCarePlan(cp, planDefinitions)));
    }

    private List<String> getPlanDefinitionIds(List<CarePlan> carePlans) {
        return carePlans
                .stream()
                .flatMap(cp -> cp.getInstantiatesCanonical().stream().map(ic -> ic.getValue()))
                .collect(Collectors.toList());
    }

    private List<PlanDefinitionModel> getPlanDefinitionsForCarePlan(CarePlan carePlan, List<PlanDefinitionModel> planDefinitionModels) {
        return planDefinitionModels
                .stream()
                .filter(pd -> carePlan.getInstantiatesCanonical().contains(pd.getId()))
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
}
