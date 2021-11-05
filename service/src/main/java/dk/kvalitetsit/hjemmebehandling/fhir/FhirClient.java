package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
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
        throw new UnsupportedOperationException();
        //return lookupById(patientId, Patient.class);
    }

    public Optional<QuestionnaireResponse> lookupQuestionnaireResponseById(String questionnaireResponseId) {
        return lookupById(questionnaireResponseId, QuestionnaireResponse.class);
    }

    public List<QuestionnaireResponse> lookupQuestionnaireResponses(String cpr, List<String> questionnaireIds) {
        var questionnaireCriterion = QuestionnaireResponse.QUESTIONNAIRE.hasAnyOfIds(questionnaireIds);
        var subjectCriterion = QuestionnaireResponse.SUBJECT.hasChainedProperty("Patient", Patient.IDENTIFIER.exactly().systemAndValues(Systems.CPR, cpr));

        return lookupByCriteria(QuestionnaireResponse.class, questionnaireCriterion, subjectCriterion);
    }

    public List<QuestionnaireResponse> lookupQuestionnaireResponsesByExaminationStatus(List<ExaminationStatus> statuses) {
        throw new UnsupportedOperationException();

    }

    public List<QuestionnaireResponse> lookupQuestionnaireResponsesByExaminationStatus(ExaminationStatus status) {
        var criterion = new TokenClientParam("examination_status").exactly().code(status.toString().toLowerCase());
        return lookupByCriterion(QuestionnaireResponse.class, criterion);
    }

    public String saveCarePlan(CarePlan carePlan) {
        return save(carePlan);
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

    public List<Questionnaire> lookupQuestionnaires(Collection<String> questionnaireIds) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        Bundle bundle = (Bundle) client
                .search()
                .forResource(Questionnaire.class)
                .where(Questionnaire.RES_ID.exactly().systemAndValues(null, questionnaireIds))
                .prettyPrint()
                .execute();

        return bundle.getEntry().stream().map(e -> (Questionnaire) e.getResource()).collect(Collectors.toList());
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

    private <T extends Resource> List<T> lookupByCriterion(Class<T> resourceClass, ICriterion<?> criterion) {
        return lookupByCriteria(resourceClass, criterion, null);
    }

    private <T extends Resource> List<T> lookupByCriteria(Class<T> resourceClass, ICriterion<?> firstCriterion, ICriterion<?> secondCriterion) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        var query = client
                .search()
                .forResource(resourceClass)
                .where(firstCriterion);
        if(secondCriterion != null) {
            query = query.and(secondCriterion);
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

    private <T extends Resource> void update(Resource resource) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);
        client.update().resource(resource).execute();
    }
}
