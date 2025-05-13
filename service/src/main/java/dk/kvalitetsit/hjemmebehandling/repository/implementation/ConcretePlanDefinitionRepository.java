package dk.kvalitetsit.hjemmebehandling.repository.implementation;

import ca.uhn.fhir.rest.gclient.ICriterion;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.repository.PlanDefinitionRepository;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
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
        var idCriterion = org.hl7.fhir.r4.model.PlanDefinition.RES_ID.exactly().code(id.unqualified());
        var lookupResult = lookupPlanDefinitionsByCriteria(List.of(idCriterion));

        if (lookupResult.getPlanDefinitions().isEmpty()) {
            return Optional.empty();
        }
        if (lookupResult.getPlanDefinitions().size() != 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", PlanDefinition.class));
        }
        return Optional.of(lookupResult.getPlanDefinitions().getFirst());
    }

    @Override
    public List<PlanDefinition> fetch(List<QualifiedId.PlanDefinitionId> ids) throws ServiceException {
        return getPlanDefinitionsById(ids).getPlanDefinitions();
    }

    @Override
    public List<PlanDefinition> fetch() throws ServiceException {
        return lookupPlanDefinitionsByCriteria(List.of()).getPlanDefinitions();
    }

    @Override
    public List<PlanDefinition> lookupPlanDefinitionsByStatus(Collection<Status> statusesToInclude) throws ServiceException {
        var criterias = new ArrayList<ICriterion<?>>();

        if (!statusesToInclude.isEmpty()) {
            Collection<String> statusesToIncludeToLowered = statusesToInclude.stream().map(Enum::toString).toList(); //status should be to lowered
            var statusCriteron = PlanDefinition.STATUS.exactly().codes(statusesToIncludeToLowered);
            criterias.add(statusCriteron);
        }
        return lookupPlanDefinitionsByCriteria(criterias).getPlanDefinitions();
    }

    @Override
    public List<PlanDefinition> fetchActivePlanDefinitionsUsingQuestionnaireWithId(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException {
        var statusCriterion = PlanDefinition.STATUS.exactly().code(Enumerations.PublicationStatus.ACTIVE.toCode());
        var questionnaireCriterion = PlanDefinition.DEFINITION.hasId(questionnaireId.unqualified());

        List<ICriterion<? extends ca.uhn.fhir.rest.gclient.IParam>> criteria = new ArrayList<>(List.of(statusCriterion, questionnaireCriterion));
        return lookupPlanDefinitionsByCriteria(criteria).getPlanDefinitions();
    }

    private FhirLookupResult getPlanDefinitionsById(List<QualifiedId.PlanDefinitionId> ids) throws ServiceException {
        var idCriterion = org.hl7.fhir.r4.model.PlanDefinition.RES_ID.exactly().codes(ids.stream().map(QualifiedId::unqualified).toList());
        return lookupPlanDefinitionsByCriteria(List.of(idCriterion));
    }

    private FhirLookupResult lookupPlanDefinitionsByCriteria(List<ICriterion<?>> criteria) throws ServiceException {
        // Includes the Questionnaire resources.
        return client.lookupByCriteria(PlanDefinition.class, criteria, List.of(PlanDefinition.INCLUDE_DEFINITION));
    }



}
