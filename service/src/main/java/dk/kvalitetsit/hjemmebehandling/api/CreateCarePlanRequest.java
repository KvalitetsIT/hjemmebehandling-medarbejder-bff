package dk.kvalitetsit.hjemmebehandling.api;

import io.swagger.v3.oas.annotations.media.Schema;

public class CreateCarePlanRequest {
    private String cpr;
    private String planDefinitionId;

    @Schema(required = true, description = "Cpr-number of the patient that the careplan applies to.", example = "0101010101")
    public String getCpr() {
        return cpr;
    }

    public void setCpr(String cpr) {
        this.cpr = cpr;
    }

    @Schema(required = true, description = "Id of the plandefinition that the careplan should instantiate.", example = "2")
    public String getPlanDefinitionId() {
        return planDefinitionId;
    }

    public void setPlanDefinitionId(String planDefinitionId) {
        this.planDefinitionId = planDefinitionId;
    }
}
