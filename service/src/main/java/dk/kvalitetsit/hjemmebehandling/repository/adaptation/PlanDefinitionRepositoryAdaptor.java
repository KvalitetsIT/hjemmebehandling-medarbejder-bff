package dk.kvalitetsit.hjemmebehandling.repository.adaptation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.repository.PlanDefinitionRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.PlanDefinition;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PlanDefinitionRepositoryAdaptor implements PlanDefinitionRepository<PlanDefinitionModel> {

    private final PlanDefinitionRepository<PlanDefinition> repository;
    private final FhirMapper mapper;

    public PlanDefinitionRepositoryAdaptor(PlanDefinitionRepository<PlanDefinition> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<PlanDefinitionModel> lookupPlanDefinitionsByStatus(Collection<Status> statusesToInclude) throws ServiceException, AccessValidationException {
        return repository.lookupPlanDefinitionsByStatus(statusesToInclude).stream().map(mapper::mapPlanDefinition).toList();
    }

    @Override
    public List<PlanDefinitionModel> fetchActivePlanDefinitionsUsingQuestionnaireWithId(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException, AccessValidationException {
        return repository.fetchActivePlanDefinitionsUsingQuestionnaireWithId(questionnaireId).stream().map(mapper::mapPlanDefinition).toList();
    }

    public void update(PlanDefinition resource) throws ServiceException, AccessValidationException {
        repository.update(resource);
    }

    public QualifiedId.PlanDefinitionId save(PlanDefinition resource) throws ServiceException {
        return repository.save(resource);
    }

    @Override
    public void update(PlanDefinitionModel resource) throws ServiceException, AccessValidationException {
        repository.update(mapper.mapPlanDefinitionModel(resource));
    }

    @Override
    public QualifiedId.PlanDefinitionId save(PlanDefinitionModel resource) throws ServiceException {
        return repository.save(mapper.mapPlanDefinitionModel(resource));
    }

    @Override
    public Optional<PlanDefinitionModel> fetch(QualifiedId.PlanDefinitionId id) throws ServiceException, AccessValidationException {
        return repository.fetch(id).map(mapper::mapPlanDefinition);
    }

    @Override
    public List<PlanDefinitionModel> fetch(List<QualifiedId.PlanDefinitionId> id) throws ServiceException, AccessValidationException {
        return repository.fetch(id).stream().map(mapper::mapPlanDefinition).toList();
    }

    @Override
    public List<PlanDefinitionModel> fetch() throws ServiceException, AccessValidationException {
        return repository.fetch().stream().map(mapper::mapPlanDefinition).toList();
    }

    @Override
    public List<PlanDefinitionModel> history(QualifiedId.PlanDefinitionId id) throws ServiceException, AccessValidationException {
        return repository.history(id).stream().map(mapper::mapPlanDefinition).toList();
    }

    @Override
    public List<PlanDefinitionModel> history(List<QualifiedId.PlanDefinitionId> planDefinitionIds) throws ServiceException, AccessValidationException {
        return repository.history(planDefinitionIds).stream().map(mapper::mapPlanDefinition).toList();
    }
}
