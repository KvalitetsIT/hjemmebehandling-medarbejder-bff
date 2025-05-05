package dk.kvalitetsit.hjemmebehandling.repository.implementation;


import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.repository.PractitionerRepository;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Questionnaire;

import java.util.*;

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
        /*
        TODO:
         This is considered bad practice as we expect one entry, however this may result in a "NoSuchElementException".
         Introduce a lookup on the FhirClient which throws an exception if multiple entrees were found - only a single entry should be fetched otherwise illigalstateExecption
         (This is the case for all repository implementations and therefore this check should be located inside the client)
        */
        var result = getQuestionnairesById(id);

        if (result.size() > 1) {
            throw new IllegalStateException("Expected single entry, however multiple entrees were found");
        } else if ( result.isEmpty()) {
            return Optional.empty();
        }
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
    public List<Questionnaire> lookupVersionsOfQuestionnaireById(List<QualifiedId.QuestionnaireId> ids) {
        return client.lookupHistorical(ids, Questionnaire.class);
    }

    @Override
    public List<Questionnaire> fetch(Collection<String> statusesToInclude) throws ServiceException {
        var criterias = new ArrayList<ICriterion<?>>();
        var organizationId = userContextProvider.getUserContext().orgId().orElseThrow(() -> new ServiceException("Could not fetch questionnaires - Expected organisation id", ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE));
        var organizationCriterion = FhirUtils.buildOrganizationCriterion(organizationId);
        criterias.add(organizationCriterion);

        if (!statusesToInclude.isEmpty()) {
            Collection<String> statusesToIncludeToLowered = statusesToInclude.stream().map(String::toLowerCase).toList(); //status should be to lowered
            var statusCriterion = Questionnaire.STATUS.exactly().codes(statusesToIncludeToLowered);
            criterias.add(statusCriterion);
        }
        return client.lookupByCriteria(Questionnaire.class, criterias).getQuestionnaires();
    }

    private List<Questionnaire> getQuestionnairesById(List<QualifiedId.QuestionnaireId> questionnaireIds) throws ServiceException {
        var idCriterion = Questionnaire.RES_ID.exactly().codes(questionnaireIds.stream().map(QualifiedId.QuestionnaireId::unqualified).toList());
        var organizationId = userContextProvider.getUserContext().orgId().orElseThrow(() -> new ServiceException("Could not fetch questionnaires - Expected organisation id", ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE));
        var organizationCriterion = FhirUtils.buildOrganizationCriterion(organizationId);
        return client.lookupByCriteria(Questionnaire.class, List.of(idCriterion, organizationCriterion)).getQuestionnaires();
    }

    private List<Questionnaire> getQuestionnairesById(QualifiedId.QuestionnaireId questionnaireIds) throws ServiceException {
        var idCriterion = org.hl7.fhir.r4.model.Questionnaire.RES_ID.exactly().codes(questionnaireIds.unqualified());
        var organizationId = userContextProvider.getUserContext().orgId().orElseThrow(() -> new ServiceException("Could not fetch questionnaires - Expected organisation id", ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE));
        var organizationCriterion = FhirUtils.buildOrganizationCriterion(organizationId);
        return client.lookupByCriteria(Questionnaire.class, List.of(idCriterion, organizationCriterion)).getQuestionnaires();
    }
}
