package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.constants.PlanDefinitionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class GetPlanDefinitionRequest {
	private Collection<String> statusesToInclude = List.of();

	@Schema(required = true, description = "The statuses to include in result")
	public Collection<String> getStatusesToInclude() {
		return statusesToInclude;
	}

	public void setStatusesToInclude(Collection<String> statusesToInclude) {
		this.statusesToInclude = statusesToInclude;
	}
}
