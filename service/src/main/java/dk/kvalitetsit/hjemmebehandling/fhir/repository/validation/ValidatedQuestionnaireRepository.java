package dk.kvalitetsit.hjemmebehandling.fhir.repository.validation;

import dk.kvalitetsit.hjemmebehandling.fhir.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ValidatedQuestionnaireRepository implements QuestionnaireRepository<QuestionnaireModel> {

    private final AccessValidator accessValidator;
    private final QuestionnaireRepository<QuestionnaireModel> questionnaireRepository;

    public ValidatedQuestionnaireRepository(AccessValidator accessValidator, QuestionnaireRepository<QuestionnaireModel> questionnaireRepository) {
        this.accessValidator = accessValidator;
        this.questionnaireRepository = questionnaireRepository;
    }

    @Override
    public List<QuestionnaireModel> lookupQuestionnairesByStatus(Collection<String> statusesToInclude) throws ServiceException {
        return
    }

    @Override
    public List<QuestionnaireModel> lookupVersionsOfQuestionnaireById(List<QualifiedId.QuestionnaireId> ids) {
        return List.of();
    }

    @Override
    public void update(QuestionnaireModel resource) throws ServiceException {

    }

    @Override
    public QualifiedId.QuestionnaireId save(QuestionnaireModel resource) throws ServiceException {
        return null;
    }

    @Override
    public Optional<QuestionnaireModel> fetch(QualifiedId.QuestionnaireId id) throws ServiceException {
        return Optional.empty();
    }

    @Override
    public List<QuestionnaireModel> fetch(List<QualifiedId.QuestionnaireId> id) throws ServiceException {
        return List.of();
    }

    @Override
    public List<QuestionnaireModel> fetch() throws ServiceException {
        return List.of();
    }
}
