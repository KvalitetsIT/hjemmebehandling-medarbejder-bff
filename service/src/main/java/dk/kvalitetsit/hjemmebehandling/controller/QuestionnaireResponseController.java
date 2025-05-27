package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.logging.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.openapitools.api.QuestionnaireResponseApi;

import org.openapitools.model.PartialUpdateQuestionnaireResponseRequest;
import org.openapitools.model.QuestionnaireResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class QuestionnaireResponseController extends BaseController implements QuestionnaireResponseApi {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseController.class);

    private final QuestionnaireResponseService questionnaireResponseService;
    private final AuditLoggingService auditLoggingService;
    private final DtoMapper dtoMapper;

    public QuestionnaireResponseController(QuestionnaireResponseService questionnaireResponseService, AuditLoggingService auditLoggingService, DtoMapper dtoMapper) {
        this.questionnaireResponseService = questionnaireResponseService;
        this.auditLoggingService = auditLoggingService;
        this.dtoMapper = dtoMapper;
    }




    @Override
    public ResponseEntity<List<QuestionnaireResponseDto>> getQuestionnaireResponsesByCarePlanId(String carePlanId, List<String> questionnaireIds, Optional<Integer> limit, Optional<Integer> offset) {
        if (carePlanId == null || questionnaireIds == null || questionnaireIds.isEmpty()) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }
        try {

            List<QuestionnaireResponseModel> questionnaireResponses = questionnaireResponseService.getQuestionnaireResponses(
                    new QualifiedId.CarePlanId(carePlanId),
                    questionnaireIds.stream().map(QualifiedId.QuestionnaireId::new).toList()
            );

            auditLoggingService.log("GET /v1/questionnaireresponse/" + carePlanId, questionnaireResponses.stream().map(QuestionnaireResponseModel::patient).toList());

            var dtos = questionnaireResponses.stream().map(dtoMapper::mapQuestionnaireResponseModel).toList();

            return ResponseEntity.ok(dtos);
        } catch (AccessValidationException | ServiceException e) {
            logger.error("Could not look up questionnaire responses by cpr and questionnaire ids", e);
            throw toStatusCodeException(e);
        }
    }

    @Override
    public ResponseEntity<List<QuestionnaireResponseDto>> getQuestionnaireResponsesByStatus(List<org.openapitools.model.ExaminationStatus> status, Optional<Integer> limit, Optional<Integer> offset) {
        if (status == null || status.isEmpty()) {
            throw new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE);
        }
        try {
            List<QuestionnaireResponseModel> questionnaireResponses = questionnaireResponseService.getQuestionnaireResponsesByStatus(status.stream().map(dtoMapper::mapExaminationStatusDto).toList(), new Pagination(offset, limit));
            auditLoggingService.log("GET /v1/questionnaireresponse/", questionnaireResponses.stream().map(QuestionnaireResponseModel::patient).toList());
            return ResponseEntity.ok(questionnaireResponses.stream().map(dtoMapper::mapQuestionnaireResponseModel).toList());
        } catch (AccessValidationException | ServiceException e) {
            logger.error("Could not look up questionnaire responses by status", e);
            throw toStatusCodeException(e);
        }
    }




    @Override
    public ResponseEntity<Void> patchQuestionnaireResponse(String id, PartialUpdateQuestionnaireResponseRequest partialUpdateQuestionnaireResponseRequest) {
        try {
            var examinationStatus = partialUpdateQuestionnaireResponseRequest
                    .getExaminationStatus()
                    .map(dtoMapper::mapExaminationStatusDto)
                    .orElseThrow(() -> new BadRequestException(ErrorDetails.PARAMETERS_INCOMPLETE));

            QuestionnaireResponseModel questionnaireResponse = questionnaireResponseService.updateExaminationStatus(
                    new QualifiedId.QuestionnaireResponseId(id),
                    examinationStatus
            );

            auditLoggingService.log("PATCH /v1/questionnaireresponse/" + id, questionnaireResponse.patient());

            return ResponseEntity.ok().build();

        } catch (AccessValidationException | ServiceException e) {
            logger.error("Could not update questionnaire response", e);
            throw toStatusCodeException(e);
        }
    }
}
