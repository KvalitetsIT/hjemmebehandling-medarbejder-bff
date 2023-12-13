package dk.kvalitetsit.hjemmebehandling.api.request;

import dk.kvalitetsit.hjemmebehandling.api.dto.PatientDto;

public class CreatePatientRequest {
    private PatientDto patient;

    public PatientDto getPatient() {
        return patient;
    }

    public void setPatient(PatientDto patient) {
        this.patient = patient;
    }
}
