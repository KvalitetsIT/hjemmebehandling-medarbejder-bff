package dk.kvalitetsit.hjemmebehandling.service.access;

import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.service.PlanDefinitionService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collection;
import java.util.List;

public class ValidatedPlanDefinitionService implements PlanDefinitionService {

    private final AccessValidator accessValidator;
    private final PlanDefinitionService service;

    public ValidatedPlanDefinitionService(AccessValidator accessValidator, PlanDefinitionService service) {
        this.accessValidator = accessValidator;
        this.service = service;
    }

    @Override
    public List<PlanDefinitionModel> getPlanDefinitions(Collection<Status> statusesToInclude) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public QualifiedId.PlanDefinitionId createPlanDefinition(PlanDefinitionModel planDefinition) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public void updatePlanDefinition(QualifiedId.PlanDefinitionId id, String name, Status status, List<QualifiedId.QuestionnaireId> questionnaireIds, List<ThresholdModel> thresholds) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public void retirePlanDefinition(QualifiedId.PlanDefinitionId id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> getCarePlansThatIncludes(QualifiedId.PlanDefinitionId id) throws ServiceException {
        throw new NotImplementedException();
    }
}
