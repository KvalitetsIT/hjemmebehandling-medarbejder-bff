package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.api.dto.ErrorDto;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.QuestionnaireDto;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.BaseQuestionDto;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.api.request.CreateQuestionnaireRequest;
import dk.kvalitetsit.hjemmebehandling.api.request.PatchQuestionnaireRequest;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.mapping.ToModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Number;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Text;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Choice.MultipleChoice;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Choice.SingleChoice;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Question;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Tag(name = "Questionnaire", description = "API for manipulating and retrieving Questionnaires.")
public class QuestionnaireController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireController.class);

    private final QuestionnaireService questionnaireService;
    private final AuditLoggingService auditLoggingService;

    private final LocationHeaderBuilder locationHeaderBuilder;

    public QuestionnaireController(QuestionnaireService questionnaireService, AuditLoggingService auditLoggingService, LocationHeaderBuilder locationHeaderBuilder) {
        this.questionnaireService = questionnaireService;
        this.auditLoggingService = auditLoggingService;
        this.locationHeaderBuilder = locationHeaderBuilder;
    }

    @Operation(summary = "Get all Questionnaires.", description = "Retrieves a list of Questionnaire.")
    @GetMapping(value = "/v1/questionnaire", produces = { "application/json" })
    public ResponseEntity<List<QuestionnaireDto>> getQuestionnaires(@RequestParam(value = "statusesToInclude", required = false) Optional<Collection<String>> statusesToInclude) {
        if(statusesToInclude.isPresent() && statusesToInclude.get().isEmpty()){
            var details = ErrorDetails.PARAMETERS_INCOMPLETE;
            details.setDetails("Statusliste blev sendt med, men indeholder ingen elementer");
            throw new BadRequestException(details);
        }

        List<QuestionnaireModel> questionnaires = questionnaireService.getQuestionnaires(statusesToInclude.orElseGet(List::of));

        return ResponseEntity.ok(questionnaires.stream()
                .map(QuestionnaireModel::toDto)
                .sorted(Comparator.comparing(QuestionnaireDto::getLastUpdated, Comparator.nullsFirst(Date::compareTo).reversed()))
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get Questionnaire by id.", description = "Retrieves a Questionnaire by its id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation.", content = @Content(
                    schema = @Schema(implementation = QuestionnaireDto.class))),
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

        if(questionnaire.isEmpty()) {
            throw new ResourceNotFoundException(String.format("Questionnaire with id %s not found.", id), ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }
        return ResponseEntity.ok(questionnaire.get().toDto());
    }

    @Operation(summary = "Create a new Questionnaire.", description = "Create a Questionnaire.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successful operation.", headers = { @Header(name = "Location", description = "URL pointing to the created Questionnaire.")}, content = @Content),
        @ApiResponse(responseCode = "500", description = "Error during creation of Questionnaire.", content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PostMapping(value = "/v1/questionnaire", consumes = { "application/json" })
    public ResponseEntity<Void> createQuestionnaire(@RequestBody CreateQuestionnaireRequest request) {
        validateQuestions(request.getQuestionnaire().getQuestions());

        QuestionnaireModel questionnaire = request.getQuestionnaire().toModel();
        
        List<BaseQuestion<?>> callToActions = collectionToStream(request.getQuestionnaire().getCallToActions())
                .map(ToModel::toModel)
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

            List<BaseQuestion<?>> questions = request.getQuestions()
                    .stream()
                .map(ToModel::toModel)
                .collect(Collectors.toList());

            List<BaseQuestion<?>> callToActions = collectionToStream(request.getCallToActions())
                .map(ToModel::toModel)
                .collect(Collectors.toList());

            questionnaireService.updateQuestionnaire(questionnaireId, request.getTitle(), request.getDescription(), request.getStatus(), questions, callToActions);

            return ResponseEntity.ok().build();

        }
        catch(AccessValidationException | ServiceException e) {
            throw toStatusCodeException(e);
        }
    }


    @PutMapping(value = "/v1/questionnaire/{id}/retire")
    public ResponseEntity<Void> retireQuestionnaire(@PathVariable String id) {
        try {
            questionnaireService.retireQuestionnaire(id);
        }
        catch(ServiceException se) {
            throw toStatusCodeException(se);
        }

        return ResponseEntity.ok().build();
    }


    @Operation(
            summary = "Checks if the questionnaire is in use by any planDefinitions",
            description = "Returns true if the questionnaire is in use by planDefinition and otherwise false if not"
    )
    @GetMapping(value = "/v1/questionnaire/{id}/used")
    public ResponseEntity<Boolean> isQuestionnaireInUse(@PathVariable String id) {
        System.out.printf("id: %s\n", id);
        boolean isQuestionnaireInUse;
        try {
            isQuestionnaireInUse = !questionnaireService.getPlanDefinitionsThatIncludes(id).isEmpty();
        }
        catch(ServiceException se) {
            throw toStatusCodeException(se);
        }

        return ResponseEntity.ok().body(isQuestionnaireInUse);

    }


    @GetMapping(value = "/v1/questionnaire/mock")
    @Operation(summary = "Get a questionnaire.", description = "Get a questionnaire")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Successful operation.",
                    headers = {},
                    content = @Content(
                            schema = @Schema())),
    })
    public ResponseEntity<QuestionnaireDto> getNewQuestionnaire(){

        Question<Text> q1 = new Question<Text>("Hvad er dit navn");
        q1.answer(new Text("Bob"));


        Question<Number> q2 = new Question<>("Hvor gammel er du?");
        q2.answer(new Number(20));


        MultipleChoice<Text> q3 = new MultipleChoice<>("Hvad hedder dine forældre?");
        q3.addOption(new Text("Bob"));
        q3.addOption(new Text("Alice"));
        q3.addOption(new Text("Charles"));

        q3.answer(List.of(new Text("Alice"), new Text("Charles")));


        SingleChoice<Text> q4 = new SingleChoice<>("Hvad hedder din mor?");
        q4.addOption(new Text("Bob"));
        q4.addOption(new Text("Alice"));
        q4.addOption(new Text("Charles"));

        q4.answer(new Text("Alice"));


        SingleChoice<Text> q5 = new SingleChoice<>("Er du syg?");
        q5.addOption(new Text("Ja"));
        q5.addOption(new Text("Nej"));

        q5.answer(new Text("Nej"));


        SingleChoice<Number> q6 = new SingleChoice<>("Hvor måltider har du spist i dag?");
        q6.addOption(new Number(1));
        q6.addOption(new Number(2));
        q6.addOption(new Number(3));
        q6.addOption(new Number(4));

        QuestionnaireModel questionnaire = new QuestionnaireModel();

        // Mock questionnaire
        questionnaire.setStatus(QuestionnaireStatus.ACTIVE);
        questionnaire.setTitle("some fake title");
        questionnaire.setDescription("some fake description");
        questionnaire.setVersion("1");
        questionnaire.setLastUpdated(new Date());
        questionnaire.setLastUpdated(new Date());
        questionnaire.setOrganizationId("Organization/fakemedicinsk");

        questionnaire.setId(new QualifiedId("1", ResourceType.Questionnaire));
        questionnaire.setQuestions(List.of(q1, q2, q3, q4, q5, q6));

        return ResponseEntity.accepted().body(questionnaire.toDto());
    }


    private void validateQuestions(List<BaseQuestionDto<?>> questions){
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

            /* TODO: Include code below. It has been temporarily excluded
            if (question.getQuestionType() == null) {
                throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
            }

            if (Objects.requireNonNull(question.getQuestionType()) == QuestionType.QUANTITY) {
                var measurementType = question.getMeasurementType();
                if (measurementType == null || (measurementType.getCode() == null || measurementType.getDisplay() == null) || measurementType.getSystem() == null) {
                    throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
                }
            }
            */

        }
    }
    private Stream<BaseQuestionDto<?>> collectionToStream(Collection<BaseQuestionDto<?>> collection) {
        return Optional.ofNullable(collection).stream().flatMap(Collection::stream);
    }
}
