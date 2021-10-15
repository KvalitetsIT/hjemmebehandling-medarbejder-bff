package dk.kvalitetsit.hjemmebehandling.api;

public class CreateCarePlanRequest {
    private String cpr;
    private String planDefinitionId;

    public String getCpr() {
        return cpr;
    }

    public void setCpr(String cpr) {
        this.cpr = cpr;
    }

    public String getPlanDefinitionId() {
        return planDefinitionId;
    }

    public void setPlanDefinitionId(String planDefinitionId) {
        this.planDefinitionId = planDefinitionId;
    }
}
