package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.PartialUpdateQuestionnaireResponseRequest;
import dk.kvalitetsit.hjemmebehandling.api.QuestionnaireResponseDto;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
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
public class QuestionnaireResponseController extends BaseController {
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

    @GetMapping(value = "/v1/questionnaireresponse/{carePlanId}")
    public ResponseEntity<List<QuestionnaireResponseDto>> getQuestionnaireResponsesByCarePlanId(@PathVariable("carePlanId") String carePlanId, @RequestParam("questionnaireIds") List<String> questionnaireIds) {
        if(carePlanId == null || questionnaireIds == null || questionnaireIds.isEmpty()) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }

        try {
            List<QuestionnaireResponseModel> questionnaireResponses = questionnaireResponseService.getQuestionnaireResponses(carePlanId, questionnaireIds);

            return ResponseEntity.ok(questionnaireResponses.stream().map(qr -> dtoMapper.mapQuestionnaireResponseModel(qr)).collect(Collectors.toList()));
        }
        catch(AccessValidationException | ServiceException e) {
            logger.error("Could not look up questionnaire responses by cpr and questionnaire ids", e);
            throw toStatusCodeException(e);
        }
    }

    @GetMapping(value = "/v1/questionnaireresponse")
    public ResponseEntity<List<QuestionnaireResponseDto>> getQuestionnaireResponsesByStatus(@RequestParam("status") List<ExaminationStatus> statuses, int pageNumber, int pageSize) {
        if(statuses == null || statuses.isEmpty()) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }

        try {
            List<QuestionnaireResponseModel> questionnaireResponses = questionnaireResponseService.getQuestionnaireResponsesByStatus(statuses, new PageDetails(pageNumber, pageSize));

            return ResponseEntity.ok(questionnaireResponses.stream().map(qr -> dtoMapper.mapQuestionnaireResponseModel(qr)).collect(Collectors.toList()));
        }
        catch(ServiceException e) {
            logger.error("Could not look up questionnaire responses by status", e);
            throw toStatusCodeException(e);
        }
    }

    @PatchMapping(value = "/v1/questionnaireresponse/{id}")
    public ResponseEntity<Void> patchQuestionnaireResponse(@PathVariable String id, @RequestBody PartialUpdateQuestionnaireResponseRequest request) {
        if(request.getExaminationStatus() == null) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }

        try {
            questionnaireResponseService.updateExaminationStatus(id, request.getExaminationStatus());
        }
        catch(AccessValidationException | ServiceException e) {
            logger.error("Could not update questionnaire response", e);
            throw toStatusCodeException(e);
        }
        return ResponseEntity.ok().build();
    }
}
