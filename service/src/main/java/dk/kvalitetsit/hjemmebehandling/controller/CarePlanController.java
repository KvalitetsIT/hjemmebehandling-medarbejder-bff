package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.function.ServerRequest;

import java.net.URI;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Tag(name = "CarePlan", description = "API for manipulating and retrieving CarePlans.")
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
    public ResponseEntity<List<CarePlanDto>> getCarePlansByCpr(@RequestParam("cpr") Optional<String> cpr) {
        if(!cpr.isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<CarePlanModel> carePlans = carePlanService.getCarePlansByCpr(cpr.get());
            if(carePlans.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(carePlans.stream().map(cp -> dtoMapper.mapCarePlanModel(cp)).collect(Collectors.toList()));
        }
        catch(ServiceException e) {
            logger.error("Could not look up careplans by cpr", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get CarePlan by id.", description = "Retrieves a CarePlan by its id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = @Content(schema = @Schema(implementation = CarePlanDto.class))),
            @ApiResponse(responseCode = "404", description = "CarePlan not found.", content = @Content)
    })
    @GetMapping(value = "/v1/careplan/{id}", produces = { "application/json" })
    public ResponseEntity<CarePlanDto> getCarePlanById(@PathVariable @Parameter(description = "Id of the CarePlan to be retrieved.") String id) {
        // Look up the CarePlan
        Optional<CarePlanModel> carePlan = Optional.empty();

        try {
            carePlan = carePlanService.getCarePlanById(id);
        }
        catch (AccessValidationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        catch(ServiceException e) {
            // TODO: Distinguish when 'id' did not exist (bad request), and anything else (internal server error).
            logger.error("Could not update questionnaire response", e);
            return ResponseEntity.internalServerError().build();
        }

        if(!carePlan.isPresent()) {
            return ResponseEntity.notFound().header("Reason", String.format("CarePlan with id %s not found.", id)).build();
        }
        return ResponseEntity.ok(dtoMapper.mapCarePlanModel(carePlan.get()));
    }

    @Operation(summary = "Create a new CarePlan for a patient.", description = "Create a CarePlan for a patient, based on a PlanDefinition.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successful operation.", headers = { @Header(name = "Location", description = "URL pointing to the created CarePlan.")}, content = @Content),
            @ApiResponse(responseCode = "500", description = "Error during creation of CarePlan.", content = @Content)
    })
    @PostMapping(value = "/v1/careplan", consumes = { "application/json" })
    public ResponseEntity<Void> createCarePlan(@RequestBody CreateCarePlanRequest request) {
        String carePlanId = null;
        try {
            carePlanId = carePlanService.createCarePlan(dtoMapper.mapCarePlanDto(request.getCarePlan()));
        }
        catch(AccessValidationException e) {
            logger.info("Detected access violation.", e);
            return ResponseEntity.badRequest().build();
        }
        catch(ServiceException e) {
            logger.error("Error creating CarePlan", e);
            return ResponseEntity.internalServerError().build();
        }

        URI location = locationHeaderBuilder.buildLocationHeader(carePlanId);
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
            frequencies.put(questionnaireId, dtoMapper.mapFrequencyDto(frequencyDtos.get(questionnaireId)));
        }

        return frequencies;
    }
}
