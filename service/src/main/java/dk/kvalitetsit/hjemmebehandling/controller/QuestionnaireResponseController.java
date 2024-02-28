package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.PartialUpdateQuestionnaireResponseRequest;
import dk.kvalitetsit.hjemmebehandling.api.QuestionnaireResponseDto;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.api.PaginatedList;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
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

    private final QuestionnaireResponseService questionnaireResponseService;
    private final AuditLoggingService auditLoggingService;
    private final DtoMapper dtoMapper;

    public QuestionnaireResponseController(QuestionnaireResponseService questionnaireResponseService, AuditLoggingService auditLoggingService, DtoMapper dtoMapper) {
        this.questionnaireResponseService = questionnaireResponseService;
        this.auditLoggingService = auditLoggingService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping(value = "/v1/questionnaireresponse")
    public void createQuestionnaireResponse(QuestionnaireResponseDto questionnaireResponse) {
        throw new UnsupportedOperationException();
    }

    @GetMapping(value = "/v1/questionnaireresponse/{carePlanId}")
    public ResponseEntity<PaginatedList<QuestionnaireResponseDto>> getQuestionnaireResponsesByCarePlanId(
            @PathVariable("carePlanId") String carePlanId,
            @RequestParam("questionnaireIds") List<String> questionnaireIds,
            int pageNumber,
            int pageSize
    ) {
        if(carePlanId == null || questionnaireIds == null || questionnaireIds.isEmpty()) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }

        try {
            Pagination pagination = new Pagination(pageNumber,pageSize);

            List<QuestionnaireResponseModel> questionnaireResponses = questionnaireResponseService.getQuestionnaireResponses(carePlanId, questionnaireIds);

            auditLoggingService.log("GET /v1/questionnaireresponse/"+carePlanId, questionnaireResponses.stream().map(QuestionnaireResponseModel::getPatient).collect(Collectors.toList()));

            var dtos = questionnaireResponses
                    .stream()
                    .map(dtoMapper::mapQuestionnaireResponseModel)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new PaginatedList<>(dtos, pagination));
        }
        catch(AccessValidationException | ServiceException e) {
            logger.error("Could not look up questionnaire responses by cpr and questionnaire ids", e);
            throw toStatusCodeException(e);
        }
    }

    @GetMapping(value = "/v1/questionnaireresponse")
    public ResponseEntity<List<QuestionnaireResponseDto>> getQuestionnaireResponsesByStatus(
            @RequestParam("status") List<ExaminationStatus> statuses,
            int pageNumber,
            int pageSize
    ) {
        if(statuses == null || statuses.isEmpty()) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }

        try {
            List<QuestionnaireResponseModel> questionnaireResponses = questionnaireResponseService.getQuestionnaireResponsesByStatus(statuses, new Pagination(pageNumber, pageSize));
            auditLoggingService.log("GET /v1/questionnaireresponse/", questionnaireResponses.stream().map(QuestionnaireResponseModel::getPatient).collect(Collectors.toList()));
            return ResponseEntity.ok(questionnaireResponses.stream().map(dtoMapper::mapQuestionnaireResponseModel).collect(Collectors.toList()));
        }
        catch(Exception e) {
            logger.error("Could not look up questionnaire responses by status", e);
            throw toStatusCodeException(e);
        }
    }

    @PatchMapping(value = "/v1/questionnaireresponse/{id}")
    public ResponseEntity<Void> patchQuestionnaireResponse(
            @PathVariable String id,
            @RequestBody PartialUpdateQuestionnaireResponseRequest request
    ) {
        if(request.getExaminationStatus() == null) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }

        try {
            QuestionnaireResponseModel questionnaireResponse = questionnaireResponseService.updateExaminationStatus(id, request.getExaminationStatus());
            auditLoggingService.log("PATCH /v1/questionnaireresponse/"+id, questionnaireResponse.getPatient());
        }
        catch(AccessValidationException | ServiceException e) {
            logger.error("Could not update questionnaire response", e);
            throw toStatusCodeException(e);
        }
        return ResponseEntity.ok().build();
    }
}
