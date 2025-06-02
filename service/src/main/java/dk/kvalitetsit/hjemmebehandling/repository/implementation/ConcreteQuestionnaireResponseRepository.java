package dk.kvalitetsit.hjemmebehandling.repository.implementation;

import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireResponseRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

import java.util.List;
import java.util.Optional;

/**
 * A concrete implementation of the {@link QuestionnaireResponseRepository} interface for managing
 * {@link QuestionnaireResponse} entities.
 * <p>
 * This class provides the underlying logic to retrieve, store, and manipulate organization-related data
 * within the domain, serving as the bridge between the domain model and data source.
 */
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
        var idCriterion = QuestionnaireResponse.RES_ID.exactly().code(id.unqualified());
        var result = lookupQuestionnaireResponseByCriteria(List.of(idCriterion));
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

    @Override
    public List<QuestionnaireResponse> history(QualifiedId.QuestionnaireResponseId id) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public List<QuestionnaireResponse> history(List<QualifiedId.QuestionnaireResponseId> questionnaireResponseIds) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }


    public List<QuestionnaireResponse> fetchByStatus(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds) throws ServiceException {
        var questionnaireCriterion = QuestionnaireResponse.QUESTIONNAIRE.hasAnyOfIds(questionnaireIds.stream().map(QualifiedId::unqualified).toList());
        var basedOnCriterion = QuestionnaireResponse.BASED_ON.hasId(carePlanId.unqualified());
        return lookupQuestionnaireResponseByCriteria(List.of(questionnaireCriterion, basedOnCriterion));
    }

    @Override
    public List<QuestionnaireResponse> fetch(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds) {
        throw new NotImplementedException();
    }

    @Override
    public List<QuestionnaireResponse> fetchByStatus(List<ExaminationStatus> statuses) throws ServiceException {
        var statusCriterion = new TokenClientParam(SearchParameters.EXAMINATION_STATUS).exactly().codes(statuses.stream().map(Enum::toString).toArray(String[]::new));
        return lookupQuestionnaireResponseByCriteria(List.of(statusCriterion));
    }

    @Override
    public List<QuestionnaireResponse> fetch(List<ExaminationStatus> statuses, QualifiedId.CarePlanId carePlanId) throws ServiceException {
        throw new NotImplementedException();
    }

    public List<QuestionnaireResponse> fetchByStatus(List<ExaminationStatus> statuses, QualifiedId.CarePlanId carePlanId) throws ServiceException {
        var statusCriterion = new TokenClientParam(SearchParameters.EXAMINATION_STATUS).exactly().codes(statuses.stream().map(Enum::toString).toArray(String[]::new));
        var basedOnCriterion = QuestionnaireResponse.BASED_ON.hasId(carePlanId.unqualified());
        return lookupQuestionnaireResponseByCriteria(List.of(statusCriterion, basedOnCriterion));
    }

    @Override
    public List<QuestionnaireResponse> fetchByStatus(ExaminationStatus status) throws ServiceException {
        return fetchByStatus(List.of(status));
    }

    private List<QuestionnaireResponse> lookupQuestionnaireResponseByCriteria(List<ICriterion<?>> criteria) throws ServiceException {
        return client.fetchByCriteria(
                QuestionnaireResponse.class,
                criteria,
                List.of(QuestionnaireResponse.INCLUDE_BASED_ON, QuestionnaireResponse.INCLUDE_QUESTIONNAIRE, QuestionnaireResponse.INCLUDE_SUBJECT)
        );
    }


}
