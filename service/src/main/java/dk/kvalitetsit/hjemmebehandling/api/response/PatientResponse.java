package dk.kvalitetsit.hjemmebehandling.api.response;

import dk.kvalitetsit.hjemmebehandling.api.dto.PatientDto;

public class PatientResponse {
    private PatientDto patientDto;

    public PatientDto getPatientDto() {
        return patientDto;
    }

    public void setPatientDto(PatientDto patientDto) {
        this.patientDto = patientDto;
    }
}
