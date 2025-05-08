package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
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
import org.hl7.fhir.r4.model.ResourceType;
import org.openapitools.api.QuestionnaireApi;
import org.openapitools.model.CreateQuestionnaireRequest;
import org.openapitools.model.PatchQuestionnaireRequest;
import org.openapitools.model.QuestionDto;
import org.openapitools.model.QuestionnaireDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

@RestController
public class QuestionnaireController extends BaseController implements QuestionnaireApi {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireController.class);

    private final QuestionnaireService questionnaireService;
    private final DtoMapper dtoMapper;
    private final LocationHeaderBuilder locationHeaderBuilder;

    public QuestionnaireController(QuestionnaireService questionnaireService, AuditLoggingService auditLoggingService, DtoMapper dtoMapper, LocationHeaderBuilder locationHeaderBuilder) {
        this.questionnaireService = questionnaireService;
        this.dtoMapper = dtoMapper;
        this.locationHeaderBuilder = locationHeaderBuilder;
    }

    @Override
    public ResponseEntity<Void> createQuestionnaire(CreateQuestionnaireRequest createQuestionnaireRequest) {
        validateQuestions(createQuestionnaireRequest.getQuestionnaire().getQuestions());

        QuestionnaireModel questionnaire = dtoMapper.mapQuestionnaireDto(createQuestionnaireRequest.getQuestionnaire());
        try {
            String questionnaireId = questionnaireService.createQuestionnaire(questionnaire);
            URI location = locationHeaderBuilder.buildLocationHeader(questionnaireId);
            return ResponseEntity.created(location).build();
        } catch (ServiceException e) {
            logger.error("Could not create questionnaire");
            throw toStatusCodeException(e);
        }
    }

    @Override
    public ResponseEntity<QuestionnaireDto> getQuestionnaireById(String id) {
        // Look up the Questionnaire
        Optional<QuestionnaireModel> questionnaire = Optional.empty();

        try {
            String questionnaireId = FhirUtils.qualifyId(id, ResourceType.Questionnaire);
            questionnaire = questionnaireService.getQuestionnaireById(questionnaireId);
        } catch (AccessValidationException | ServiceException e) {
            logger.error("Could not update questionnaire response", e);
            throw toStatusCodeException(e);
        }

        if (questionnaire.isEmpty()) {
            throw new ResourceNotFoundException(String.format("Questionnaire with id %s not found.", id), ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }
        return ResponseEntity.ok(dtoMapper.mapQuestionnaireModel(questionnaire.get()));
    }

    @Override
    public ResponseEntity<List<QuestionnaireDto>> getQuestionnaires(Optional<List<String>> statusesToInclude) {

        if (statusesToInclude.isPresent() && statusesToInclude.get().isEmpty()) {
            var details = ErrorDetails.PARAMETERS_INCOMPLETE;
            details.setDetails("Statusliste blev sendt med, men indeholder ingen elementer");
            throw new BadRequestException(details);
        }
        try {
            List<QuestionnaireModel> questionnaires = questionnaireService.getQuestionnaires(statusesToInclude.orElse(List.of()));

            return ResponseEntity.ok(questionnaires.stream().map(dtoMapper::mapQuestionnaireModel).sorted(Comparator.comparing(x -> x.getLastUpdated().orElse(null), Comparator.nullsFirst(OffsetDateTime::compareTo).reversed())).toList());


        } catch (ServiceException e) {
            throw toStatusCodeException(e);
        }
    }

    @Override
    public ResponseEntity<Boolean> isQuestionnaireInUse(String id) {
        System.out.printf("id: %s\n", id);
        boolean isQuestionnaireInUse;
        try {
            isQuestionnaireInUse = !questionnaireService.getPlanDefinitionsThatIncludes(id).isEmpty();
        } catch (ServiceException se) {
            throw toStatusCodeException(se);
        }

        return ResponseEntity.ok().body(isQuestionnaireInUse);
    }

    @Override
    public ResponseEntity<Void> patchQuestionnaire(String id, PatchQuestionnaireRequest patchQuestionnaireRequest) {

        validateQuestions(patchQuestionnaireRequest.getQuestions());

        try {
            String questionnaireId = FhirUtils.qualifyId(id, ResourceType.Questionnaire);

            List<QuestionModel> questions = collectionToStream(patchQuestionnaireRequest.getQuestions()).map(dtoMapper::mapQuestionDto).toList();

            QuestionModel callToAction = dtoMapper.mapQuestionDto(patchQuestionnaireRequest.getCallToAction());

            questionnaireService.updateQuestionnaire(questionnaireId, patchQuestionnaireRequest.getTitle(), patchQuestionnaireRequest.getDescription(), patchQuestionnaireRequest.getStatus(), questions, callToAction);
            return ResponseEntity.ok().build();


        } catch (AccessValidationException | ServiceException e) {
            throw toStatusCodeException(e);
        }
    }

    @Override
    public ResponseEntity<Void> retireQuestionnaire(String id) {
        try {
            questionnaireService.retireQuestionnaire(id);
        } catch (ServiceException se) {
            throw toStatusCodeException(se);
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updateQuestionnaire(QuestionnaireDto questionnaireDto) {
        throw new UnsupportedOperationException();
    }


    private void validateQuestions(List<QuestionDto> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }

        //All questions should have a unique ID
        List<String> ids = new ArrayList<String>();
        for (var question : questions) {

            var idIsInList = question.getLinkId().map(ids::contains).orElse(false);

            if (idIsInList) {
                throw new BadRequestException(ErrorDetails.QUESTIONS_ID_IS_NOT_UNIQUE);
            }

            question.getLinkId().ifPresent(ids::add);

            if (question.getQuestionType().isEmpty()) {
                throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
            }

            if (question.getQuestionType().get() == QuestionDto.QuestionTypeEnum.QUANTITY) {
                // todo: Handle 'Optional.get()' without 'isPresent()' check below
                var measurementType = question.getMeasurementType().get();
                if (measurementType.getCode().isEmpty() || measurementType.getDisplay().isEmpty() || measurementType.getSystem().isEmpty()) {
                    throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
                }
            }
        }
    }

    private Stream<QuestionDto> collectionToStream(Collection<QuestionDto> collection) {
        return Optional.ofNullable(collection).stream().flatMap(Collection::stream);
    }

}
