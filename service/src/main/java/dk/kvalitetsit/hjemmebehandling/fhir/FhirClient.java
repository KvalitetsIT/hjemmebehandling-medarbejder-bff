package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FhirClient {
    private static final Logger logger = LoggerFactory.getLogger(FhirClient.class);

    private FhirContext context;
    private String endpoint;

    public FhirClient(FhirContext context, String endpoint) {
        this.context = context;
        this.endpoint = endpoint;
    }

    public List<CarePlan> lookupCarePlansByPatientId(String patientId) {
        return lookupByCriterion(CarePlan.class, CarePlan.PATIENT.hasId(patientId));
    }

    public Optional<CarePlan> lookupCarePlanById(String carePlanId) {
        return lookupById(carePlanId, CarePlan.class);
    }

    public Optional<Patient> lookupPatientById(String patientId) {
        return lookupById(patientId, Patient.class);
    }

    public Optional<Patient> lookupPatientByCpr(String cpr) {
        return lookupSingletonByCriterion(Patient.class, Patient.IDENTIFIER.exactly().systemAndValues(Systems.CPR, cpr));
    }

    public List<Patient> lookupPatientsById(Collection<String> patientIds) {
        return lookupByCriterion(Patient.class, Patient.RES_ID.exactly().codes(patientIds));
    }

    public Optional<QuestionnaireResponse> lookupQuestionnaireResponseById(String questionnaireResponseId) {
        return lookupById(questionnaireResponseId, QuestionnaireResponse.class);
    }

    public List<QuestionnaireResponse> lookupQuestionnaireResponses(String carePlanId, List<String> questionnaireIds) {
        var questionnaireCriterion = QuestionnaireResponse.QUESTIONNAIRE.hasAnyOfIds(questionnaireIds);
        var basedOnCriterion = QuestionnaireResponse.BASED_ON.hasId(carePlanId);
        return lookupByCriteria(QuestionnaireResponse.class, questionnaireCriterion, basedOnCriterion);
    }

    public List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) {
        var codes = statuses.stream().map(s -> s.toString()).collect(Collectors.toList());
        var criterion = new TokenClientParam(SearchParameters.EXAMINATION_STATUS).exactly().codes(codes.toArray(new String[codes.size()]));
        return lookupByCriterion(QuestionnaireResponse.class, criterion);
    }

    public List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatus(ExaminationStatus status) {
        return lookupQuestionnaireResponsesByStatus(List.of(status));
    }

    public String saveCarePlan(CarePlan carePlan) {
        return save(carePlan);
    }

    public String saveCarePlan(CarePlan carePlan, Patient patient) {
        // Build a transaction bundle.
        var bundle = new BundleBuilder().buildCarePlanBundle(carePlan, patient);

        return saveInTransaction(bundle, ResourceType.CarePlan);
    }

    public String savePatient(Patient patient) {
        return save(patient);
    }

    public String saveQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) {
        return save(questionnaireResponse);
    }

    public Optional<PlanDefinition> lookupPlanDefinition(String planDefinitionId) {
        return lookupById(planDefinitionId, PlanDefinition.class);
    }

    public FhirLookupResult lookupPlanDefinitions() {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        var query = client
                .search()
                .forResource(PlanDefinition.class)
                .include(PlanDefinition.INCLUDE_DEFINITION);

        Bundle bundle = (Bundle) query.execute();

        return FhirLookupResult.fromBundle(bundle);
    }

    public List<PlanDefinition> lookupPlanDefinitions(Collection<String> planDefinitionIds) {
        return lookupByCriterion(PlanDefinition.class, PlanDefinition.RES_ID.exactly().codes(planDefinitionIds));
    }

    public List<Questionnaire> lookupQuestionnaires(Collection<String> questionnaireIds) {
        return lookupByCriterion(Questionnaire.class, Questionnaire.RES_ID.exactly().codes(questionnaireIds));
    }

    public void updateCarePlan(CarePlan carePlan) {
        update(carePlan);
    }

    public void updateQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) {
        update(questionnaireResponse);
    }

    private <T extends Resource> Optional<T> lookupSingletonByCriterion(Class<T> resourceClass, ICriterion<?> criterion) {
        List<T> result = lookupByCriterion(resourceClass, criterion);

        if(result == null || result.isEmpty()) {
            return Optional.empty();
        }
        if(result.size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", resourceClass.getName()));
        }
        return Optional.of(result.get(0));
    }

    private <T extends Resource> List<T> lookupAll(Class<T> resourceClass) {
        return lookupByCriteria(resourceClass);
    }

    private <T extends Resource> List<T> lookupByCriterion(Class<T> resourceClass, ICriterion<?> criterion) {
        return lookupByCriteria(resourceClass, criterion);
    }

    private <T extends Resource> List<T> lookupByCriteria(Class<T> resourceClass, ICriterion<?>... criteria) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        var query = client
                .search()
                .forResource(resourceClass);
        if(criteria.length > 0) {
            query = query.where(criteria[0]);
            for(int i = 1; i < criteria.length; i++) {
                query = query.and(criteria[i]);
            }
        }

        Bundle bundle = (Bundle) query.execute();
        return bundle.getEntry().stream().map(e -> ((T) e.getResource())).collect(Collectors.toList());
    }

    private <T extends Resource> Optional<T> lookupById(String id, Class<T> resourceClass) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        try {
            T resource = client
                    .read()
                    .resource(resourceClass)
                    .withId(id)
                    .execute();
            return Optional.of(resource);
        }
        catch(ResourceNotFoundException e) {
            // Swallow the exception - corresponds to a 404 response
            return Optional.empty();
        }
    }

    private <T extends Resource> String save(Resource resource) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        MethodOutcome outcome = client.create().resource(resource).execute();
        if(!outcome.getCreated()) {
            throw new IllegalStateException(String.format("Tried to create resource of type %s, but it was not created!", resource.getResourceType().name()));
        }
        return outcome.getId().toUnqualifiedVersionless().getIdPart();
    }

    private String saveInTransaction(Bundle transactionBundle, ResourceType resourceType) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        // Execute the transaction
        var responseBundle = client.transaction().withBundle(transactionBundle).execute();

        // Locate the 'primary' entry in the response
        var id = "";
        for(var responseEntry : responseBundle.getEntry()) {
            var status = responseEntry.getResponse().getStatus();
            var location = responseEntry.getResponse().getLocation();
            if(!status.startsWith("201")) {
                throw new IllegalStateException(String.format("Creating %s failed. Received unwanted http statuscode: %s", resourceType, status));
            }
            if(location.startsWith(resourceType.toString())) {
                id = location.replaceFirst("/_history.*$", "");
            }
        }

        if(id.isEmpty()) {
            throw new IllegalStateException("Could not locate location-header in response when executing transaction.");
        }
        return id;
    }

    private <T extends Resource> void update(Resource resource) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);
        client.update().resource(resource).execute();
    }
}
