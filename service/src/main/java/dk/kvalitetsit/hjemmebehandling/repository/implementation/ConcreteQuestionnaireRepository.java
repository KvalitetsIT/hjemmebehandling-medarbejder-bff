package dk.kvalitetsit.hjemmebehandling.repository.implementation;


import ca.uhn.fhir.rest.gclient.ICriterion;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.OrganizationModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.Questionnaire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A concrete implementation of the {@link QuestionnaireRepository} interface for managing
 * {@link Questionnaire} entities.
 * <p>
 * This class provides the underlying logic to retrieve, store, and manipulate organization-related data
 * within the domain, serving as the bridge between the domain model and data source.
 */
public class ConcreteQuestionnaireRepository implements QuestionnaireRepository<Questionnaire> {


    private final FhirClient client;
    private final UserContextProvider userContextProvider;

    public ConcreteQuestionnaireRepository(FhirClient client, UserContextProvider userContextProvider) {
        this.client = client;
        this.userContextProvider = userContextProvider;
    }

    @Override
    public void update(Questionnaire resource) {
        client.updateResource(resource);
    }

    @Override
    public QualifiedId.QuestionnaireId save(Questionnaire resource) throws ServiceException {
        return new QualifiedId.QuestionnaireId(client.saveResource(resource));
    }

    @Override
    public Optional<Questionnaire> fetch(QualifiedId.QuestionnaireId id) throws ServiceException {
        var result = getQuestionnairesById(id);
        if (result.size() > 1)
            throw new IllegalStateException("Expected single entry, however multiple entrees were found");
        else if (result.isEmpty()) return Optional.empty();
        return Optional.ofNullable(result.getFirst());
    }

    @Override
    public List<Questionnaire> fetch(List<QualifiedId.QuestionnaireId> ids) throws ServiceException {
        return this.getQuestionnairesById(ids);
    }

    @Override
    public List<Questionnaire> fetch() {
        throw new NotImplementedException();
    }

    @Override
    public List<Questionnaire> history(QualifiedId.QuestionnaireId id) throws ServiceException, AccessValidationException {
        return client.lookupHistorical(id, Questionnaire.class);
    }

    @Override
    public List<Questionnaire> history(List<QualifiedId.QuestionnaireId> ids) throws ServiceException, AccessValidationException {
        return client.lookupHistorical(ids, Questionnaire.class);
    }

    @Override
    public List<Questionnaire> fetch(Collection<String> statusesToInclude) throws ServiceException {
        var criteria = new ArrayList<ICriterion<?>>();
        var organizationId = getOrganizationId();
        var organizationCriterion = FhirUtils.buildOrganizationCriterion(organizationId);
        criteria.add(organizationCriterion);

        if (!statusesToInclude.isEmpty()) {
            Collection<String> statusesToIncludeToLowered = statusesToInclude.stream().map(String::toLowerCase).toList(); //status should be to lowered
            var statusCriterion = Questionnaire.STATUS.exactly().codes(statusesToIncludeToLowered);
            criteria.add(statusCriterion);
        }
        return client.lookupByCriteria(Questionnaire.class, criteria);
    }

    private List<Questionnaire> getQuestionnairesById(List<QualifiedId.QuestionnaireId> questionnaireIds) throws ServiceException {
        var idCriterion = Questionnaire.RES_ID.exactly().codes(questionnaireIds.stream().map(QualifiedId.QuestionnaireId::unqualified).toList());
        var organizationId = getOrganizationId();

        var organizationCriterion = FhirUtils.buildOrganizationCriterion(organizationId);
        return client.lookupByCriteria(Questionnaire.class, List.of(idCriterion, organizationCriterion));
    }

    private QualifiedId.OrganizationId getOrganizationId() throws ServiceException {
        return userContextProvider.getUserContext().organization()
                .map(OrganizationModel::id)
                .orElseThrow(() -> new ServiceException(
                        "Could not fetch questionnaires - Expected organisation id",
                        ErrorKind.BAD_REQUEST,
                        ErrorDetails.MISSING_SOR_CODE
                ));
    }

    private List<Questionnaire> getQuestionnairesById(QualifiedId.QuestionnaireId questionnaireIds) throws ServiceException {
        var idCriterion = Questionnaire.RES_ID.exactly().codes(questionnaireIds.unqualified());
        var organizationId = getOrganizationId();

        var organizationCriterion = FhirUtils.buildOrganizationCriterion(organizationId);
        return client.lookupByCriteria(Questionnaire.class, List.of(idCriterion, organizationCriterion));
    }
}
