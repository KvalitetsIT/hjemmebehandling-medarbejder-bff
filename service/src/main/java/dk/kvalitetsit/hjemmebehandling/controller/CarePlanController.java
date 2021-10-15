package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.CarePlanDto;
import dk.kvalitetsit.hjemmebehandling.api.CreateCarePlanRequest;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
public class CarePlanController {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanController.class);

    private CarePlanService carePlanService;

    public CarePlanController(CarePlanService carePlanService) {
        this.carePlanService = carePlanService;
    }

    @GetMapping(value = "/v1/careplan")
    public CarePlanDto getCarePlan(String id) {
        throw new UnsupportedOperationException();
    }

    @PostMapping(value = "/v1/careplan")
    public void createCarePlan(@RequestBody CreateCarePlanRequest request) {
        String carepPlanId = null;
        try {
            carepPlanId = carePlanService.createCarePlan(request.getCpr(), request.getPlanDefinitionId());
        }
        catch(ServiceException e) {
            logger.error("Error creating CarePlan", e);
            throw new InternalServerErrorException();
        }

        // TODO: Return 201 and include the resource URI in the Location header.
    }

    @PutMapping(value = "/v1/careplan")
    public void updateCarePlan(CarePlanDto carePlanDto) {
        throw new UnsupportedOperationException();
    }
}
