package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.service.PlanDefinitionService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.openapitools.api.PlanDefinitionApi;
import org.openapitools.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
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


    @Override
    public ResponseEntity<Void> createPlanDefinition(CreatePlanDefinitionRequest createPlanDefinitionRequest) {
        try {
            PlanDefinitionModel planDefinition = dtoMapper.mapPlanDefinitionDto(createPlanDefinitionRequest.getPlanDefinition());
            QualifiedId.PlanDefinitionId planDefinitionId = planDefinitionService.createPlanDefinition(planDefinition);

            URI location = locationHeaderBuilder.buildLocationHeader(planDefinitionId);
            return ResponseEntity.created(location).build();

        } catch (AccessValidationException | ServiceException e) {
            logger.error("Error creating PlanDefinition", e);
            throw toStatusCodeException(e);
        }
    }

    @Override
    public ResponseEntity<List<PlanDefinitionDto>> getPlanDefinitions(List<org.openapitools.model.Status> statusesToInclude) {
        try {
            var statuses = statusesToInclude.stream().map(dtoMapper::mapStatus).toList();

            List<PlanDefinitionModel> planDefinitions = planDefinitionService.getPlanDefinitions(statuses);

            return ResponseEntity.ok(planDefinitions.stream().map(dtoMapper::mapPlanDefinitionModel)
                    .sorted(Comparator.comparing((x) -> x.getLastUpdated().orElse(null), Comparator.nullsFirst(OffsetDateTime::compareTo).reversed()))
                    .toList());
        } catch (ServiceException | AccessValidationException e) {
            logger.error("Could not look up planDefinitions", e);
            throw toStatusCodeException(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> isPlanDefinitionInUse(String id) {
        boolean isPlanDefinitionInUse;
        try {
            isPlanDefinitionInUse = !planDefinitionService.getCarePlansThatIncludes(new QualifiedId.PlanDefinitionId(id)).isEmpty();
        } catch (ServiceException | AccessValidationException se) {
            throw toStatusCodeException(se);
        }
        return ResponseEntity.ok().body(isPlanDefinitionInUse);

    }

    @Override
    public ResponseEntity<Void> patchPlanDefinition(String id, PatchPlanDefinitionRequest request) {
        try {
            String name = request.getName();
            List<QualifiedId.QuestionnaireId> questionnaireIds = getQuestionnaireIds(request.getQuestionnaireIds());
            List<ThresholdModel> thresholds = getThresholds(request.getThresholds());

            var status = dtoMapper.mapStatus(request.getStatus());

            planDefinitionService.updatePlanDefinition(new QualifiedId.PlanDefinitionId(id), name, status, questionnaireIds, thresholds);
        } catch (Exception e) {
            throw toStatusCodeException(e);
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> retirePlanDefinition(String id) {
        try {
            planDefinitionService.retirePlanDefinition(new QualifiedId.PlanDefinitionId(id));
        } catch (ServiceException | AccessValidationException se) {
            throw toStatusCodeException(se);
        }

        return ResponseEntity.ok().build();
    }


    private List<QualifiedId.QuestionnaireId> getQuestionnaireIds(List<String> questionnaireIds) {
        return questionnaireIds.stream().map(QualifiedId.QuestionnaireId::new).toList();
    }

    private List<ThresholdModel> getThresholds(List<ThresholdDto> thresholdDtos) {
        return collectionToStream(thresholdDtos).map(dtoMapper::mapThresholdDto).toList();
    }

    private <T> Stream<T> collectionToStream(Collection<T> collection) {
        return Optional.ofNullable(collection).stream().flatMap(Collection::stream);
    }


}
