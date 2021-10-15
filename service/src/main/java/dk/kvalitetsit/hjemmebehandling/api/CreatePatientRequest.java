package dk.kvalitetsit.hjemmebehandling.api;

public class CreatePatientRequest {
    private PatientDto patient;
    private String planDefinitionId;

    public PatientDto getPatient() {
        return patient;
    }

    public void setPatient(PatientDto patient) {
        this.patient = patient;
    }

    public String getPlanDefinitionId() {
        return planDefinitionId;
    }

    public void setPlanDefinitionId(String planDefinitionId) {
        this.planDefinitionId = planDefinitionId;
    }
}
