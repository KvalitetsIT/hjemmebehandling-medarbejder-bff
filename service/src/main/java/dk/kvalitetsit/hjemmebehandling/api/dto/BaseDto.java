package dk.kvalitetsit.hjemmebehandling.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class BaseDto {
    private String id;

    @Schema(required = true, description = "Id of the resource", example = "CarePlan/10")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


}
