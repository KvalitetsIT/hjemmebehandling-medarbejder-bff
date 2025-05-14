package dk.kvalitetsit.hjemmebehandling.service.implementation;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.repository.OrganizationRepository;
import dk.kvalitetsit.hjemmebehandling.repository.PractitionerRepository;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireResponseRepository;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.r4.model.Organization;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ConcreteQuestionnaireResponseService implements QuestionnaireResponseService {

    private final QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository;
    private final QuestionnaireRepository<QuestionnaireModel> questionnaireRepository;
    private final PractitionerRepository<PractitionerModel> practitionerRepository;
    private final OrganizationRepository<Organization> organizationRepository;

    private final Comparator<QuestionnaireResponseModel> priorityComparator;

    public ConcreteQuestionnaireResponseService(Comparator<QuestionnaireResponseModel> priorityComparator,
                                                QuestionnaireRepository<QuestionnaireModel> questionnaireRepository,
                                                QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository,
                                                PractitionerRepository<PractitionerModel> practitionerRepository, OrganizationRepository<Organization> organizationRepository
    ) {

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

    public List<QuestionnaireResponseModel> getQuestionnaireResponses(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds) throws ServiceException, AccessValidationException {

        List<QuestionnaireModel> historicalQuestionnaires = questionnaireRepository.history(questionnaireIds);

        return questionnaireResponseRepository.fetch(carePlanId, questionnaireIds)
                .stream()
                // Todo: The sorting below could probably be moved into the repository layer
                // .sorted((a, b) -> b.getAuthored().compareTo(a.getAuthored())) // Sort the responses by priority.
                .toList();
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponses(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds, Pagination pagination) throws ServiceException, AccessValidationException {
        return pageResponses(this.getQuestionnaireResponses(carePlanId, questionnaireIds), pagination);
    }


    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses, Pagination pagination) throws ServiceException, AccessValidationException {
        var responses = getQuestionnaireResponsesByStatus(statuses);
        return pageResponses(responses, pagination);
    }

    public QuestionnaireResponseModel updateExaminationStatus(QualifiedId.QuestionnaireResponseId questionnaireResponseId, ExaminationStatus examinationStatus) throws ServiceException, AccessValidationException {
        // Look up the QuestionnaireResponse
        var questionnaireResponse = questionnaireResponseRepository
                .fetch(questionnaireResponseId)
                .orElseThrow(() -> new ServiceException(String.format("Could not look up QuestionnaireResponse by id %s!", questionnaireResponseId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_RESPONSE_DOES_NOT_EXIST));

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
        List<QualifiedId.QuestionnaireId> ids = responses
                .stream()
                .map(QuestionnaireResponseModel::questionnaireId)
                .toList();

        List<QuestionnaireModel> historicalQuestionnaires = questionnaireRepository.history(ids);



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
