package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.service.model.PatientModel;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Component
public class FhirMapper {
    public PatientModel mapPatient(Patient patient) {
        PatientModel patientModel = new PatientModel();

        patientModel.setCpr(extractCpr(patient));
        patientModel.setFamilyName(extractFamilyName(patient));
        patientModel.setGivenName(extractGivenNames(patient));

        return patientModel;
    }

    private String extractCpr(Patient patient) {
        return patient.getIdentifier().get(0).getValue();
    }

    private String extractFamilyName(Patient patient) {
        return patient.getName().get(0).getFamily();
    }

    private String extractGivenNames(Patient patient) {
        return patient.getName().get(0).getGivenAsSingleString();
    }
}
