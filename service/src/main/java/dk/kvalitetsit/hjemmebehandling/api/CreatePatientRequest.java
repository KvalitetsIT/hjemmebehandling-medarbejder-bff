package dk.kvalitetsit.hjemmebehandling.api;

public class CreatePatientRequest {
    private PatientDto patient;

    public PatientDto getPatient() {
        return patient;
    }

    public void setPatient(PatientDto patient) {
        this.patient = patient;
    }
}
