package dk.kvalitetsit.hjemmebehandling.api.response;

import dk.kvalitetsit.hjemmebehandling.api.dto.PatientDto;

import java.util.List;

public class PatientListResponse {
    private List<PatientDto> patients;

    public List<PatientDto> getPatients() {
        return patients;
    }

    public void setPatients(List<PatientDto> patients) {
        this.patients = patients;
    }
}
