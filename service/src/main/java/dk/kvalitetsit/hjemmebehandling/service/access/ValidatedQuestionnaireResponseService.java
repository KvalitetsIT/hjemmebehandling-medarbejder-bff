package dk.kvalitetsit.hjemmebehandling.service.access;

import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

public class ValidatedQuestionnaireResponseService implements QuestionnaireResponseService {

    private final QuestionnaireResponseService service;
    private final AccessValidator accessValidator;

    public ValidatedQuestionnaireResponseService(QuestionnaireResponseService service, AccessValidator accessValidator) {
        this.service = service;
        this.accessValidator = accessValidator;
    }

    @Override
    public List<QuestionnaireResponseModel> getQuestionnaireResponses(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public List<QuestionnaireResponseModel> getQuestionnaireResponses(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds, Pagination pagination) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses, Pagination pagination) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public QuestionnaireResponseModel updateExaminationStatus(QualifiedId.QuestionnaireResponseId questionnaireResponseId, ExaminationStatus examinationStatus) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }
}
