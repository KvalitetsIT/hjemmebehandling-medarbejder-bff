package dk.kvalitetsit.hjemmebehandling.service;


import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.fhir.repository.OrganizationRepository;
import dk.kvalitetsit.hjemmebehandling.fhir.repository.PractitionerRepository;
import dk.kvalitetsit.hjemmebehandling.fhir.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.fhir.repository.QuestionnaireResponseRepository;
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

    private final QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository;
    private final QuestionnaireRepository<QuestionnaireModel> questionnaireRepository;
    private final PractitionerRepository<PractitionerModel> practitionerRepository;
    private final OrganizationRepository<Organization> organizationRepository;

    private final Comparator<QuestionnaireResponseModel> priorityComparator;

    public QuestionnaireResponseService(Comparator<QuestionnaireResponseModel> priorityComparator,
                                        AccessValidator accessValidator,
                                        QuestionnaireRepository<QuestionnaireModel> questionnaireRepository,
                                        QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository,
                                        PractitionerRepository<PractitionerModel> practitionerRepository, OrganizationRepository<Organization> organizationRepository
    ) {
        super(accessValidator);
        this.priorityComparator = priorityComparator;
        this.questionnaireResponseRepository = questionnaireResponseRepository;
        this.questionnaireRepository = questionnaireRepository;
        this.practitionerRepository = practitionerRepository;
        this.organizationRepository = organizationRepository;
    }


    private static List<QuestionnaireResponseModel> pageResponses(List<QuestionnaireResponseModel> responses, Pagination pagination) {
        return responses
                .stream()
                .skip((long) (pagination.offset() - 1) * pagination.limit())
                .limit(pagination.limit())
                .toList();
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponses(String carePlanId, List<String> questionnaireIds) throws ServiceException, AccessValidationException {

        List<QualifiedId> qualifiedQuestionnaireIds = questionnaireIds.stream().map(x -> new QualifiedId(x, ResourceType.Questionnaire)).toList();
        QualifiedId qualifiedCarePlanId = new QualifiedId(carePlanId, ResourceType.CarePlan);

        List<QuestionnaireModel> historicalQuestionnaires = questionnaireRepository.lookupVersionsOfQuestionnaireById(qualifiedQuestionnaireIds);

        // Validate that the user is allowed to retrieve the QuestionnaireResponses.
        //validateAccess(responses);

        var orgId = organizationRepository.getOrganizationId();

        return questionnaireResponseRepository.fetch(qualifiedCarePlanId, qualifiedQuestionnaireIds)
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
        QualifiedId qualifiedId = new QualifiedId( questionnaireResponseId, ResourceType.QuestionnaireResponse);

        var questionnaireResponse = questionnaireResponseRepository.fetch(qualifiedId).orElseThrow(() -> new ServiceException(String.format("Could not look up QuestionnaireResponse by id %s!", questionnaireResponseId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_RESPONSE_DOES_NOT_EXIST));

        // Validate that the user is allowed to update the QuestionnaireResponse.
        //validateAccess(questionnaireResponse);

        var orgId = organizationRepository.getOrganizationId();

        PractitionerModel user = practitionerRepository.getOrCreateUserAsPractitioner();

        QuestionnaireResponseModel mappedResponse = QuestionnaireResponseModel.Builder
                .from(questionnaireResponse)
                .examinationStatus(examinationStatus)
                .examinationAuthor(user)
                .build();

        // Save the updated QuestionnaireResponse
        questionnaireResponseRepository.update(mappedResponse);
        return mappedResponse;
    }

    private QuestionnaireResponseModel extractMaximalPriorityResponse(List<QuestionnaireResponseModel> responses) {
        return responses
                .stream()
                .min(priorityComparator).orElseThrow(() -> new IllegalStateException("Could not extract QuestionnaireResponse of maximal priority - the list was empty!"));
    }


    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException, AccessValidationException {

        List<QuestionnaireResponseModel> responses = questionnaireResponseRepository.fetchByStatus(statuses);

        // below is supposed to return a list of ids in the following format: "questionnaire-infektionsmedicinsk-1"
        List<QualifiedId> ids = responses
                .stream()
                .map(QuestionnaireResponseModel::id)
                .toList();

        List<QuestionnaireModel> historicalQuestionnaires = questionnaireRepository.lookupVersionsOfQuestionnaireById(ids);

        var orgId = organizationRepository.getOrganizationId();


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
