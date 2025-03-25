package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionStatus;
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
import org.openapitools.api.PlanDefinitionApi;
import org.openapitools.model.CreatePlanDefinitionRequest;
import org.openapitools.model.PatchPlanDefinitionRequest;
import org.openapitools.model.PlanDefinitionDto;
import org.openapitools.model.ThresholdDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Tag(name = "PlanDefinition", description = "API for manipulating and retrieving PlanDefinitions.")
public class PlanDefinitionController extends BaseController implements PlanDefinitionApi {
    private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionController.class);

    private final PlanDefinitionService planDefinitionService;
    private final DtoMapper dtoMapper;
    private final LocationHeaderBuilder locationHeaderBuilder;

    public PlanDefinitionController(PlanDefinitionService planDefinitionService, DtoMapper dtoMapper, LocationHeaderBuilder locationHeaderBuilder) {
        this.planDefinitionService = planDefinitionService;
        this.dtoMapper = dtoMapper;
        this.locationHeaderBuilder = locationHeaderBuilder;
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

    @Override
    public ResponseEntity<Void> createPlanDefinition(CreatePlanDefinitionRequest createPlanDefinitionRequest) {
        String planDefinitionId = null;
        try {
            PlanDefinitionModel planDefinition = dtoMapper.mapPlanDefinitionDto(createPlanDefinitionRequest.getPlanDefinition());
            planDefinitionId = planDefinitionService.createPlanDefinition(planDefinition);
        }
        catch(AccessValidationException | ServiceException e) {
            logger.error("Error creating PlanDefinition", e);
            throw toStatusCodeException(e);
        }

        URI location = locationHeaderBuilder.buildLocationHeader(planDefinitionId);
        return ResponseEntity.created(location).build();
    }

    @Override
    public ResponseEntity<List<PlanDefinitionDto>> getPlanDefinitions(List<String> statusesToInclude) {
        try {

            if( statusesToInclude != null &&  statusesToInclude.isEmpty()){
                var details = ErrorDetails.PARAMETERS_INCOMPLETE;
                details.setDetails("Statusliste blev sendt med, men indeholder ingen elementer");
                throw new BadRequestException(details);
            }

            List<PlanDefinitionModel> planDefinitions = planDefinitionService.getPlanDefinitions(statusesToInclude);

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

    @Override
    public ResponseEntity<Boolean> isPlanDefinitionInUse(String id) {
        boolean isPlanDefinitionInUse;
        try {
            isPlanDefinitionInUse = !planDefinitionService.getCarePlansThatIncludes(id).isEmpty();
        }
        catch(ServiceException se) {
            throw toStatusCodeException(se);
        }
        return ResponseEntity.ok().body(isPlanDefinitionInUse);

    }

    @Override
    public ResponseEntity<Void> patchPlanDefinition(String id, PatchPlanDefinitionRequest request) {
        try {
            String name = request.getName();
            List<String> questionnaireIds = getQuestionnaireIds(request.getQuestionnaireIds());
            List<ThresholdModel> thresholds = getThresholds(request.getThresholds());
            PatchPlanDefinitionRequest.StatusEnum status = request.getStatus();

            planDefinitionService.updatePlanDefinition(id, name, dtoMapper.mapPlanDefinitionStatusDto(status), questionnaireIds, thresholds);
        }
        catch(Exception e) {
            throw toStatusCodeException(e);
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> retirePlanDefinition(String id) {
        try {
            planDefinitionService.retirePlanDefinition(id);
        }
        catch(ServiceException se) {
            throw toStatusCodeException(se);
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updatePlanDefinition(PlanDefinitionDto planDefinitionDto) {
        throw new UnsupportedOperationException();
    }









}
