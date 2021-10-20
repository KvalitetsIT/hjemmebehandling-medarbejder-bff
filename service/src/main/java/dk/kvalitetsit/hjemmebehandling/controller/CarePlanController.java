package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class CarePlanController {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanController.class);

    private CarePlanService carePlanService;
    private DtoMapper dtoMapper;
    private LocationHeaderBuilder locationHeaderBuilder;

    private static final String QUESTIONNAIRES_KEY = "questionnaires";

    public CarePlanController(CarePlanService carePlanService, DtoMapper dtoMapper, LocationHeaderBuilder locationHeaderBuilder) {
        this.carePlanService = carePlanService;
        this.dtoMapper = dtoMapper;
        this.locationHeaderBuilder = locationHeaderBuilder;
    }

    @GetMapping(value = "/v1/careplan")
    public List<CarePlanDto> getCarePlans(@RequestParam("cpr") Optional<String> cpr) {
        throw new UnsupportedOperationException();
    }

    @GetMapping(value = "/v1/careplan/{id}")
    public CarePlanDto getCarePlan(@PathVariable String id) {
        // Look up the CarePlan
        Optional<CarePlanModel> carePlan = carePlanService.getCarePlan(id);
        if(!carePlan.isPresent()) {
            throw new ResourceNotFoundException(String.format("CarePlan with id %s not found.", id));
        }
        return dtoMapper.mapCarePlanModel(carePlan.get());
    }

    @PostMapping(value = "/v1/careplan")
    public ResponseEntity<?> createCarePlan(@RequestBody CreateCarePlanRequest request) {
        String carepPlanId = null;
        try {
            carepPlanId = carePlanService.createCarePlan(request.getCpr(), request.getPlanDefinitionId());
        }
        catch(ServiceException e) {
            logger.error("Error creating CarePlan", e);
            throw new InternalServerErrorException();
        }

        URI location = locationHeaderBuilder.buildLocationHeader(carepPlanId);
        return ResponseEntity.created(location).build();
    }

    @PutMapping(value = "/v1/careplan")
    public void updateCarePlan(CarePlanDto carePlanDto) {
        throw new UnsupportedOperationException();
    }

    @PatchMapping(value = "/v1/careplan/{id}")
    public void patchCarePlan(@PathVariable String id, @RequestBody PartialUpdateCareplanRequest request) {
        if(request.getQuestionnaireIds() == null || request.getQuestionnaireFrequencies() == null) {
            throw new BadRequestException(String.format("Both questionnaireIds and questionnaireFrequencies must be supplied!"));
        }

        try {
            carePlanService.updateQuestionnaires(id, request.getQuestionnaireIds(), mapFrequencies(request.getQuestionnaireFrequencies()));
        }
        catch(ServiceException e) {
            // TODO: Distinguish when 'id' did not exist (bad request), and anything else (internal server error).
            throw new InternalServerErrorException();
        }

        // TODO: Return an appropriate status code.
    }

    private Map<String, FrequencyModel> mapFrequencies(Map<String, FrequencyDto> frequencyDtos) {
        Map<String, FrequencyModel> frequencies = new HashMap<>();

        for(String questionnaireId : frequencyDtos.keySet()) {
            frequencies.put(questionnaireId, dtoMapper.mapFrequency(frequencyDtos.get(questionnaireId)));
        }

        return frequencies;
    }
}
