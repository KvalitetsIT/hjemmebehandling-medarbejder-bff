package dk.kvalitetsit.hjemmebehandling.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.model.PatientModel;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PatientService {
    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    public PatientService(FhirClient fhirClient, FhirMapper fhirMapper) {
        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
    }

    public void createPatient(PatientModel patientModel) throws ServiceException {
        try {
            fhirClient.savePatient(fhirMapper.mapPatientModel(patientModel));
        }
        catch(Exception e) {
            throw new ServiceException("Error saving patient", e);
        }
    }

    public List<PatientModel> getPatients(String clinicalIdentifier) {
        FhirContext context = FhirContext.forR4();
        IGenericClient client = context.newRestfulGenericClient("http://hapi-server:8080/fhir");

        Bundle bundle = (Bundle) client.search().forResource("Patient").prettyPrint().execute();

//        org.hl7.fhir.r4.model.Patient patient = new org.hl7.fhir.r4.model.Patient();

        PatientModel p = new PatientModel();

        p.setCpr("0101010101");
        p.setFamilyName("Ærtegærde Ømø Ååstrup");
        p.setGivenName("Torgot");

        return List.of(p);
    }

    public PatientModel getPatient(String cpr) {
        // Look up the patient
        Patient patient = fhirClient.lookupPatient(cpr);
        if(patient == null) {
            return null;
        }

        // Map to the domain model
        return fhirMapper.mapPatient(patient);
    }
}
