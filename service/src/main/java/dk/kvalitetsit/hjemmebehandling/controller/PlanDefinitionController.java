package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.constants.PlanDefinitionStatus;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.service.PlanDefinitionService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.hl7.fhir.r4.model.ResourceType;
import org.openapitools.model.CreatePlanDefinitionRequest;
import org.openapitools.model.PlanDefinitionDto;
import org.openapitools.model.ThresholdDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Tag(name = "PlanDefinition", description = "API for manipulating and retrieving PlanDefinitions.")
public class PlanDefinitionController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionController.class);

    private final PlanDefinitionService planDefinitionService;
    private final DtoMapper dtoMapper;
    private final LocationHeaderBuilder locationHeaderBuilder;

    public PlanDefinitionController(PlanDefinitionService planDefinitionService, DtoMapper dtoMapper, LocationHeaderBuilder locationHeaderBuilder) {
        this.planDefinitionService = planDefinitionService;
        this.dtoMapper = dtoMapper;
        this.locationHeaderBuilder = locationHeaderBuilder;
    }

    @GetMapping(value = "/v1/plandefinition")
    public ResponseEntity<List<PlanDefinitionDto>> getPlanDefinitions(@RequestParam("statusesToInclude") Optional<Collection<String>> statusesToInclude) {
        try {
            if(statusesToInclude.isPresent() && statusesToInclude.get().isEmpty()){
                var details = ErrorDetails.PARAMETERS_INCOMPLETE;
                details.setDetails("Statusliste blev sendt med, men indeholder ingen elementer");
                throw new BadRequestException(details);
            }

            Collection<String> nonOptionalStatusesToInclude = statusesToInclude.orElseGet(List::of);
            List<PlanDefinitionModel> planDefinitions = planDefinitionService.getPlanDefinitions(nonOptionalStatusesToInclude);


            return ResponseEntity.ok(planDefinitions.stream()
                    .map(dtoMapper::mapPlanDefinitionModel)
                    .sorted(Comparator.comparing(PlanDefinitionDto::getLastUpdated, Comparator.nullsFirst(OffsetDateTime::compareTo).reversed()))
                    .collect(Collectors.toList()));
        }
        catch(ServiceException e) {
            logger.error("Could not look up plandefinitions", e);
            throw toStatusCodeException(e);
        }
    }


    @Operation(summary = "Create a new PlanDefinition.", description = "Create a PlanDefinition.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successful operation.", headers = { @Header(name = "Location", description = "URL pointing to the created PlanDefinition.")}, content = @Content),
        @ApiResponse(responseCode = "500", description = "Error during creation of PlanDefinition.", content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PostMapping(value = "/v1/plandefinition", consumes = { "application/json" })
    public ResponseEntity<Void> createPlanDefinition(@RequestBody CreatePlanDefinitionRequest request) {
        String planDefinitionId = null;
        try {
            PlanDefinitionModel planDefinition = dtoMapper.mapPlanDefinitionDto(request.getPlanDefinition());
            planDefinitionId = planDefinitionService.createPlanDefinition(planDefinition);
        }
        catch(AccessValidationException | ServiceException e) {
            logger.error("Error creating PlanDefinition", e);
            throw toStatusCodeException(e);
        }

        URI location = locationHeaderBuilder.buildLocationHeader(planDefinitionId);
        return ResponseEntity.created(location).build();
    }

    @PutMapping(value = "/v1/plandefinition")
    public void updatePlanDefinition(PlanDefinitionDto planDefinitionDto) {
        throw new UnsupportedOperationException();
    }

    @PatchMapping(value = "/v1/plandefinition/{id}")
    public ResponseEntity<Void> patchPlanDefinition(@PathVariable String id, @RequestBody PatchPlanDefinitionRequest request) {
        try {
            String name = request.getName();
            List<String> questionnaireIds = getQuestionnaireIds(request.getQuestionnaireIds());
            List<ThresholdModel> thresholds = getThresholds(request.getThresholds());
            PlanDefinitionStatus status = request.getStatus();
            planDefinitionService.updatePlanDefinition(id, name, status, questionnaireIds, thresholds);
        }
        catch(Exception e) {
            throw toStatusCodeException(e);
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/v1/plandefinition/{id}/retire")
    public ResponseEntity<Void> retirePlanDefinition(@PathVariable String id) {
        try {
            planDefinitionService.retirePlanDefinition(id);
        }
        catch(ServiceException se) {
            throw toStatusCodeException(se);
        }

        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Checks if the plandefinition is in use by any careplans",
        description = "Returns true if the plandefinition is in use by careplans and otherwise false if not"
    )
    @GetMapping(value = "/v1/plandefinition/{id}/used")
    public ResponseEntity<Boolean> isPlanDefinitionInUse(@PathVariable String id) {
        boolean isPlanDefinitionInUse;
        try {
            isPlanDefinitionInUse = !planDefinitionService.getCarePlansThatIncludes(id).isEmpty();
        }
        catch(ServiceException se) {
            throw toStatusCodeException(se);
        }

        return ResponseEntity.ok().body(isPlanDefinitionInUse);

    }

    private List<String> getQuestionnaireIds(List<String> questionnaireIds) {
        return collectionToStream(questionnaireIds)
            .map(id -> FhirUtils.qualifyId(id, ResourceType.Questionnaire))
            .collect(Collectors.toList());
    }

    private List<ThresholdModel> getThresholds(List<ThresholdDto> thresholdDtos) {
        return collectionToStream(thresholdDtos)
            .map(dtoMapper::mapThresholdDto)
            .collect(Collectors.toList());
    }

    private <T> Stream<T> collectionToStream(Collection<T> collection) {
        return Optional.ofNullable(collection).stream().flatMap(Collection::stream);
    }
}
