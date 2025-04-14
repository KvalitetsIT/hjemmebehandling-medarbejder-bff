package dk.kvalitetsit.hjemmebehandling.service;


import dk.kvalitetsit.hjemmebehandling.api.PaginatedList;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionnaireResponseService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseService.class);


    private final Client<
                CarePlanModel,
                PlanDefinitionModel,
                PractitionerModel,
                PatientModel,
                QuestionnaireModel,
                QuestionnaireResponseModel,
                Organization,
                CarePlanStatus> fhirClient;


    private final Comparator<QuestionnaireResponseModel> priorityComparator;

    public QuestionnaireResponseService(
            Client<
                                CarePlanModel,
                                PlanDefinitionModel,
                                PractitionerModel,
                                PatientModel,
                                QuestionnaireModel,
                                QuestionnaireResponseModel,
                                Organization,
                                CarePlanStatus> fhirClient,
            Comparator<QuestionnaireResponseModel> priorityComparator,
            AccessValidator accessValidator
    ) {
        super(accessValidator);

        this.fhirClient = fhirClient;

        this.priorityComparator = priorityComparator;
    }

    public PaginatedList<QuestionnaireResponseModel> getQuestionnaireResponsesWithTotal(String carePlanId, List<String> questionnaireIds, Pagination pagination) throws ServiceException, AccessValidationException {
        return new PaginatedList<>(this.getQuestionnaireResponses(carePlanId, questionnaireIds), pagination);
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponses(String carePlanId, List<String> questionnaireIds) throws ServiceException, AccessValidationException {
        List<QuestionnaireModel> historicalQuestionnaires = fhirClient.lookupVersionsOfQuestionnaireById(questionnaireIds);

        List<QuestionnaireResponseModel> responses =fhirClient.lookupQuestionnaireResponses(carePlanId, questionnaireIds);

        if (responses.isEmpty()) return List.of();

        // Validate that the user is allowed to retrieve the QuestionnaireResponses.
        //validateAccess(responses);

        // Sort the responses by priority.
        responses = sortResponsesByDate(responses);

        var orgId = fhirClient.getOrganizationId();

        return responses;

    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponses(String carePlanId, List<String> questionnaireIds, Pagination pagination) throws ServiceException, AccessValidationException {

        if (pagination != null) {
            // TODO: the pagination is supposed to be done during the fhir request. otherwise, a lot of entrees may be requested which may not be relevant
            // Perform paging if required.
            return pageResponses(this.getQuestionnaireResponses(carePlanId, questionnaireIds), pagination);
        }
        return this.getQuestionnaireResponses(carePlanId, questionnaireIds);
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException, AccessValidationException {


        List<QuestionnaireResponseModel> responses = fhirClient.lookupQuestionnaireResponsesByStatus(statuses);

        // Validate that the user is allowed to retrieve the QuestionnaireResponses.
        //validateAccess(responses);

        if (responses.isEmpty()) return List.of();

        // below is supposed to return a list of ids in the following format: "questionnaire-infektionsmedicinsk-1"
        List<String> ids = responses
                .stream()
                .map(questionnaire -> questionnaire.id().toString())
                .toList();

        List<QuestionnaireModel> historicalQuestionnaires = fhirClient.lookupVersionsOfQuestionnaireById(ids);


        // Filter the responses: We want only one response per <patientId, questionnaireId>-pair,
        // and in case of multiple entries, we want the 'most important' one.
        // Grouping, ordering and pagination should ideally happen in the FHIR-server, but the grouping part seems to
        // require a server extension. So for now, we do it here.
        responses = filterResponses(responses);

        // Sort the responses by priority.
        responses = sortResponses(responses);


        var orgId = fhirClient.getOrganizationId();

        return responses;

    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses, Pagination pagination) throws ServiceException, AccessValidationException {
        var responses = getQuestionnaireResponsesByStatus(statuses);

        // Perform paging if required.
        if (pagination == null) {
            return responses;
        }
        return pageResponses(responses, pagination);
    }


    public QuestionnaireResponseModel updateExaminationStatus(String questionnaireResponseId, ExaminationStatus examinationStatus) throws ServiceException, AccessValidationException {
        // Look up the QuestionnaireResponse
        String qualifiedId = FhirUtils.qualifyId(questionnaireResponseId, ResourceType.QuestionnaireResponse);

        var questionnaireResponse = fhirClient.lookupQuestionnaireResponseById(questionnaireResponseId)
                .orElseThrow(() -> new ServiceException(String.format("Could not look up QuestionnaireResponse by id %s!", questionnaireResponseId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_RESPONSE_DOES_NOT_EXIST));

        // Validate that the user is allowed to update the QuestionnaireResponse.
        //validateAccess(questionnaireResponse);

        var orgId = fhirClient.getOrganizationId();

        PractitionerModel user = fhirClient.getOrCreateUserAsPractitioner();

        QuestionnaireResponseModel mappedResponse = QuestionnaireResponseModel.Builder
                .from(questionnaireResponse)
                .examinationStatus(examinationStatus)
                .examinationAuthor(user)
                .build();

        // Save the updated QuestionnaireResponse
        fhirClient.updateQuestionnaireResponse(mappedResponse);
        return mappedResponse;
    }

    private List<QuestionnaireResponseModel> filterResponses(List<QuestionnaireResponseModel> responses) {
        // Given the list of responses, ensure that only one QuestionnaireResponse exists for each <patientId, questionnaireId>-pair,
        // and in case of duplicates, the one with the highest priority is retained.

        // Group the responses by  <patientId, questionnaireId>.
        var groupedResponses = responses
                .stream()
                .collect(Collectors.groupingBy(r -> new ImmutablePair<>(r.id(), r.questionnaireId())));

        // For each of the pairs, retain only the response with maximal priority.
        return groupedResponses.values()
                .stream()
                .map(this::extractMaximalPriorityResponse)
                .toList();
    }

    private List<QuestionnaireResponseModel> sortResponses(List<QuestionnaireResponseModel> responses) {
        return responses
                .stream()
                .sorted(priorityComparator)
                .toList();
    }

    private static List<QuestionnaireResponseModel> sortResponsesByDate(List<QuestionnaireResponseModel> responses) {
        return responses
                .stream()
                //.sorted((a, b) -> b.getAuthored().compareTo(a.getAuthored()))
                .toList();
    }

    private static List<QuestionnaireResponseModel> pageResponses(List<QuestionnaireResponseModel> responses, Pagination pagination) {
        return responses
                .stream()
                .skip((long) (pagination.getOffset() - 1) * pagination.getLimit())
                .limit(pagination.getLimit())
                .toList();
    }

    private  QuestionnaireResponseModel extractMaximalPriorityResponse(List<QuestionnaireResponseModel> responses) {
        var response = responses
                .stream()
                .min(priorityComparator); // priorityComperator is ordering elements from high to low, so extract the first element (eg. highest priority)
        if (response.isEmpty()) {
            throw new IllegalStateException("Could not extract QuestionnaireResponse of maximal priority - the list was empty!");
        }
        return response.get();
    }
}
