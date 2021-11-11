package dk.kvalitetsit.hjemmebehandling.api;

import io.swagger.v3.oas.annotations.media.Schema;

public class CreateCarePlanRequest {
    private CarePlanDto carePlan;

    @Schema(required = true, description = "The careplan to create. If the referenced patient does not already exist, (s)he  is also creted.")
    public CarePlanDto getCarePlan() {
        return carePlan;
    }

    public void setCarePlan(CarePlanDto carePlan) {
        this.carePlan = carePlan;
    }
}
