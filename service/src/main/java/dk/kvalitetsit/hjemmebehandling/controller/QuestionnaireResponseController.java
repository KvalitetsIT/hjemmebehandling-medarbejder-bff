package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.PartialUpdateQuestionnaireResponseRequest;
import dk.kvalitetsit.hjemmebehandling.api.QuestionnaireResponseDto;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name = "QuestionnaireResponse", description = "API for manipulating and retrieving QuestionnaireResponses.")
public class QuestionnaireResponseController {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseController.class);

    private QuestionnaireResponseService questionnaireResponseService;
    private DtoMapper dtoMapper;

    public QuestionnaireResponseController(QuestionnaireResponseService questionnaireResponseService, DtoMapper dtoMapper) {
        this.questionnaireResponseService = questionnaireResponseService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping(value = "/v1/questionnaireresponse")
    public void createQuestionnaireResponse(QuestionnaireResponseDto questionnaireResponse) {
        throw new UnsupportedOperationException();
    }

    @GetMapping(value = "/v1/questionnaireresponse/{cpr}")
    public ResponseEntity<List<QuestionnaireResponseDto>> getQuestionnaireResponsesByCpr(@PathVariable("cpr") String cpr, @RequestParam("questionnaireIds") List<String> questionnaireIds) {
        if(cpr == null || questionnaireIds == null || questionnaireIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<QuestionnaireResponseModel> questionnaireResponses = questionnaireResponseService.getQuestionnaireResponses(cpr, questionnaireIds);
            if(questionnaireResponses.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(questionnaireResponses.stream().map(qr -> dtoMapper.mapQuestionnaireResponseModel(qr)).collect(Collectors.toList()));
        }
        catch(ServiceException e) {
            logger.error("Could not look up questionnaire responses by cpr and questionnaire ids", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/v1/questionnaireresponse")
    public ResponseEntity<List<QuestionnaireResponseDto>> getQuestionnaireResponsesByStatus(@RequestParam("status") List<ExaminationStatus> statuses, int pageNumber, int pageSize) {
        if(statuses == null || statuses.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<QuestionnaireResponseModel> questionnaireResponses = questionnaireResponseService.getQuestionnaireResponsesByStatus(statuses, new PageDetails(pageNumber, pageSize));
            if(questionnaireResponses.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(questionnaireResponses.stream().map(qr -> dtoMapper.mapQuestionnaireResponseModel(qr)).collect(Collectors.toList()));
        }
        catch(ServiceException e) {
            logger.error("Could not look up questionnaire responses by status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PatchMapping(value = "/v1/questionnaireresponse/{id}")
    public ResponseEntity<Void> patchQuestionnaireResponse(@PathVariable String id, @RequestBody PartialUpdateQuestionnaireResponseRequest request) {
        if(request.getExaminationStatus() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            questionnaireResponseService.updateExaminationStatus(id, request.getExaminationStatus());
        }
        catch(ServiceException e) {
            // TODO: Distinguish when 'id' did not exist (bad request), and anything else (internal server error).
            logger.error("Could not update questionnaire response", e);
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }
}
