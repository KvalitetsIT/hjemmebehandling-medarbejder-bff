package dk.kvalitetsit.hjemmebehandling.repository.adaptation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireResponseRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

import java.util.List;
import java.util.Optional;

/**
 * Adapter responsible for translating between FHIR resources and domain-specific logic.
 * <p>
 * This class primarily handles the mapping of business models to domain representations
 * and delegates calls deeper into the application stack with the appropriate arguments.
 * <p>
 * Currently, it implements the {@link QuestionnaireResponseRepository} interface for {@link QuestionnaireResponseModel} entities.
 * Note that this implementation detail may change in the future.
 */
public class QuestionnaireResponseRepositoryAdaptor implements QuestionnaireResponseRepository<QuestionnaireResponseModel> {

    private final QuestionnaireResponseRepository<QuestionnaireResponse> repository;
    private final FhirMapper mapper;

    public QuestionnaireResponseRepositoryAdaptor(QuestionnaireResponseRepository<QuestionnaireResponse> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<QuestionnaireResponseModel> fetch(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds) throws ServiceException, AccessValidationException {
        return repository.fetch(carePlanId, questionnaireIds).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public List<QuestionnaireResponseModel> fetchByStatus(List<ExaminationStatus> statuses) throws ServiceException, AccessValidationException {
        return repository.fetchByStatus(statuses).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public List<QuestionnaireResponseModel> fetch(List<ExaminationStatus> statuses, QualifiedId.CarePlanId carePlanId) throws ServiceException, AccessValidationException {
        return repository.fetch(statuses, carePlanId).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public List<QuestionnaireResponseModel> fetchByStatus(ExaminationStatus status) throws ServiceException, AccessValidationException {
        return repository.fetchByStatus(status).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public void update(QuestionnaireResponseModel resource) throws ServiceException, AccessValidationException {
        repository.update(mapper.mapQuestionnaireResponseModel(resource));
    }

    @Override
    public QualifiedId.QuestionnaireResponseId save(QuestionnaireResponseModel resource) throws ServiceException {
        return repository.save(mapper.mapQuestionnaireResponseModel(resource));
    }

    @Override
    public Optional<QuestionnaireResponseModel> fetch(QualifiedId.QuestionnaireResponseId id) throws ServiceException, AccessValidationException {
        return repository.fetch(id).map(mapper::mapQuestionnaireResponse);
    }

    @Override
    public List<QuestionnaireResponseModel> fetch(List<QualifiedId.QuestionnaireResponseId> id) throws ServiceException, AccessValidationException {
        return repository.fetch(id).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public List<QuestionnaireResponseModel> fetch() throws ServiceException, AccessValidationException {
        return repository.fetch().stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public List<QuestionnaireResponseModel> history(QualifiedId.QuestionnaireResponseId id) throws ServiceException, AccessValidationException {
        return repository.history(id).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public List<QuestionnaireResponseModel> history(List<QualifiedId.QuestionnaireResponseId> ids) throws ServiceException, AccessValidationException {
        return repository.history(ids).stream().map(mapper::mapQuestionnaireResponse).toList();
    }
}
