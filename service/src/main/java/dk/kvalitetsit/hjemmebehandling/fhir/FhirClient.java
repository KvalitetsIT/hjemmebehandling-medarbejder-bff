package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.controller.PatientController;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FhirClient {
    private static final Logger logger = LoggerFactory.getLogger(FhirClient.class);

    private FhirContext context;
    private String endpoint;

    public FhirClient(FhirContext context, String endpoint) {
        this.context = context;
        this.endpoint = endpoint;
    }

    public Patient lookupPatient(String cpr) {
        // "http://hapi-server:8080/fhir"
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        // <identifier><system value="http://acme.org/mrns"/><value value="12345"/></identifier>
        Bundle bundle = (Bundle) client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndValues(Systems.CPR, cpr))
                .prettyPrint()
                .execute();

        // Extract patient from the bundle
        if(bundle.getTotal() == 0) {
            return null;
        }
        if(bundle.getTotal() > 1) {
            logger.warn("More than one patient present!");
        }

        Bundle.BundleEntryComponent component = bundle.getEntry().get(bundle.getEntry().size() - 1);
        return (Patient) component.getResource();
    }

    public void savePatient(Patient patient) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        client.create().resource(patient).prettyPrint().execute();
    }
}
