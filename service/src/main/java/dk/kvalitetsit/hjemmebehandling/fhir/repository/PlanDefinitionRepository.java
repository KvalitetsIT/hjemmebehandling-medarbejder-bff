package dk.kvalitetsit.hjemmebehandling.fhir.repository;


import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionStatus;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.Collection;
import java.util.List;

public interface PlanDefinitionRepository<PlanDefinition> extends Repository<PlanDefinition, QualifiedId.PlanDefinitionId> {

    /**
     * Looks up plan definitions filtered by status values.
     *
     * @param statusesToInclude Statuses to include.
     * @return List of matching plan definitions.
     * @throws ServiceException If the operation fails.
     */
    List<PlanDefinition> lookupPlanDefinitionsByStatus(Collection<PlanDefinitionStatus> statusesToInclude) throws ServiceException;

    /**
     * Fetches active plan definitions that reference the specified questionnaire.
     *
     * @param questionnaireId The ID of the questionnaire.
     * @return List of plan definitions using the questionnaire.
     * @throws ServiceException If the operation fails.
     */
    List<PlanDefinition> fetchActivePlanDefinitionsUsingQuestionnaireWithId(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException;





}
