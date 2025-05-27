package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;

import java.util.List;

public interface QuestionnaireResponseService {

    List<QuestionnaireResponseModel> getQuestionnaireResponses(
            QualifiedId.CarePlanId carePlanId,
            List<QualifiedId.QuestionnaireId> questionnaireIds
    ) throws ServiceException, AccessValidationException;

    List<QuestionnaireResponseModel> getQuestionnaireResponses(
            QualifiedId.CarePlanId carePlanId,
            List<QualifiedId.QuestionnaireId> questionnaireIds,
            Pagination pagination
    ) throws ServiceException, AccessValidationException;

    List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(
            List<ExaminationStatus> statuses,
            Pagination pagination
    ) throws ServiceException, AccessValidationException;

    QuestionnaireResponseModel updateExaminationStatus(
            QualifiedId.QuestionnaireResponseId questionnaireResponseId,
            ExaminationStatus examinationStatus
    ) throws ServiceException, AccessValidationException;

    List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException, AccessValidationException;
}
