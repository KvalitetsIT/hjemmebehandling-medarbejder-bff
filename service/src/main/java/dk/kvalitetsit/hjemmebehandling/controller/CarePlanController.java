package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ForbiddenException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
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
import org.webjars.NotFoundException;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Tag(name = "CarePlan", description = "API for manipulating and retrieving CarePlans.")
public class CarePlanController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanController.class);

    private CarePlanService carePlanService;
    private DtoMapper dtoMapper;
    private LocationHeaderBuilder locationHeaderBuilder;

    private enum SearchType {
        CPR, UNSATISFIED_CAREPLANS
    }

    public CarePlanController(CarePlanService carePlanService, DtoMapper dtoMapper, LocationHeaderBuilder locationHeaderBuilder) {
        this.carePlanService = carePlanService;
        this.dtoMapper = dtoMapper;
        this.locationHeaderBuilder = locationHeaderBuilder;
    }

    @GetMapping(value = "/v1/careplan")
    public ResponseEntity<List<CarePlanDto>> searchCarePlans(@RequestParam("cpr") Optional<String> cpr, @RequestParam("only_unsatisfied_schedules") Optional<Boolean> onlyUnsatisfiedSchedules, @RequestParam("only_active_careplans") Optional<Boolean> onlyActiveCarePlans, @RequestParam("page_number") Optional<Integer> pageNumber, @RequestParam("page_size") Optional<Integer> pageSize) {
        var searchType = determineSearchType(cpr, onlyUnsatisfiedSchedules, pageNumber, pageSize);
        if(!searchType.isPresent()) {
            logger.info("Detected unsupported parameter combination for SearchCarePlan, rejecting request.");
            throw new BadRequestException(ErrorDetails.UNSUPPORTED_SEARCH_PARAMETER_COMBINATION);
        }

        try {
            List<CarePlanModel> carePlans = null;

            if(cpr.isPresent()) {
                carePlans = carePlanService.getCarePlansByCpr(cpr.get(), onlyActiveCarePlans.orElse(false));
            }
            else if(onlyUnsatisfiedSchedules.isPresent() && onlyUnsatisfiedSchedules.get()) {
                carePlans = carePlanService.getCarePlansWithUnsatisfiedSchedules(onlyActiveCarePlans.orElse(false), new PageDetails(pageNumber.get(), pageSize.get()));
            }

            return ResponseEntity.ok(carePlans.stream().map(cp -> dtoMapper.mapCarePlanModel(cp)).collect(Collectors.toList()));
        }
        catch(ServiceException e) {
            logger.error("Could not look up careplans by cpr", e);
            throw toStatusCodeException(e);
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
        catch(AccessValidationException | ServiceException e) {
            logger.error("Could not update questionnaire response", e);
            throw toStatusCodeException(e);
        }

        if(!carePlan.isPresent()) {
            throw new ResourceNotFoundException(String.format("CarePlan with id %s not found.", id), ErrorDetails.CAREPLAN_DOES_NOT_EXIST);
        }
        return ResponseEntity.ok(dtoMapper.mapCarePlanModel(carePlan.get()));
    }

    @Operation(summary = "Create a new CarePlan for a patient.", description = "Create a CarePlan for a patient, based on a PlanDefinition.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successful operation.", headers = { @Header(name = "Location", description = "URL pointing to the created CarePlan.")}, content = @Content),
            @ApiResponse(responseCode = "500", description = "Error during creation of CarePlan.", content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PostMapping(value = "/v1/careplan", consumes = { "application/json" })
    public ResponseEntity<Void> createCarePlan(@RequestBody CreateCarePlanRequest request) {
        String carePlanId = null;
        try {
            carePlanId = carePlanService.createCarePlan(dtoMapper.mapCarePlanDto(request.getCarePlan()));
        }
        catch(AccessValidationException | ServiceException e) {
            logger.error("Error creating CarePlan", e);
            throw toStatusCodeException(e);
        }

        URI location = locationHeaderBuilder.buildLocationHeader(carePlanId);
        return ResponseEntity.created(location).build();
    }

    @PutMapping(value = "/v1/careplan")
    public void updateCarePlan(CarePlanDto carePlanDto) {
        throw new UnsupportedOperationException();
    }

    @PatchMapping(value = "/v1/careplan/{id}")
    public ResponseEntity<Void> patchCarePlan(@PathVariable String id, @RequestBody PartialUpdateCareplanRequest request) {
        if(request.getQuestionnaireIds() == null || request.getQuestionnaireFrequencies() == null) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }

        try {
            carePlanService.updateQuestionnaires(id, request.getQuestionnaireIds(), mapFrequencies(request.getQuestionnaireFrequencies()));
        }
        catch(AccessValidationException | ServiceException e) {
            throw toStatusCodeException(e);
        }

        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/v1/careplan/{id}/resolve-alarm")
    public ResponseEntity<Void> resolveAlarm(@PathVariable String id) {
        try {
            carePlanService.resolveAlarm(id);
        }
        catch(AccessValidationException | ServiceException e) {
            throw toStatusCodeException(e);
        }

        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/v1/careplan/{id}/complete")
    public ResponseEntity<Void> completeCarePlan(@PathVariable String id) {
        try {
            carePlanService.completeCarePlan(id);
        }
        catch(ServiceException e) {
            throw toStatusCodeException(e);
        }

        return ResponseEntity.ok().build();
    }

    private Map<String, FrequencyModel> mapFrequencies(Map<String, FrequencyDto> frequencyDtos) {
        Map<String, FrequencyModel> frequencies = new HashMap<>();

        for(String questionnaireId : frequencyDtos.keySet()) {
            frequencies.put(questionnaireId, dtoMapper.mapFrequencyDto(frequencyDtos.get(questionnaireId)));
        }

        return frequencies;
    }

    private Optional<SearchType> determineSearchType(Optional<String> cpr, Optional<Boolean> onlyUnsatisfiedSchedules, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        boolean sameParameterPresence = cpr.isPresent() == onlyUnsatisfiedSchedules.isPresent();
        if(sameParameterPresence) {
            return Optional.empty();
        }

        boolean pagingParametersPresent = pageNumber.isPresent() && pageSize.isPresent();
        if(cpr.isPresent() && !onlyUnsatisfiedSchedules.isPresent() && !pagingParametersPresent) {
            return Optional.of(SearchType.CPR);
        }
        if(!cpr.isPresent() && onlyUnsatisfiedSchedules.isPresent() && onlyUnsatisfiedSchedules.get() && pagingParametersPresent) {
            return Optional.of(SearchType.UNSATISFIED_CAREPLANS);
        }
        return Optional.empty();
    }
}
