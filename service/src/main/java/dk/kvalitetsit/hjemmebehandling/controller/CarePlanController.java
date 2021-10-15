package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.CarePlanDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CarePlanController {
    @GetMapping(value = "/v1/careplan")
    public CarePlanDto getCarePlan(String id) {
        throw new UnsupportedOperationException();
    }

    @PostMapping(value = "/v1/careplan")
    public void createCarePlan(String cpr, String planDefinitionId) {
        // Return 201 and include the resource UTI in the Location header.
        throw new UnsupportedOperationException();
    }

    @PutMapping(value = "/v1/careplan")
    public void updateCarePlan(CarePlanDto carePlanDto) {
        throw new UnsupportedOperationException();
    }
}
