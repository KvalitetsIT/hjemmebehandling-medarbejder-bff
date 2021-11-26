package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.fhir.*;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class QuestionnaireResponseService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    private Comparator<QuestionnaireResponse> priorityComparator;

    public QuestionnaireResponseService(FhirClient fhirClient, FhirMapper fhirMapper, Comparator<QuestionnaireResponse> priorityComparator, AccessValidator accessValidator) {
        super(accessValidator);

        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.priorityComparator = priorityComparator;
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponses(String carePlanId, List<String> questionnaireIds) throws ServiceException, AccessValidationException {
        FhirLookupResult lookupResult = fhirClient.lookupQuestionnaireResponses(carePlanId, questionnaireIds);
        List<QuestionnaireResponse> responses = lookupResult.getQuestionnaireResponses();
        if(responses.isEmpty()) {
            return List.of();
        }

        // Validate that the user is allowed to retrieve the QuestionnaireResponses.
        validateAccess(responses);

        // Map and return the responses
        return responses
                .stream()
                .map(qr -> fhirMapper.mapQuestionnaireResponse(qr, lookupResult))
                .collect(Collectors.toList());
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException {
        return getQuestionnaireResponsesByStatus(statuses, null);
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses, PageDetails pageDetails) throws ServiceException {
        // Get the questionnaires by status
        FhirLookupResult lookupResult = fhirClient.lookupQuestionnaireResponsesByStatus(statuses);
        List<QuestionnaireResponse> responses = lookupResult.getQuestionnaireResponses();
        if(responses.isEmpty()) {
            return List.of();
        }

        // Filter the responses: We want only one response per <patientId, questionnaireId>-pair,
        // and in case of multiple entries, we want the 'most important' one.
        // Grouping, ordering and pagination should ideally happen in the FHIR-server, but the grouping part seems to
        // require a server extension. So for now, we do it here.
        responses = filterResponses(responses);

        // Sort the responses by priority.
        responses = sortResponses(responses);

        // Perform paging if required.
        if(pageDetails != null) {
            responses = pageResponses(responses, pageDetails);
        }

        // Map and return the responses
        return responses
                .stream()
                .map(qr -> fhirMapper.mapQuestionnaireResponse(qr, lookupResult))
                .collect(Collectors.toList());
    }

    public void updateExaminationStatus(String questionnaireResponseId, ExaminationStatus examinationStatus) throws ServiceException, AccessValidationException {
        // Look up the QuestionnaireResponse
        FhirLookupResult lookupResult = fhirClient.lookupQuestionnaireResponseById(questionnaireResponseId);
        QuestionnaireResponse questionnaireResponse = lookupResult.getQuestionnaireResponse(FhirUtils.qualifyId(questionnaireResponseId, ResourceType.QuestionnaireResponse))
                .orElseThrow(() -> new ServiceException(String.format("Could not look up QuestionnaireResponse by id %s!", questionnaireResponseId), ErrorKind.BAD_REQUEST));

        // Validate that the user is allowed to update the QuestionnaireResponse.
        validateAccess(questionnaireResponse);

        // Update the Questionnaireresponse
        QuestionnaireResponseModel mappedResponse = fhirMapper.mapQuestionnaireResponse(questionnaireResponse, lookupResult);
        mappedResponse.setExaminationStatus(examinationStatus);

        // Save the updated QuestionnaireResponse
        fhirClient.updateQuestionnaireResponse(fhirMapper.mapQuestionnaireResponseModel(mappedResponse));
    }

    private List<QuestionnaireResponse> filterResponses(List<QuestionnaireResponse> responses) {
        // Given the list of responses, ensure that only one QuestionnaireResponse exists for each <patientId, questionnaireId>-pair,
        // and in case of duplicates, the one with the highest priority is retained.

        // Group the responses by  <patientId, questionnaireId>.
        var groupedResponses = responses
                .stream()
                .collect(Collectors.groupingBy(r -> new ImmutablePair<>(r.getSubject().getReference(), r.getQuestionnaire())));

        // For each of the pairs, retain only the response with maximal priority.
        return groupedResponses.values()
                .stream()
                .map(rs -> extractMaximalPriorityResponse(rs))
                .collect(Collectors.toList());
    }

    private List<QuestionnaireResponse> sortResponses(List<QuestionnaireResponse> responses) {
        return responses
                .stream()
                .sorted(priorityComparator)
                .collect(Collectors.toList());
    }

    private List<QuestionnaireResponse> pageResponses(List<QuestionnaireResponse> responses, PageDetails pageDetails) {
        return responses
                .stream()
                .skip((pageDetails.getPageNumber() - 1) * pageDetails.getPageSize())
                .limit(pageDetails.getPageSize())
                .collect(Collectors.toList());
    }

    private QuestionnaireResponse extractMaximalPriorityResponse(List<QuestionnaireResponse> responses) {
        var response = responses
                .stream()
                .max(priorityComparator);
        if(!response.isPresent()) {
            throw new IllegalStateException("Could not extract QuestionnaireResponse of maximal priority - the list was empty!");
        }
        return response.get();
    }
}
