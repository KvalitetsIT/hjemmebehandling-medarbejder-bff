package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.constants.PlanDefinitionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collection;
import java.util.Optional;

public class GetPlanDefinitionRequest {
    private Optional<Collection<String>> statusesToInclude = Optional.empty();

    @Schema(required = false, description = "The statuses to include in result")
    public Optional<Collection<String>> getStatusesToInclude() {
        return statusesToInclude;
    }

    public void setStatusesToInclude(Optional<Collection<String>> statusesToInclude) {
        this.statusesToInclude = statusesToInclude;
    }
}
