package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.CarePlanDto;
import dk.kvalitetsit.hjemmebehandling.api.CreateCarePlanRequest;
import dk.kvalitetsit.hjemmebehandling.api.FrequencyDto;
import dk.kvalitetsit.hjemmebehandling.api.PartialUpdateCareplanRequest;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.model.FrequencyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class CarePlanController {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanController.class);

    private CarePlanService carePlanService;

    private static final String QUESTIONNAIRES_KEY = "questionnaires";
    private static final Set<String> ALLOWED_UPDATE_FIELDS = Set.of(QUESTIONNAIRES_KEY);

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

    @PatchMapping(value = "/v1/careplan/{id}")
    public void partialUpdate(@PathVariable String id, @RequestBody PartialUpdateCareplanRequest request) {
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
            frequencies.put(questionnaireId, mapFrequency(frequencyDtos.get(questionnaireId)));
        }

        return frequencies;
    }

    private FrequencyModel mapFrequency(FrequencyDto frequencyDto) {
        FrequencyModel frequencyModel = new FrequencyModel();

        frequencyModel.setWeekday(frequencyDto.getWeekday());

        return frequencyModel;
    }
}
