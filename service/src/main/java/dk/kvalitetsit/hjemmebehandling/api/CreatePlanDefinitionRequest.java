package dk.kvalitetsit.hjemmebehandling.api;

import io.swagger.v3.oas.annotations.media.Schema;

public class CreatePlanDefinitionRequest {
    private PlanDefinitionDto planDefinition;

    @Schema(required = true, description = "The plandefinition to create.")
    public PlanDefinitionDto getPlanDefinition() {
        return planDefinition;
    }

    public void setPlanDefinition(PlanDefinitionDto planDefinition) {
        this.planDefinition = planDefinition;
    }
}
