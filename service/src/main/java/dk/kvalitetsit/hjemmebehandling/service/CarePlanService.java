package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.model.FrequencyModel;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Provider;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Patient patient = fhirClient.lookupPatient(cpr);

        // Look up the PlanDefinition based on the planDefinitionId
        PlanDefinition planDefinition = fhirClient.lookupPlanDefinition(planDefinitionId);

        // Based on that, build a CarePlan
        CarePlan carePlan = fhirObjectBuilder.buildCarePlan(patient, planDefinition);

        // Save the carePlan, return the id.
        try {
            return fhirClient.saveCarePlan(carePlan);
        }
        catch(Exception e) {
            throw new ServiceException("Error saving CarePlan", e);
        }
    }

    public void updateQuestionnaires(String carePlanId, List<String> questionnaireIds, Map<String, FrequencyModel> frequencies) throws ServiceException {
        // Look up the questionnaires to verify that they exist, throw an exception in case they don't.
        List<Questionnaire> questionnaires = fhirClient.lookupQuestionnaires(questionnaireIds);
        if(questionnaires == null || questionnaires.size() != questionnaireIds.size()) {
            throw new ServiceException("Could not look up questionnaires to update!");
        }

        // Look up the CarePlan, throw an exception in case it does not exist.
        CarePlan carePlan = fhirClient.lookupCarePlan(carePlanId);
        if(carePlan == null) {
            throw new ServiceException(String.format("Could not lookup careplan with id %s!", carePlanId));
        }

        // Update the carePlan
        Map<String, Timing> timings = mapFrequencies(frequencies);
        fhirObjectBuilder.setQuestionnairesForCarePlan(carePlan, questionnaires, timings);

        // Save the updated CarePlan
        fhirClient.updateCarePlan(carePlan);
    }

    private Map<String, Timing> mapFrequencies(Map<String, FrequencyModel> frequencies) {
        return frequencies
                .entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), fhirMapper.mapFrequencyModel(e.getValue())))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }
}
