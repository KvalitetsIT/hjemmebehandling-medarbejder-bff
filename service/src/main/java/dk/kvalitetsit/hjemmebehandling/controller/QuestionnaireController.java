package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.CreateQuestionnaireRequest;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.ErrorDto;
import dk.kvalitetsit.hjemmebehandling.api.QuestionnaireDto;
import dk.kvalitetsit.hjemmebehandling.api.PatchQuestionnaireRequest;
import dk.kvalitetsit.hjemmebehandling.api.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Tag(name = "Questionnaire", description = "API for manipulating and retrieving Questionnaires.")
public class QuestionnaireController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireController.class);

    private QuestionnaireService questionnaireService;
    private AuditLoggingService auditLoggingService;
    private DtoMapper dtoMapper;
    private LocationHeaderBuilder locationHeaderBuilder;

    public QuestionnaireController(QuestionnaireService questionnaireService, AuditLoggingService auditLoggingService, DtoMapper dtoMapper, LocationHeaderBuilder locationHeaderBuilder) {
        this.questionnaireService = questionnaireService;
        this.auditLoggingService = auditLoggingService;
        this.dtoMapper = dtoMapper;
        this.locationHeaderBuilder = locationHeaderBuilder;
    }

    @Operation(summary = "Get all Questionnaires.", description = "Retrieves a list of Questionnaire.")
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
            String questionnaireId = FhirUtils.qualifyId(id, ResourceType.Questionnaire);
            questionnaire = questionnaireService.getQuestionnaireById(questionnaireId);
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

    @Operation(summary = "Create a new Questionnaire.", description = "Create a Questionnaire.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successful operation.", headers = { @Header(name = "Location", description = "URL pointing to the created Questionnaire.")}, content = @Content),
        @ApiResponse(responseCode = "500", description = "Error during creation of Questionnaire.", content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PostMapping(value = "/v1/questionnaire", consumes = { "application/json" })
    public ResponseEntity<Void> createQuestionnaire(@RequestBody CreateQuestionnaireRequest request) {
        validateQuestions(request.getQuestionnaire().getQuestions());

        QuestionnaireModel questionnaire = dtoMapper.mapQuestionnaireDto(request.getQuestionnaire());
        
        List<QuestionModel> callToActions = collectionToStream(request.getQuestionnaire().getCallToActions())
                .map(c -> dtoMapper.mapQuestionDto(c))
                .collect(Collectors.toList());
        questionnaire.setCallToActions(callToActions);
        
        String questionnaireId = questionnaireService.createQuestionnaire(questionnaire);

        URI location = locationHeaderBuilder.buildLocationHeader(questionnaireId);
        return ResponseEntity.created(location).build();
    }

    @PutMapping(value = "/v1/questionnaire")
    public void updateQuestionnaire(QuestionnaireDto questionnaireDto) {
        throw new UnsupportedOperationException();
    }

    @PatchMapping(value = "/v1/questionnaire/{id}")
    public ResponseEntity<Void> patchQuestionnaire(@PathVariable String id, @RequestBody PatchQuestionnaireRequest request) {

        validateQuestions(request.getQuestions());

        try {
            String questionnaireId = FhirUtils.qualifyId(id, ResourceType.Questionnaire);
            List<QuestionModel> questions = collectionToStream(request.getQuestions())
                .map(q -> dtoMapper.mapQuestionDto(q))
                .collect(Collectors.toList());
            List<QuestionModel> callToActions = collectionToStream(request.getCallToActions())
                .map(c -> dtoMapper.mapQuestionDto(c))
                .collect(Collectors.toList());

            questionnaireService.updateQuestionnaire(questionnaireId, request.getTitle(), request.getDescription(), request.getStatus(), questions, callToActions);
        }
        catch(AccessValidationException | ServiceException e) {
            throw toStatusCodeException(e);
        }

        return ResponseEntity.ok().build();
    }

    private void validateQuestions(List<QuestionDto> questions){
        if(questions == null || questions.isEmpty()) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }

        //All questions should have a unique ID
        List<String> ids = new ArrayList<String>();
        for (var question : questions){
            var idIsInList = ids.contains(question.getLinkId());
            if(idIsInList)
                throw new BadRequestException(ErrorDetails.QUESTIONS_ID_IS_NOT_UNIQUE);
            ids.add(question.getLinkId());
        }
    }
    private Stream<QuestionDto> collectionToStream(Collection<QuestionDto> collection) {
        return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
    }
}
