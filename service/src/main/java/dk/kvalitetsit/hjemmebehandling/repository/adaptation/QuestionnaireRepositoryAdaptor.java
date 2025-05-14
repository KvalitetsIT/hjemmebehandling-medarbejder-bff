package dk.kvalitetsit.hjemmebehandling.repository.adaptation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Questionnaire;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adapter responsible for translating between FHIR resources and domain-specific logic.
 * <p>
 * This class primarily handles the mapping of business models to domain representations
 * and delegates calls deeper into the application stack with the appropriate arguments.
 * <p>
 * Currently, it implements the {@link QuestionnaireRepository} interface for {@link QuestionnaireModel} entities.
 * Note that this implementation detail may change in the future.
 */
public class QuestionnaireRepositoryAdaptor implements QuestionnaireRepository<QuestionnaireModel> {

    private final QuestionnaireRepository<Questionnaire> repository;
    private final FhirMapper mapper;

    public QuestionnaireRepositoryAdaptor(QuestionnaireRepository<Questionnaire> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<QuestionnaireModel> fetch(Collection<String> statusesToInclude) throws ServiceException, AccessValidationException {
        return repository.fetch(statusesToInclude).stream().map(mapper::mapQuestionnaire).toList();
    }

    @Override
    public void update(QuestionnaireModel resource) throws ServiceException {
        repository.update(mapper.mapQuestionnaireModel(resource));
    }

    @Override
    public QualifiedId.QuestionnaireId save(QuestionnaireModel resource) throws ServiceException {
        return repository.save(mapper.mapQuestionnaireModel(resource));
    }

    @Override
    public Optional<QuestionnaireModel> fetch(QualifiedId.QuestionnaireId id) throws ServiceException, AccessValidationException {
        return repository.fetch(id).map(mapper::mapQuestionnaire);
    }

    @Override
    public List<QuestionnaireModel> fetch(List<QualifiedId.QuestionnaireId> id) throws ServiceException, AccessValidationException {
        return repository.fetch(id).stream().map(mapper::mapQuestionnaire).toList();
    }

    @Override
    public List<QuestionnaireModel> fetch() throws ServiceException, AccessValidationException {
        return repository.fetch().stream().map(mapper::mapQuestionnaire).toList();
    }

    @Override
    public List<QuestionnaireModel> history(QualifiedId.QuestionnaireId id) throws ServiceException, AccessValidationException {
        return repository.history(id).stream().map(mapper::mapQuestionnaire).toList();
    }

    @Override
    public List<QuestionnaireModel> history(List<QualifiedId.QuestionnaireId> questionnaireIds) throws ServiceException, AccessValidationException {
        return repository.history(questionnaireIds).stream().map(mapper::mapQuestionnaire).toList();
    }
}
