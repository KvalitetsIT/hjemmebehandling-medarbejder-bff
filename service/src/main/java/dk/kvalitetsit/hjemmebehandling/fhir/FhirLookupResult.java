package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FhirLookupResult {
    private Map<String, CarePlan> carePlansById;
    private Map<String, PlanDefinition> planDefinitionsById;
    private Map<String, Questionnaire> questionnairesById;

    private FhirLookupResult() {
        carePlansById = new HashMap<>();
        planDefinitionsById = new HashMap<>();
        questionnairesById = new HashMap<>();
    }

    public static FhirLookupResult fromBundle(Bundle bundle) {
        FhirLookupResult result = new FhirLookupResult();

        bundle.getEntry().forEach(e -> result.addResource(e.getResource()));

        return result;
    }

    public static FhirLookupResult fromResource(Resource resource) {
        FhirLookupResult result = new FhirLookupResult();

        result.addResource(resource);

        return result;
    }

    public PlanDefinition getPlanDefinition(String planDefinitionId) {
        return getResource(planDefinitionId, planDefinitionsById);
    }

    public List<PlanDefinition> getPlanDefinitions() {
        return getResources(planDefinitionsById);
    }

    public Questionnaire getQuestionnaire(String questionnaireId) {
        return getResource(questionnaireId, questionnairesById);
    }

    public List<Questionnaire> getQuestionnaires() {
        return getResources(questionnairesById);
    }

    private <T extends Resource> T getResource(String resourceId, Map<String, T> resourcesById) {
        if(!resourcesById.containsKey(resourceId)) {
            throw new IllegalArgumentException(String.format("No resource with id %s was present!", resourceId));
        }
        return resourcesById.get(resourceId);
    }

    private <T extends Resource> List<T> getResources(Map<String, T> resourcesById) {
        return resourcesById.values().stream().collect(Collectors.toList());
    }

    private void addResource(Resource resource) {
        String resourceId = resource.getIdElement().toUnqualifiedVersionless().getValue();
        switch(resource.getResourceType()) {
            case PlanDefinition:
                planDefinitionsById.put(resourceId, (PlanDefinition) resource);
                break;
            case Questionnaire:
                questionnairesById.put(resourceId, (Questionnaire) resource);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown resource type: %s", resource.getResourceType().toString()));
        }
    }
}
