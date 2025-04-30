package dk.kvalitetsit.hjemmebehandling.fhir.repository.implementation;

import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.fhir.repository.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.repository.QuestionnaireResponseRepository;
import dk.kvalitetsit.hjemmebehandling.model.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

import java.util.List;
import java.util.Optional;

public class ConcreteQuestionnaireResponseRepository implements QuestionnaireResponseRepository<QuestionnaireResponse> {

    private final FhirClient client;

    public ConcreteQuestionnaireResponseRepository(FhirClient client) {
        this.client = client;
    }

    @Override
    public void update(QuestionnaireResponse resource) {
        client.updateResource(resource);
    }

    @Override
    public QualifiedId.QuestionnaireResponseId save(QuestionnaireResponse resource) throws ServiceException {
        return new QualifiedId.QuestionnaireResponseId(client.saveResource(resource));
    }

    @Override
    public Optional<QuestionnaireResponse> fetch(QualifiedId.QuestionnaireResponseId id) throws ServiceException {
        var idCriterion = QuestionnaireResponse.RES_ID.exactly().code(id.id());
        var result =  lookupQuestionnaireResponseByCriteria(List.of(idCriterion));
        if (result.size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", org.hl7.fhir.r4.model.QuestionnaireResponse.class));
        }
        return Optional.of(result.getFirst());
    }

    @Override
    public List<QuestionnaireResponse> fetch(List<QualifiedId.QuestionnaireResponseId> id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<QuestionnaireResponse> fetch() {
        throw new NotImplementedException();
    }


    public List<QuestionnaireResponse> fetchByStatus(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds) {
        var questionnaireCriterion = QuestionnaireResponse.QUESTIONNAIRE.hasAnyOfIds(questionnaireIds.stream().map(QualifiedId::id).toList());
        var basedOnCriterion = QuestionnaireResponse.BASED_ON.hasId(carePlanId.id());
        return lookupQuestionnaireResponseByCriteria(List.of(questionnaireCriterion, basedOnCriterion));
    }

    @Override
    public List<QuestionnaireResponse> fetch(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds) {
        throw new NotImplementedException();
    }

    @Override
    public List<QuestionnaireResponse> fetchByStatus(List<ExaminationStatus> statuses) throws ServiceException {
        var statusCriterion = new TokenClientParam(SearchParameters.EXAMINATION_STATUS).exactly().codes(statuses.stream().map(Enum::toString).toArray(String[]::new));
        return lookupQuestionnaireResponseByCriteria(List.of(statusCriterion, organizationCriterion));
    }

    @Override
    public List<QuestionnaireResponse> fetch(List<ExaminationStatus> statuses, QualifiedId.CarePlanId carePlanId) throws ServiceException {
        throw new NotImplementedException();
    }

    public List<QuestionnaireResponse> fetchByStatus(List<ExaminationStatus> statuses, QualifiedId.CarePlanId carePlanId) throws ServiceException {
        var statusCriterion = new TokenClientParam(SearchParameters.EXAMINATION_STATUS).exactly().codes(statuses.stream().map(Enum::toString).toArray(String[]::new));
        var organizationCriterion = FhirUtils.buildOrganizationCriterion();
        var basedOnCriterion = QuestionnaireResponse.BASED_ON.hasId(carePlanId.id());
        return lookupQuestionnaireResponseByCriteria(List.of(statusCriterion, organizationCriterion, basedOnCriterion));
    }

    @Override
    public List<QuestionnaireResponse> fetchByStatus(ExaminationStatus status) throws ServiceException {
        return fetchByStatus(List.of(status));
    }

    private List<QuestionnaireResponse> lookupQuestionnaireResponseByCriteria(List<ICriterion<?>> criteria) {
        return client.lookupByCriteria(
                QuestionnaireResponse.class,
                criteria,
                List.of(QuestionnaireResponse.INCLUDE_BASED_ON,
                        QuestionnaireResponse.INCLUDE_QUESTIONNAIRE,
                        QuestionnaireResponse.INCLUDE_SUBJECT)
        ).getQuestionnaireResponses();
    }


}
