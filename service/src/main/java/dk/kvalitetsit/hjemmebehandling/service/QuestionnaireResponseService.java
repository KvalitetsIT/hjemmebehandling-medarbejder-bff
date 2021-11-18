package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class QuestionnaireResponseService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    private FhirObjectBuilder fhirObjectBuilder;

    private Comparator<QuestionnaireResponse> priorityComparator;

    private UserContextProvider userContextProvider;

    public QuestionnaireResponseService(FhirClient fhirClient, FhirMapper fhirMapper, FhirObjectBuilder fhirObjectBuilder, Comparator<QuestionnaireResponse> priorityComparator, UserContextProvider userContextProvider) {
        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.fhirObjectBuilder = fhirObjectBuilder;
        this.priorityComparator = priorityComparator;
        this.userContextProvider = userContextProvider;
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponses(String carePlanId, List<String> questionnaireIds) throws ServiceException {
        List<QuestionnaireResponse> responses = fhirClient.lookupQuestionnaireResponses(carePlanId, questionnaireIds);
        if(responses.isEmpty()) {
            return List.of();
        }

        // Look up questionnaires
        Map<String, Questionnaire> questionnairesById = getQuestionnairesById(questionnaireIds);

        // Look up careplan
        CarePlan carePlan = fhirClient.lookupCarePlanById(carePlanId).orElseThrow(() -> new IllegalStateException(String.format("Could not look up CarePlan for id %s!", carePlanId)));

        // Extract the patientId, get the patient
        String patientId = carePlan.getSubject().getReference();
        Patient patient = fhirClient.lookupPatientById(patientId).orElseThrow(() -> new IllegalStateException(String.format("Could not look up Patient for id %s!", patientId)));

        return constructResult(responses, questionnairesById, Map.of(patient.getIdElement().toUnqualifiedVersionless().toString(), patient));
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException {
        return getQuestionnaireResponsesByStatus(statuses, null);
    }

    public List<QuestionnaireResponseModel> getQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses, PageDetails pageDetails) throws ServiceException {
        // Get the questionnaires by status
        List<QuestionnaireResponse> responses = fhirClient.lookupQuestionnaireResponsesByStatus(statuses);
        if(responses.isEmpty()) {
            return List.of();
        }

        // Filter the responses: We want only one response per <patientId, questionnaireId>-pair,
        // and in case of multiple entries, we want the 'most important' one.
        // Grouping, ordring and pagination should ideally happen in the FHIR-server, but the grouping part seems to
        // require a server extension. So for now, we do it here.
        responses = filterResponses(responses);

        // Sort the responses by priority.
        responses = sortResponses(responses);

        // Perform paging if required.
        if(pageDetails != null) {
            responses = pageResponses(responses, pageDetails);
        }

        // Extract the questionnaireIds, get the questionnaires
        Set<String> questionnaireIds = responses.stream().map(qr -> qr.getQuestionnaire()).collect(Collectors.toSet());
        Map<String, Questionnaire> questionnairesById = getQuestionnairesById(questionnaireIds);

        // Extract the patientIds, get the patients
        Set<String> patientIds = responses.stream().map(qr -> qr.getSubject().getReference()).collect(Collectors.toSet());
        Map<String, Patient> patientsById = getPatientsById(patientIds);

        // Return the result
        return constructResult(responses, questionnairesById, patientsById);
    }

    public void updateExaminationStatus(String questionnaireResponseId, ExaminationStatus examinationStatus) throws ServiceException, AccessValidationException {
        // Look up the QuestionnaireResponse
        QuestionnaireResponse questionnaireResponse = fhirClient.lookupQuestionnaireResponseById(questionnaireResponseId)
                .orElseThrow(() -> new ServiceException(String.format("Could not look up QuestionnaireResponse by id %s!", questionnaireResponseId)));

        // Validate that the user is allowed to update the QuestionnaireResponse.
        validateAccess(questionnaireResponse);

        // Update the Questionnaireresponse
        fhirObjectBuilder.updateExaminationStatusForQuestionnaireResponse(questionnaireResponse, examinationStatus);

        // Save the updated QuestionnaireResponse
        fhirClient.updateQuestionnaireResponse(questionnaireResponse);
    }

    private void validateAccess(DomainResource resource) throws AccessValidationException {
        // Validate that the user is allowed to update the Resource.
        String userOrganizationId = getOrganizationIdForUser();
        String resourceOrganizationId = getOrganizationIdForResource(resource);

        if(!userOrganizationId.equals(resourceOrganizationId)) {
            throw new AccessValidationException(String.format("Error updating status on resource of type %s. Id was %s. User belongs to organization %s, but resource belongs to organization %s.",
                    resource.getResourceType(),
                    resource.getId(),
                    userOrganizationId,
                    resourceOrganizationId));
        }
    }

    private String getOrganizationIdForUser() {
        var context = userContextProvider.getUserContext();
        if(context == null) {
            throw new IllegalStateException("UserContext was not initialized!");
        }

        Organization organization = fhirClient.lookupOrganizationBySorCode(context.getSorCode())
                .orElseThrow(() -> new IllegalStateException(String.format("No organization was present for sorCode %s!", context.getSorCode())));

        return organization.getIdElement().toUnqualifiedVersionless().getValue();
    }

    private String getOrganizationIdForResource(DomainResource resource) {
        var extension = resource.getExtension()
                .stream()
                .filter(e -> e.getUrl().equals(Systems.ORGANIZATION) && e.getValue() instanceof Reference)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format("No organization id was present on resource %s!", resource.getId())));
        return ((Reference) extension.getValue()).getReference();
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

    private Map<String, Questionnaire> getQuestionnairesById(Collection<String> questionnaireIds) {
        Set<String> distinctIds = asUnqualifiedDistinct(questionnaireIds);

        Map<String, Questionnaire> questionnairesById = fhirClient.lookupQuestionnaires(distinctIds)
                .stream()
                .collect(Collectors.toMap(q -> q.getIdElement().toUnqualifiedVersionless().getValue(), q -> q));

        if(!distinctIds.equals(asUnqualifiedDistinct(questionnairesById.keySet()))) {
            throw new IllegalStateException("Could not look up every questionnaire when retrieving questionnaireResponses!");
        }
        return questionnairesById;
    }

    private Map<String, Patient> getPatientsById(Collection<String> patientIds) {
        Set<String> distinctIds = asUnqualifiedDistinct(patientIds);

        Map<String, Patient> patientsById = fhirClient.lookupPatientsById(distinctIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getIdElement().toUnqualifiedVersionless().toString(), p -> p));

        if(!distinctIds.equals(asUnqualifiedDistinct(patientsById.keySet()))) {
            throw new IllegalStateException("Could not look up every patient when retrieving questionnaireResponses!");
        }
        return patientsById;
    }

    private List<QuestionnaireResponseModel> constructResult(List<QuestionnaireResponse> responses, Map<String, Questionnaire> questionnairesById, Map<String, Patient> patientsById) {
        return responses
                .stream()
                .map(qr -> fhirMapper.mapQuestionnaireResponse(qr, questionnairesById.get(qr.getQuestionnaire()), patientsById.get(qr.getSubject().getReference())))
                .collect(Collectors.toList());
    }

    private Set<String> asUnqualifiedDistinct(Collection<String> ids) {
        return ids
                .stream()
                .map(id -> FhirUtils.unqualifyId(id))
                .collect(Collectors.toSet());
    }
}
