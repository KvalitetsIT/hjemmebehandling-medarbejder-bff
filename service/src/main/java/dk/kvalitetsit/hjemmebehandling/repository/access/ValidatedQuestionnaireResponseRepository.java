package dk.kvalitetsit.hjemmebehandling.repository.access;

import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireResponseRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.List;
import java.util.Optional;

public class ValidatedQuestionnaireResponseRepository implements QuestionnaireResponseRepository<QuestionnaireResponseModel> {

    private final QuestionnaireResponseRepository<QuestionnaireResponseModel> repository;
    private final AccessValidator<QuestionnaireResponseModel> accessValidator;

    public ValidatedQuestionnaireResponseRepository(QuestionnaireResponseRepository<QuestionnaireResponseModel> repository, AccessValidator<QuestionnaireResponseModel> accessValidator) {
        this.repository = repository;
        this.accessValidator = accessValidator;
    }

    @Override
    public List<QuestionnaireResponseModel> fetch(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch(carePlanId, questionnaireIds));
    }

    @Override
    public List<QuestionnaireResponseModel> fetchByStatus(List<ExaminationStatus> statuses) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetchByStatus(statuses));
    }

    @Override
    public List<QuestionnaireResponseModel> fetch(List<ExaminationStatus> statuses, QualifiedId.CarePlanId carePlanId) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch(statuses, carePlanId));
    }

    @Override
    public List<QuestionnaireResponseModel> fetchByStatus(ExaminationStatus status) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetchByStatus(status));
    }

    @Override
    public void update(QuestionnaireResponseModel resource) throws ServiceException {
        repository.update(resource);
    }

    @Override
    public QualifiedId.QuestionnaireResponseId save(QuestionnaireResponseModel resource) throws ServiceException {
        return repository.save(resource);
    }

    @Override
    public Optional<QuestionnaireResponseModel> fetch(QualifiedId.QuestionnaireResponseId id) throws ServiceException, AccessValidationException {
        var result = repository.fetch(id);
        if (result.isPresent()) return Optional.of(accessValidator.validateAccess(result.get()));
        return result;
    }

    @Override
    public List<QuestionnaireResponseModel> fetch(List<QualifiedId.QuestionnaireResponseId> id) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch(id));
    }

    @Override
    public List<QuestionnaireResponseModel> fetch() throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch());
    }

    @Override
    public List<QuestionnaireResponseModel> history(QualifiedId.QuestionnaireResponseId id) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.history(id));
    }

    @Override
    public List<QuestionnaireResponseModel> history(List<QualifiedId.QuestionnaireResponseId> ids) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.history(ids));
    }
}
