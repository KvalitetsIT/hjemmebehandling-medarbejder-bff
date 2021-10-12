package dk.kvalitetsit.hjemmebehandling.api;

public class PatientResponse {
    private PatientDto patientDto;

    public PatientDto getPatientDto() {
        return patientDto;
    }

    public void setPatientDto(PatientDto patientDto) {
        this.patientDto = patientDto;
    }
}
