package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.CarePlanDto;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.QuestionnaireDto;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireService;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Questionnaire", description = "API for manipulating and retrieving Questionnaires.")
public class QuestionnaireController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireController.class);

    private QuestionnaireService questionnaireService;
    private AuditLoggingService auditLoggingService;
    private DtoMapper dtoMapper;

    public QuestionnaireController(QuestionnaireService questionnaireService, AuditLoggingService auditLoggingService, DtoMapper dtoMapper) {
        this.questionnaireService = questionnaireService;
        this.auditLoggingService = auditLoggingService;
        this.dtoMapper = dtoMapper;
    }

    @Operation(summary = "Get all Questionnaires.", description = "Retrieves a list of Questionnaire.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful operation.", content = @Content(schema = @Schema(implementation = QuestionnaireDto.class)))
    })
    @GetMapping(value = "/v1/questionnaire", produces = { "application/json" })
    public ResponseEntity<List<QuestionnaireDto>> getQuestionnaires() {
        List<QuestionnaireModel> questionnaires = questionnaireService.getQuestionnaires();

        return ResponseEntity.ok(questionnaires.stream().map(q -> dtoMapper.mapQuestionnaireModel(q)).collect(Collectors.toList()));
    }

    @Operation(summary = "Get Questionnaire by id.", description = "Retrieves a Questionnaire by its id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = @Content(schema = @Schema(implementation = QuestionnaireDto.class))),
            @ApiResponse(responseCode = "404", description = "CarePlan not found.", content = @Content)
    })
    @GetMapping(value = "/v1/questionnaire/{id}", produces = { "application/json" })
    public ResponseEntity<QuestionnaireDto> getQuestionnaireById(@PathVariable @Parameter(description = "Id of the questionnaire to be retrieved.") String id) {
        // Look up the Questionnaire
        Optional<QuestionnaireModel> questionnaire = Optional.empty();

        try {
            questionnaire = questionnaireService.getQuestionnaireById(id);
        }
        catch(AccessValidationException | ServiceException e) {
            logger.error("Could not update questionnaire response", e);
            throw toStatusCodeException(e);
        }

        if(!questionnaire.isPresent()) {
            throw new ResourceNotFoundException(String.format("Questionnaire with id %s not found.", id), ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }
        return ResponseEntity.ok(dtoMapper.mapQuestionnaireModel(questionnaire.get()));
    }
}
