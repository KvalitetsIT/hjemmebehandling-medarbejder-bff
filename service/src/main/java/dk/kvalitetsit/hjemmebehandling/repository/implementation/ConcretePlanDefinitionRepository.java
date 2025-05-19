package dk.kvalitetsit.hjemmebehandling.repository.implementation;

import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IParam;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.repository.PlanDefinitionRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.PlanDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A concrete implementation of the {@link PlanDefinitionRepository} interface for managing
 * {@link PlanDefinition} entities.
 * <p>
 * This class provides the underlying logic to retrieve, store, and manipulate organization-related data
 * within the domain, serving as the bridge between the domain model and data source.
 */
public class ConcretePlanDefinitionRepository implements PlanDefinitionRepository<PlanDefinition> {

    private final FhirClient client;

    public ConcretePlanDefinitionRepository(FhirClient client) {
        this.client = client;
    }

    @Override
    public void update(PlanDefinition resource) {
        client.updateResource(resource);
    }

    @Override
    public QualifiedId.PlanDefinitionId save(PlanDefinition resource) throws ServiceException {
        return new QualifiedId.PlanDefinitionId(client.saveResource(resource));
    }

    @Override
    public Optional<PlanDefinition> fetch(QualifiedId.PlanDefinitionId id) throws ServiceException {
        var idCriterion = PlanDefinition.RES_ID.exactly().code(id.unqualified());
        var planDefinitions = lookupPlanDefinitionsByCriteria(List.of(idCriterion));

        if (planDefinitions.isEmpty()) return Optional.empty();

        if (planDefinitions.size() != 1) throw new IllegalStateException(String.format(
                "Could not lookup single resource of class %s!",
                PlanDefinition.class
        ));

        return Optional.of(planDefinitions.getFirst());
    }

    @Override
    public List<PlanDefinition> fetch(List<QualifiedId.PlanDefinitionId> ids) throws ServiceException {
        return getPlanDefinitionsById(ids);
    }

    @Override
    public List<PlanDefinition> fetch() throws ServiceException {
        return lookupPlanDefinitionsByCriteria(List.of());
    }

    @Override
    public List<PlanDefinition> history(QualifiedId.PlanDefinitionId id) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public List<PlanDefinition> history(List<QualifiedId.PlanDefinitionId> planDefinitionIds) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public List<PlanDefinition> lookupPlanDefinitionsByStatus(Collection<Status> statusesToInclude) throws ServiceException {
        var criteria = new ArrayList<ICriterion<?>>();

        if (!statusesToInclude.isEmpty()) {
            Collection<String> statusesToIncludeToLowered = statusesToInclude.stream().map(Enum::toString).toList(); //status should be to lowered
            var statusCriterion = PlanDefinition.STATUS.exactly().codes(statusesToIncludeToLowered);
            criteria.add(statusCriterion);
        }
        return lookupPlanDefinitionsByCriteria(criteria);
    }

    @Override
    public List<PlanDefinition> fetchActivePlanDefinitionsUsingQuestionnaireWithId(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException {
        var statusCriterion = PlanDefinition.STATUS.exactly().code(Enumerations.PublicationStatus.ACTIVE.toCode());
        var questionnaireCriterion = PlanDefinition.DEFINITION.hasId(questionnaireId.unqualified());

        List<ICriterion<? extends IParam>> criteria = new ArrayList<>(List.of(statusCriterion, questionnaireCriterion));
        return lookupPlanDefinitionsByCriteria(criteria);
    }

    private List<PlanDefinition> getPlanDefinitionsById(List<QualifiedId.PlanDefinitionId> ids) throws ServiceException {
        var idCriterion = org.hl7.fhir.r4.model.PlanDefinition.RES_ID.exactly().codes(ids.stream().map(QualifiedId::unqualified).toList());
        return lookupPlanDefinitionsByCriteria(List.of(idCriterion));
    }

    private List<PlanDefinition> lookupPlanDefinitionsByCriteria(List<ICriterion<?>> criteria) throws ServiceException {
        return client.lookupByCriteria(PlanDefinition.class, criteria, List.of(PlanDefinition.INCLUDE_DEFINITION));
    }


}
