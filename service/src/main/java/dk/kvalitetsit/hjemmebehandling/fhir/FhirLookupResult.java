package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FhirLookupResult {
    private Map<String, CarePlan> carePlansById;
    private Map<String, Organization> organizationsById;
    private Map<String, Patient> patientsById;
    private Map<String, PlanDefinition> planDefinitionsById;
    private Map<String, Questionnaire> questionnairesById;
    private Map<String, QuestionnaireResponse> questionnaireResponsesById;
    private Map<String, Practitioner> practitionersById;

    private FhirLookupResult() {
        // Using LinkedHashMap preserves the insertion order (necessary for eg. returning sorted results).
        carePlansById = new LinkedHashMap<>();
        organizationsById = new LinkedHashMap<>();
        patientsById = new LinkedHashMap<>();
        planDefinitionsById = new LinkedHashMap<>();
        questionnairesById = new LinkedHashMap<>();
        questionnaireResponsesById = new LinkedHashMap<>();
        practitionersById = new LinkedHashMap<>();
    }

    public static FhirLookupResult fromBundle(Bundle bundle) {
        FhirLookupResult result = new FhirLookupResult();

        bundle.getEntry().forEach(e -> result.addResource(e.getResource()));

        return result;
    }

    public static FhirLookupResult fromResource(Resource resource) {
        return fromResources(resource);
    }

    public static FhirLookupResult fromResources(Resource... resources) {
        FhirLookupResult result = new FhirLookupResult();

        for(Resource resource : resources) {
            result.addResource(resource);
        }

        return result;
    }

    public Optional<CarePlan> getCarePlan(String carePlanId) {
        return getResource(carePlanId, carePlansById);
    }

    public List<CarePlan> getCarePlans() {
        return getResources(carePlansById);
    }

    public Optional<Organization> getOrganization(String organizationId) {
        return getResource(organizationId, organizationsById);
    }

    public List<Organization> getOrganizations() {
        return getResources(organizationsById);
    }

    public Optional<Patient> getPatient(String patientId) {
        return getResource(patientId, patientsById);
    }

    public List<Patient> getPatients() {
        return getResources(patientsById);
    }

    public Optional<PlanDefinition> getPlanDefinition(String planDefinitionId) {
        return getResource(planDefinitionId, planDefinitionsById);
    }

    public List<PlanDefinition> getPlanDefinitions() {
        return getResources(planDefinitionsById);
    }

    public Optional<Questionnaire> getQuestionnaire(String questionnaireId) {
        return getResource(questionnaireId, questionnairesById);
    }

    public List<Questionnaire> getQuestionnaires() {
        return getResources(questionnairesById);
    }

    public Optional<QuestionnaireResponse> getQuestionnaireResponse(String questionnaireResponseId) {
        return getResource(questionnaireResponseId, questionnaireResponsesById);
    }

    public List<QuestionnaireResponse> getQuestionnaireResponses() {
        return getResources(questionnaireResponsesById);
    }

    public Optional<Practitioner> getPractitioner(String practitionerId) {
        return getResource(practitionerId, practitionersById);
    }

    public List<Practitioner> getPractitioners() {
        return getResources(practitionersById);
    }

    public FhirLookupResult merge(FhirLookupResult result) {
        for(CarePlan carePlan : result.carePlansById.values()) {
            addResource(carePlan);
        }
        for(Organization organization : result.organizationsById.values()) {
            addResource(organization);
        }
        for(Patient patient : result.patientsById.values()) {
            addResource(patient);
        }
        for(PlanDefinition planDefinition : result.planDefinitionsById.values()) {
            addResource(planDefinition);
        }
        for(Questionnaire questionnaire: result.questionnairesById.values()) {
            addResource(questionnaire);
        }
        for(QuestionnaireResponse questionnaireResponse: result.questionnaireResponsesById.values()) {
            addResource(questionnaireResponse);
        }
        for(Practitioner practitioner: result.practitionersById.values()) {
            addResource(practitioner);
        }

        return this;
    }

    public List<DomainResource> values() {
        return Stream.of(
                        carePlansById.values().stream(),
                        organizationsById.values().stream(),
                        patientsById.values().stream(),
                        planDefinitionsById.values().stream(),
                        questionnairesById.values().stream(),
                        questionnaireResponsesById.values().stream()
                )
                .flatMap(s -> s)
                .collect(Collectors.toList());
    }

    private <T extends Resource> Optional<T> getResource(String resourceId, Map<String, T> resourcesById) {
        if(!resourcesById.containsKey(resourceId)) {
            return Optional.empty();
        }
        return Optional.of(resourcesById.get(resourceId));
    }

    private <T extends Resource> List<T> getResources(Map<String, T> resourcesById) {
        return resourcesById.values().stream().collect(Collectors.toList());
    }

    private void addResource(Resource resource) {
        String resourceId = resource.getIdElement().toUnqualifiedVersionless().getValue();
        switch(resource.getResourceType()) {
            case CarePlan:
                carePlansById.put(resourceId, (CarePlan) resource);
                break;
            case Organization:
                organizationsById.put(resourceId, (Organization) resource);
                break;
            case Patient:
                patientsById.put(resourceId, (Patient) resource);
                break;
            case PlanDefinition:
                planDefinitionsById.put(resourceId, (PlanDefinition) resource);
                break;
            case Questionnaire:
                questionnairesById.put(resourceId, (Questionnaire) resource);
                break;
            case QuestionnaireResponse:
                questionnaireResponsesById.put(resourceId, (QuestionnaireResponse) resource);
                break;
            case Practitioner:
                practitionersById.put(resourceId, (Practitioner) resource);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown resource type: %s", resource.getResourceType().toString()));
        }
    }
}
