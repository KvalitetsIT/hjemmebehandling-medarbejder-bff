package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.service.PlanDefinitionService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name = "PlanDefinition", description = "API for manipulating and retrieving PlanDefinitions.")
public class PlanDefinitionController {
    private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionController.class);

    private PlanDefinitionService planDefinitionService;
    private DtoMapper dtoMapper;

    public PlanDefinitionController(PlanDefinitionService planDefinitionService, DtoMapper dtoMapper) {
        this.planDefinitionService = planDefinitionService;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping(value = "/v1/plandefinition")
    public ResponseEntity<List<PlanDefinitionDto>> getPlanDefinitions() {
        try {
            List<PlanDefinitionModel> planDefinitions = planDefinitionService.getPlanDefinitions();
            if(planDefinitions.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(planDefinitions.stream().map(pd -> dtoMapper.mapPlanDefinitionModel(pd)).collect(Collectors.toList()));
        }
        catch(ServiceException e) {
            logger.error("Could not look up plandefinitions", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
