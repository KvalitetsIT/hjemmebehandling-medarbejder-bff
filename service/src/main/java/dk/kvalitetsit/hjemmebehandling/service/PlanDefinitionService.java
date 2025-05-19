package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.Collection;
import java.util.List;

public interface PlanDefinitionService {

    List<PlanDefinitionModel> getPlanDefinitions(Collection<Status> statusesToInclude) throws ServiceException, AccessValidationException;

    QualifiedId.PlanDefinitionId createPlanDefinition(PlanDefinitionModel planDefinition) throws ServiceException, AccessValidationException;

    // TODO: Breakdown this method into multiple methods
    void updatePlanDefinition(
            QualifiedId.PlanDefinitionId id,
            String name,
            Status status,
            List<QualifiedId.QuestionnaireId> questionnaireIds,
            List<ThresholdModel> thresholds
    ) throws ServiceException, AccessValidationException;

    void retirePlanDefinition(QualifiedId.PlanDefinitionId id) throws ServiceException, AccessValidationException;

    List<CarePlanModel> getCarePlansThatIncludes(QualifiedId.PlanDefinitionId id) throws ServiceException, AccessValidationException;
}
