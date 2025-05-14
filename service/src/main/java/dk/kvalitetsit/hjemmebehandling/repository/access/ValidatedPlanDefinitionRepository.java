package dk.kvalitetsit.hjemmebehandling.repository.access;

import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.repository.PlanDefinitionRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ValidatedPlanDefinitionRepository implements PlanDefinitionRepository<PlanDefinitionModel> {

    private final AccessValidator<PlanDefinitionModel> accessValidator;
    private final PlanDefinitionRepository<PlanDefinitionModel> repository;

    public ValidatedPlanDefinitionRepository(AccessValidator<PlanDefinitionModel> accessValidator, PlanDefinitionRepository<PlanDefinitionModel> repository) {
        this.accessValidator = accessValidator;
        this.repository = repository;
    }

    @Override
    public List<PlanDefinitionModel> lookupPlanDefinitionsByStatus(Collection<Status> statusesToInclude) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.lookupPlanDefinitionsByStatus(statusesToInclude));
    }

    @Override
    public List<PlanDefinitionModel> fetchActivePlanDefinitionsUsingQuestionnaireWithId(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetchActivePlanDefinitionsUsingQuestionnaireWithId(questionnaireId));
    }

    @Override
    public void update(PlanDefinitionModel resource) throws ServiceException {
        repository.update(resource);
    }

    @Override
    public QualifiedId.PlanDefinitionId save(PlanDefinitionModel resource) throws ServiceException {
        return repository.save(resource);
    }

    @Override
    public Optional<PlanDefinitionModel> fetch(QualifiedId.PlanDefinitionId id) throws ServiceException, AccessValidationException {
        var result = repository.fetch(id);
        if (result.isPresent()) return Optional.of(accessValidator.validateAccess(result.get()));
        return result;
    }

    @Override
    public List<PlanDefinitionModel> fetch(List<QualifiedId.PlanDefinitionId> id) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch(id));
    }

    @Override
    public List<PlanDefinitionModel> fetch() throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch());
    }

    @Override
    public List<PlanDefinitionModel> history(QualifiedId.PlanDefinitionId id) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.history(id));
    }

    @Override
    public List<PlanDefinitionModel> history(List<QualifiedId.PlanDefinitionId> planDefinitionIds) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.history(planDefinitionIds));
    }
}
