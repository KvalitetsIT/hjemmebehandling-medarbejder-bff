package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class CarePlanController {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanController.class);

    private CarePlanService carePlanService;
    private DtoMapper mapper;

    private static final String QUESTIONNAIRES_KEY = "questionnaires";

    public CarePlanController(CarePlanService carePlanService, DtoMapper mapper) {
        this.carePlanService = carePlanService;
        this.mapper = mapper;
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
            frequencies.put(questionnaireId, mapper.mapFrequency(frequencyDtos.get(questionnaireId)));
        }

        return frequencies;
    }
}
