package dk.kvalitetsit.hello.api;

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
