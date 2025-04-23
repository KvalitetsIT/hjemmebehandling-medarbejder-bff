package dk.kvalitetsit.hjemmebehandling.fhir.repository;


import ca.uhn.fhir.rest.gclient.ICriterion;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Questionnaire;

import java.util.*;

public class ConcreteQuestionnaireRepository implements QuestionnaireRepository<Questionnaire> {


    private final FhirClient client;

    public ConcreteQuestionnaireRepository(FhirClient client) {
        this.client = client;
    }

    @Override
    public void update(Questionnaire resource) {
        client.updateResource(resource);
    }

    @Override
    public String save(Questionnaire resource) throws ServiceException {
        return client.saveResource(resource);
    }

    @Override
    public Optional<Questionnaire> fetch(QualifiedId id) throws ServiceException {
        return Optional.ofNullable(getQuestionnairesById(id).getQuestionnaires().getFirst());
    }

    @Override
    public List<Questionnaire> fetch(List<QualifiedId> ids) throws ServiceException {
        return getQuestionnairesById(ids).getQuestionnaires();
    }

    @Override
    public List<Questionnaire> fetch() {
        throw new NotImplementedException();
    }

    @Override
    public List<Questionnaire> lookupVersionsOfQuestionnaireById(List<QualifiedId> ids) {
        List<org.hl7.fhir.r4.model.Questionnaire> resources = new LinkedList<>();
        ids.forEach(id -> {
            Bundle bundle = client.history().onInstance(new IdType("Questionnaire", id.id())).returnBundle(Bundle.class).execute();
            bundle.getEntry().stream().filter(bec -> bec.getResource() != null).forEach(x -> resources.add((org.hl7.fhir.r4.model.Questionnaire) x.getResource()));
        });
        return resources;
    }

    @Override
    public List<Questionnaire> lookupQuestionnairesByStatus(Collection<String> statusesToInclude) throws ServiceException {
        var criterias = new ArrayList<ICriterion<?>>();
        var organizationCriterion = client.buildOrganizationCriterion();
        criterias.add(organizationCriterion);

        if (!statusesToInclude.isEmpty()) {
            Collection<String> statusesToIncludeToLowered = statusesToInclude.stream().map(String::toLowerCase).toList(); //status should be to lowered
            var statusCriteron = org.hl7.fhir.r4.model.Questionnaire.STATUS.exactly().codes(statusesToIncludeToLowered);
            criterias.add(statusCriteron);
        }
        return client.lookupByCriteria(Questionnaire.class, criterias).getQuestionnaires();
    }

    private FhirLookupResult getQuestionnairesById(List<QualifiedId> questionnaireIds) throws ServiceException {
        var idCriterion = org.hl7.fhir.r4.model.Questionnaire.RES_ID.exactly().codes(questionnaireIds.stream().map(x -> x.id()).toList());
        var organizationCriterion = client.buildOrganizationCriterion();
        return client.lookupByCriteria(Questionnaire.class, List.of(idCriterion, organizationCriterion));
    }

    private FhirLookupResult getQuestionnairesById(QualifiedId questionnaireIds) throws ServiceException {
        var idCriterion = org.hl7.fhir.r4.model.Questionnaire.RES_ID.exactly().codes(questionnaireIds.id());
        var organizationCriterion = client.buildOrganizationCriterion();
        return client.lookupByCriteria(Questionnaire.class, List.of(idCriterion, organizationCriterion));
    }
}
