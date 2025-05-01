package dk.kvalitetsit.hjemmebehandling.repository.adaptation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Questionnaire;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * An adapter whose responsibility is to adapt between FHIR and the domain logic.
 * This primarily covers mapping from business models and calling further into the stack with the expected arguments
 * For now, it implements the QuestionnaireRepository interface, but this might change in the future
 */
public class QuestionnaireRepositoryAdaptor implements QuestionnaireRepository<QuestionnaireModel> {

    private final QuestionnaireRepository<Questionnaire> repository;
    private final FhirMapper mapper;

    public QuestionnaireRepositoryAdaptor(QuestionnaireRepository<Questionnaire> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<QuestionnaireModel> lookupQuestionnairesByStatus(Collection<String> statusesToInclude) throws ServiceException {
        return repository.lookupQuestionnairesByStatus(statusesToInclude).stream().map(mapper::mapQuestionnaire).toList();
    }

    @Override
    public List<QuestionnaireModel> lookupVersionsOfQuestionnaireById(List<QualifiedId.QuestionnaireId> ids) {
        return repository.lookupVersionsOfQuestionnaireById(ids).stream().map(mapper::mapQuestionnaire).toList();
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
    public Optional<QuestionnaireModel> fetch(QualifiedId.QuestionnaireId id) throws ServiceException {
        return repository.fetch(id).map(mapper::mapQuestionnaire);
    }

    @Override
    public List<QuestionnaireModel> fetch(List<QualifiedId.QuestionnaireId> id) throws ServiceException {
        return repository.fetch(id).stream().map(mapper::mapQuestionnaire).toList();
    }

    @Override
    public List<QuestionnaireModel> fetch() throws ServiceException {
        return repository.fetch().stream().map(mapper::mapQuestionnaire).toList();
    }
}
