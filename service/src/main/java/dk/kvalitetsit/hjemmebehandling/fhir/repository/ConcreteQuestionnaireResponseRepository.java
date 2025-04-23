package dk.kvalitetsit.hjemmebehandling.fhir.repository;

import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import dk.kvalitetsit.hjemmebehandling.constants.SearchParameters;
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
    public String save(QuestionnaireResponse resource) throws ServiceException {
        return client.saveResource(resource);
    }

    @Override
    public Optional<QuestionnaireResponse> fetch(QualifiedId id) throws ServiceException {
        var idCriterion = QuestionnaireResponse.RES_ID.exactly().code(id.id());
        var result =  lookupQuestionnaireResponseByCriteria(List.of(idCriterion));

        if (result.getPatients().isEmpty()) {
            return Optional.empty();
        }
        if (result.getPatients().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", org.hl7.fhir.r4.model.QuestionnaireResponse.class));
        }

        return Optional.of(result.getQuestionnaireResponses().getFirst());
    }

    @Override
    public List<QuestionnaireResponse> fetch(List<QualifiedId> id) throws ServiceException {
        throw new NotImplementedException();
    }


    @Override
    public List<QuestionnaireResponse> fetch() {
        throw new NotImplementedException();
    }


    public List<QuestionnaireResponse> fetchByStatus(QualifiedId carePlanId, List<QualifiedId> questionnaireIds) {
        var questionnaireCriterion = org.hl7.fhir.r4.model.QuestionnaireResponse.QUESTIONNAIRE.hasAnyOfIds(questionnaireIds.stream().map(QualifiedId::id).toList());
        var basedOnCriterion = org.hl7.fhir.r4.model.QuestionnaireResponse.BASED_ON.hasId(carePlanId.id());

        var result = lookupQuestionnaireResponseByCriteria(List.of(questionnaireCriterion, basedOnCriterion));
        return result.getQuestionnaireResponses();
    }


    @Override
    public List<QuestionnaireResponse> fetch(QualifiedId carePlanId, List<QualifiedId> questionnaireIds) {
        throw new NotImplementedException();
    }

    @Override
    public List<QuestionnaireResponse> fetchByStatus(List<ExaminationStatus> statuses) throws ServiceException {
        var statusCriterion = new TokenClientParam(SearchParameters.EXAMINATION_STATUS).exactly().codes(statuses.stream().map(Enum::toString).toArray(String[]::new));
        var organizationCriterion = client.buildOrganizationCriterion();

        var result = lookupQuestionnaireResponseByCriteria(List.of(statusCriterion, organizationCriterion));
        return result.getQuestionnaireResponses();
    }

    @Override
    public List<QuestionnaireResponse> fetch(List<ExaminationStatus> statuses, QualifiedId carePlanId) throws ServiceException {
        throw new NotImplementedException();
    }

    public List<QuestionnaireResponse> fetchByStatus(List<ExaminationStatus> statuses, QualifiedId carePlanId) throws ServiceException {
        var statusCriterion = new TokenClientParam(SearchParameters.EXAMINATION_STATUS).exactly().codes(statuses.stream().map(Enum::toString).toArray(String[]::new));
        var organizationCriterion = client.buildOrganizationCriterion();
        var basedOnCriterion = org.hl7.fhir.r4.model.QuestionnaireResponse.BASED_ON.hasId(carePlanId.id());

        var result = lookupQuestionnaireResponseByCriteria(List.of(statusCriterion, organizationCriterion, basedOnCriterion));

        return result.getQuestionnaireResponses();
    }

    @Override
    public List<QuestionnaireResponse> fetchByStatus(ExaminationStatus status) throws ServiceException {
        return fetchByStatus(List.of(status));
    }


    private FhirLookupResult lookupQuestionnaireResponseByCriteria(List<ICriterion<?>> criteria) {
        var questionnaireResponseResult = client.lookupByCriteria(org.hl7.fhir.r4.model.QuestionnaireResponse.class, criteria, List.of(org.hl7.fhir.r4.model.QuestionnaireResponse.INCLUDE_BASED_ON, org.hl7.fhir.r4.model.QuestionnaireResponse.INCLUDE_QUESTIONNAIRE, org.hl7.fhir.r4.model.QuestionnaireResponse.INCLUDE_SUBJECT));

        // We also need the planDefinitions, which are found by following the chain QuestionnaireResponse.based-on -> CarePlan.instantiates-canonical.
        // This requires a separate lookup.
        if (questionnaireResponseResult.getQuestionnaireResponses().isEmpty()) {
            return questionnaireResponseResult;
        }

        // Get the related planDefinitions
        List<String> planDefinitionIds = FhirClient.getPlanDefinitionIds(questionnaireResponseResult.getCarePlans());
        FhirLookupResult planDefinitionResult = getPlanDefinitionsById(planDefinitionIds);

        // Merge the results
        questionnaireResponseResult.merge(planDefinitionResult);

        // We also need to lookup the practitioner who (potentially) changed the examination status
        List<String> practitionerIds = FhirClient.getPractitionerIds(questionnaireResponseResult.getQuestionnaireResponses());
        if (!practitionerIds.isEmpty()) {
            FhirLookupResult practitionerResult = getPractitioners(practitionerIds);
            questionnaireResponseResult.merge(practitionerResult);
        }
        return questionnaireResponseResult;
    }


}
