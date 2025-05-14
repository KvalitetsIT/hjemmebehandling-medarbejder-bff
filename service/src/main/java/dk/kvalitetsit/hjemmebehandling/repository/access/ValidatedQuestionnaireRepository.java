package dk.kvalitetsit.hjemmebehandling.repository.access;

import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ValidatedQuestionnaireRepository implements QuestionnaireRepository<QuestionnaireModel> {

    private final AccessValidator<QuestionnaireModel> accessValidator;
    private final QuestionnaireRepository<QuestionnaireModel>repository;


    public ValidatedQuestionnaireRepository(AccessValidator<QuestionnaireModel> accessValidator, QuestionnaireRepository<QuestionnaireModel> repository) {
        this.accessValidator = accessValidator;
        this.repository = repository;
    }

    @Override
    public List<QuestionnaireModel> fetch(Collection<String> statusesToInclude) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch(statusesToInclude));
    }

    @Override
    public void update(QuestionnaireModel resource) throws ServiceException {
        repository.update(resource);
    }

    @Override
    public QualifiedId.QuestionnaireId save(QuestionnaireModel resource) throws ServiceException {
        return repository.save(resource);
    }

    @Override
    public Optional<QuestionnaireModel> fetch(QualifiedId.QuestionnaireId id) throws ServiceException, AccessValidationException {
        var result = repository.fetch(id);
        if (result.isPresent()) return Optional.of(accessValidator.validateAccess(result.get()));
        return result;
    }

    @Override
    public List<QuestionnaireModel> fetch(List<QualifiedId.QuestionnaireId> id) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch(id));
    }

    @Override
    public List<QuestionnaireModel> fetch() throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch());
    }

    @Override
    public List<QuestionnaireModel> history(QualifiedId.QuestionnaireId id) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.history(id));
    }

    @Override
    public List<QuestionnaireModel> history(List<QualifiedId.QuestionnaireId> questionnaireIds) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.history(questionnaireIds));
    }
}
