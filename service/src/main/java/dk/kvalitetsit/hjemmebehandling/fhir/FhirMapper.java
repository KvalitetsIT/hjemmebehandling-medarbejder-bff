package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.service.model.ContactDetailsModel;
import dk.kvalitetsit.hjemmebehandling.service.model.PatientModel;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Component
public class FhirMapper {
    public Patient mapPatientModel(PatientModel patientModel) {
        Patient patient = new Patient();

        patient.getIdentifier().add(makeCprIdentifier(patientModel.getCpr()));

        return patient;
    }

    public PatientModel mapPatient(Patient patient) {
        PatientModel patientModel = new PatientModel();

        patientModel.setCpr(extractCpr(patient));
        patientModel.setFamilyName(extractFamilyName(patient));
        patientModel.setGivenName(extractGivenNames(patient));
        patientModel.setPatientContactDetails(extractPatientContactDetails(patient));

        return patientModel;
    }

    private String extractCpr(Patient patient) {
        return patient.getIdentifier().get(0).getValue();
    }

    private Identifier makeCprIdentifier(String cpr) {
        Identifier identifier = new Identifier();

        identifier.setSystem(Systems.CPR);
        identifier.setValue(cpr);

        return identifier;
    }

    private String extractFamilyName(Patient patient) {
        if(patient.getName() == null || patient.getName().isEmpty()) {
            return null;
        }
        return patient.getName().get(0).getFamily();
    }

    private String extractGivenNames(Patient patient) {
        if(patient.getName() == null || patient.getName().isEmpty()) {
            return null;
        }
        return patient.getName().get(0).getGivenAsSingleString();
    }

    private ContactDetailsModel extractPatientContactDetails(Patient patient) {
        ContactDetailsModel contactDetails = new ContactDetailsModel();

        contactDetails.setPrimaryPhone(extractPrimaryPhone(patient));
        contactDetails.setEmailAddress(extractEmailAddress(patient));

        return contactDetails;
    }

    private String extractPrimaryPhone(Patient patient) {
        if(patient.getTelecom() == null || patient.getTelecom().isEmpty()) {
            return null;
        }
        for(ContactPoint cp : patient.getTelecom()) {
            if(!cp.getValue().contains("@")) {
                return cp.getValue();
            }
        }
        return null;
    }

    private String extractEmailAddress(Patient patient) {
        if(patient.getTelecom() == null || patient.getTelecom().isEmpty()) {
            return null;
        }
        for(ContactPoint cp : patient.getTelecom()) {
            if(cp.getValue().contains("@")) {
                return cp.getValue();
            }
        }
        return null;
    }
}
