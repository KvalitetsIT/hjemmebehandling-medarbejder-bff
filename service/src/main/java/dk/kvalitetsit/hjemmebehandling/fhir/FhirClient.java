package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public String saveCarePlan(CarePlan carePlan) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        MethodOutcome outcome = client.create().resource(carePlan).prettyPrint().execute();

        if(!outcome.getCreated()) {
            throw new IllegalStateException("Tried to create CarePlan, but it was not created!");
        }
        return outcome.getId().toUnqualifiedVersionless().getIdPart();
    }

    public Optional<CarePlan> lookupCarePlan(String carePlanId) {
        return lookupById(carePlanId, CarePlan.class);
    }

    public Optional<Patient> lookupPatientById(String patientId) {
        return lookupById(patientId, Patient.class);
    }

    public Optional<Patient> lookupPatientByCpr(String cpr) {
        return lookupByCriterion(Patient.class, Patient.IDENTIFIER.exactly().systemAndValues(Systems.CPR, cpr));
    }

    public void savePatient(Patient patient) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        MethodOutcome outcome = client.create().resource(patient).prettyPrint().execute();
    }

    public PlanDefinition lookupPlanDefinition(String planDefinitionId) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        // <identifier><system value="http://acme.org/mrns"/><value value="12345"/></identifier>
        Bundle bundle = (Bundle) client
                .search()
                .forResource(PlanDefinition.class)
                //.where( Patient.IDENTIFIER.exactly().systemAndValues(Systems.CPR, cpr))
                //.where(PlanDefinition.IDENTIFIER.exactly().)
                .prettyPrint()
                .execute();

        // Extract patient from the bundle
        if(bundle.getTotal() == 0) {
            return null;
        }
        if(bundle.getTotal() > 1) {
            logger.warn("More than one PlanDefinition present!");
        }

        Bundle.BundleEntryComponent component = bundle.getEntry().get(bundle.getEntry().size() - 1);
        return (PlanDefinition) component.getResource();
    }

    public List<Questionnaire> lookupQuestionnaires(List<String> questionnaireIds) {
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
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        client.update().resource(carePlan).prettyPrint().execute();
    }

    private <T extends Resource> Optional<T> lookupByCriterion(Class<T> resourceClass, ICriterion<?> criterion) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        Bundle bundle = (Bundle) client
                .search()
                .forResource(resourceClass)
                .where(criterion)
                .execute();

        // Extract resource from the bundle
        if(bundle.getTotal() == 0) {
            return Optional.empty();
        }
        if(bundle.getTotal() > 1) {
            logger.warn("More than one resource present!");
        }

        Bundle.BundleEntryComponent component = bundle.getEntry().get(bundle.getEntry().size() - 1);
        return Optional.of((T) component.getResource());
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
}
