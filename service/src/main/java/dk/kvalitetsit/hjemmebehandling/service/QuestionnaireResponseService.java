package dk.kvalitetsit.hjemmebehandling.service;


import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.client.Client;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.ResourceType;
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
            Client<CarePlanModel, PlanDefinitionModel, PractitionerModel, PatientModel, QuestionnaireModel, QuestionnaireResponseModel, Organization, CarePlanStatus> fhirClient,
            Comparator<QuestionnaireResponseModel> priorityComparator,
            AccessValidator accessValidator
    ) {
        super(accessValidator);

        this.fhirClient = fhirClient;
        this.priorityComparator = priorityComparator;

    }


    private static List<QuestionnaireResponseModel> pageResponses(List<QuestionnaireResponseModel> responses, Pagination pagination) {
        return responses
                .stream()
                .skip((long) (pagination.offset() - 1) * pagination.limit())
                .limit(pagination.limit())
                .toList();
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponses(String carePlanId, List<String> questionnaireIds) throws ServiceException, AccessValidationException {
        List<QuestionnaireModel> historicalQuestionnaires = fhirClient.lookupVersionsOfQuestionnaireById(questionnaireIds);

        // Validate that the user is allowed to retrieve the QuestionnaireResponses.
        //validateAccess(responses);

        var orgId = fhirClient.getOrganizationId();

        return fhirClient.lookupQuestionnaireResponses(carePlanId, questionnaireIds)
                .stream()
//                .sorted((a, b) -> b.getAuthored().compareTo(a.getAuthored())) // Sort the responses by priority.
                .toList();
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponses(String carePlanId, List<String> questionnaireIds, Pagination pagination) throws ServiceException, AccessValidationException {
        return pageResponses(this.getQuestionnaireResponses(carePlanId, questionnaireIds), pagination);
    }



    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses, Pagination pagination) throws ServiceException, AccessValidationException {
        var responses = getQuestionnaireResponsesByStatus(statuses);
        return pageResponses(responses, pagination);
    }

    public QuestionnaireResponseModel updateExaminationStatus(String questionnaireResponseId, ExaminationStatus examinationStatus) throws ServiceException, AccessValidationException {
        // Look up the QuestionnaireResponse
        String qualifiedId = FhirUtils.qualifyId(questionnaireResponseId, ResourceType.QuestionnaireResponse);

        var questionnaireResponse = fhirClient.lookupQuestionnaireResponseById(questionnaireResponseId).orElseThrow(() -> new ServiceException(String.format("Could not look up QuestionnaireResponse by id %s!", questionnaireResponseId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_RESPONSE_DOES_NOT_EXIST));

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

    private QuestionnaireResponseModel extractMaximalPriorityResponse(List<QuestionnaireResponseModel> responses) {
        return responses
                .stream()
                .min(priorityComparator).orElseThrow(() -> new IllegalStateException("Could not extract QuestionnaireResponse of maximal priority - the list was empty!"));

    }


    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException, AccessValidationException {


        List<QuestionnaireResponseModel> responses = fhirClient.lookupQuestionnaireResponsesByStatus(statuses);


        // below is supposed to return a list of ids in the following format: "questionnaire-infektionsmedicinsk-1"
        List<String> ids = responses
                .stream()
                .map(questionnaire -> questionnaire.id().toString())
                .toList();

        List<QuestionnaireModel> historicalQuestionnaires = fhirClient.lookupVersionsOfQuestionnaireById(ids);

        var orgId = fhirClient.getOrganizationId();



        // Filter the responses: We want only one response per <patientId, questionnaireId>-pair,
        // and in case of multiple entries, we want the 'most important' one.
        // Grouping, ordering and pagination should ideally happen in the FHIR-server, but the grouping part seems to
        // require a server extension. So for now, we do it here.
        return responses
                .stream()
                .collect(Collectors.groupingBy(r -> new ImmutablePair<>(r.id(), r.questionnaireId())))
                .values()
                .stream()
                .map(this::extractMaximalPriorityResponse)
                .sorted(priorityComparator)
                .toList();

    }



}
