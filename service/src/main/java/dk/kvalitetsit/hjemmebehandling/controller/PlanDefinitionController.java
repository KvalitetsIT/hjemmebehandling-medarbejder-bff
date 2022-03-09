package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.api.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientDetails;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
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

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Tag(name = "PlanDefinition", description = "API for manipulating and retrieving PlanDefinitions.")
public class PlanDefinitionController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionController.class);

    private PlanDefinitionService planDefinitionService;
    private DtoMapper dtoMapper;
    private LocationHeaderBuilder locationHeaderBuilder;

    public PlanDefinitionController(PlanDefinitionService planDefinitionService, DtoMapper dtoMapper, LocationHeaderBuilder locationHeaderBuilder) {
        this.planDefinitionService = planDefinitionService;
        this.dtoMapper = dtoMapper;
        this.locationHeaderBuilder = locationHeaderBuilder;
    }

    @GetMapping(value = "/v1/plandefinition")
    public ResponseEntity<List<PlanDefinitionDto>> getPlanDefinitions() {
        try {
            List<PlanDefinitionModel> planDefinitions = planDefinitionService.getPlanDefinitions();

            return ResponseEntity.ok(planDefinitions.stream().map(pd -> dtoMapper.mapPlanDefinitionModel(pd)).collect(Collectors.toList()));
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

            planDefinitionService.updatePlanDefinition(id, name, questionnaireIds, thresholds);
        }
        catch(Exception e) {
            throw toStatusCodeException(e);
        }

        return ResponseEntity.ok().build();
    }

    private List<String> getQuestionnaireIds(List<String> questionnaireIds) {
        return collectionToStream(questionnaireIds)
            .map(id -> FhirUtils.qualifyId(id, ResourceType.Questionnaire))
            .collect(Collectors.toList());
    }

    private List<ThresholdModel> getThresholds(List<ThresholdDto> thresholdDtos) {
        return collectionToStream(thresholdDtos)
            .map(t -> dtoMapper.mapThresholdDto(t))
            .collect(Collectors.toList());
    }

    private <T> Stream<T> collectionToStream(Collection<T> collection) {
        return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
    }
}
