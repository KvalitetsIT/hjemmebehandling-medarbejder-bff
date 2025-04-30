package dk.kvalitetsit.hjemmebehandling.fhir.repository.adaptation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.repository.QuestionnaireResponseRepository;
import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

import java.util.List;
import java.util.Optional;

/**
 * An adapter whose responsibility is to adapt between FHIR and the domain logic.
 * This primarily covers mapping from business models and calling further into the stack with the expected arguments
 * For now, it implements the QuestionnaireResponseRepository interface, but this might change in the future
 */
public class QuestionnaireResponseRepositoryAdaptor implements QuestionnaireResponseRepository<QuestionnaireResponseModel> {

    private final QuestionnaireResponseRepository<QuestionnaireResponse> repository;
    private final FhirMapper mapper;

    public QuestionnaireResponseRepositoryAdaptor(QuestionnaireResponseRepository<QuestionnaireResponse> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<QuestionnaireResponseModel> fetch(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds) {
        return repository.fetch(carePlanId, questionnaireIds).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public List<QuestionnaireResponseModel> fetchByStatus(List<ExaminationStatus> statuses) throws ServiceException {
        return repository.fetchByStatus(statuses).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public List<QuestionnaireResponseModel> fetch(List<ExaminationStatus> statuses, QualifiedId.CarePlanId carePlanId) throws ServiceException {
        return repository.fetch(statuses, carePlanId).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public List<QuestionnaireResponseModel> fetchByStatus(ExaminationStatus status) throws ServiceException {
        return repository.fetchByStatus(status).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public void update(QuestionnaireResponseModel resource) throws ServiceException {
        repository.update(mapper.mapQuestionnaireResponseModel(resource));
    }

    @Override
    public QualifiedId.QuestionnaireResponseId save(QuestionnaireResponseModel resource) throws ServiceException {
        return repository.save(mapper.mapQuestionnaireResponseModel(resource));
    }

    @Override
    public Optional<QuestionnaireResponseModel> fetch(QualifiedId.QuestionnaireResponseId id) throws ServiceException {
        return repository.fetch(id).map(mapper::mapQuestionnaireResponse);
    }

    @Override
    public List<QuestionnaireResponseModel> fetch(List<QualifiedId.QuestionnaireResponseId> id) throws ServiceException {
        return repository.fetch(id).stream().map(mapper::mapQuestionnaireResponse).toList();
    }

    @Override
    public List<QuestionnaireResponseModel> fetch() throws ServiceException {
        return repository.fetch().stream().map(mapper::mapQuestionnaireResponse).toList();
    }
}
