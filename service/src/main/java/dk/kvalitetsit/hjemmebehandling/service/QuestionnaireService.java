package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.api.PlanDefinitionDto;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class QuestionnaireService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    public QuestionnaireService(FhirClient fhirClient, FhirMapper fhirMapper, AccessValidator accessValidator) {
        super(accessValidator);
        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
    }

    public Optional<QuestionnaireModel> getQuestionnaireById(String questionnaireId) throws ServiceException, AccessValidationException {
        FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(List.of(questionnaireId));

        Optional<Questionnaire> questionnaire = lookupResult.getQuestionnaire(questionnaireId);
        if(!questionnaire.isPresent()) {
            return Optional.empty();
        }

        // Validate that the user is allowed to access the careplan.
        validateAccess(questionnaire.get());

        // Map the resource
        QuestionnaireModel mappedCarePlan = fhirMapper.mapQuestionnaire(questionnaire.get());
        return Optional.of(mappedCarePlan);
    }

    public List<QuestionnaireModel> getQuestionnaires(Collection<String> statusesToInclude) throws ServiceException {
        FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesByStatus(statusesToInclude);

        return lookupResult.getQuestionnaires().stream().map(q -> fhirMapper.mapQuestionnaire(q)).collect(Collectors.toList());
    }

    public String createQuestionnaire(QuestionnaireModel questionnaire) throws ServiceException {
        // Initialize basic attributes for a new CarePlan: Id, status and so on.
        initializeAttributesForNewQuestionnaire(questionnaire);

        return fhirClient.saveQuestionnaire(fhirMapper.mapQuestionnaireModel(questionnaire));
    }

    private void initializeAttributesForNewQuestionnaire(QuestionnaireModel questionnaire) {
        // Ensure that no id is present on the careplan - the FHIR server will generate that for us.
        questionnaire.setId(null);

        //questionnaire.setStatus(QuestionnaireStatus.DRAFT);

        // add unique id to question(s) and call-to-action.
        if (questionnaire.getQuestions() != null) {
            questionnaire.getQuestions().stream().filter(q -> q.getLinkId()==null).forEach(q -> q.setLinkId(IdType.newRandomUuid().getValueAsString()));
        }
        if (questionnaire.getCallToAction() != null) {
            questionnaire.getCallToAction().setLinkId(Systems.CALL_TO_ACTION_LINK_ID);
        }
    }

    public void updateQuestionnaire(String questionnaireId, String updatedTitle, String updatedDescription, String updatedStatus, List<QuestionModel> updatedQuestions, QuestionModel updatedCallToAction) throws ServiceException, AccessValidationException {

        // Look up the Questionnaire, throw an exception in case it does not exist.
        FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(List.of(questionnaireId));
        if(lookupResult.getQuestionnaires().size() != 1 || !lookupResult.getQuestionnaire(questionnaireId).isPresent()) {
            throw new ServiceException(String.format("Could not lookup questionnaire with id %s!", questionnaireId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }
        Questionnaire questionnaire = lookupResult.getQuestionnaire(questionnaireId).get();

        // Validate that the client is allowed to update the questionnaire.
        validateAccess(questionnaire);

        // Validate that status change is legal
        validateStatusChangeIsLegal(questionnaire, updatedStatus);

        // Update questionnaire
        QuestionnaireModel questionnaireModel = fhirMapper.mapQuestionnaire(questionnaire);
        updateQuestionnaireModel(questionnaireModel, updatedTitle, updatedDescription, updatedStatus, updatedQuestions, updatedCallToAction);

        // Save the updated Questionnaire
        fhirClient.updateQuestionnaire(fhirMapper.mapQuestionnaireModel(questionnaireModel));
    }

    private void updateQuestionnaireModel(QuestionnaireModel questionnaireModel, String updatedTitle, String updatedDescription, String updatedStatus, List<QuestionModel> updatedQuestions, QuestionModel updatedCallToAction) {
        // make sure all question(s) and call-to-action has a unique id
        updatedQuestions.stream().filter(q -> q.getLinkId()==null).forEach(q -> q.setLinkId(IdType.newRandomUuid().getValueAsString()));
        if (updatedCallToAction.getLinkId() == null) {
            updatedCallToAction.setLinkId(Systems.CALL_TO_ACTION_LINK_ID);
        }

        questionnaireModel.setTitle(updatedTitle);
        questionnaireModel.setDescription(updatedDescription);
        questionnaireModel.setStatus(QuestionnaireStatus.valueOf(updatedStatus));
        questionnaireModel.setQuestions(updatedQuestions);
        questionnaireModel.setCallToAction(updatedCallToAction);
    }

    private void validateStatusChangeIsLegal(Questionnaire questionnaire, String updatedStatus) throws ServiceException {
        List<Enumerations.PublicationStatus> validStatuses;
        switch (questionnaire.getStatus()) {
            case ACTIVE:
                validStatuses = List.of(Enumerations.PublicationStatus.ACTIVE, Enumerations.PublicationStatus.RETIRED);
                break;
            case DRAFT:
                validStatuses = List.of(Enumerations.PublicationStatus.DRAFT, Enumerations.PublicationStatus.ACTIVE);
                break;
            default:
                validStatuses = List.of();
                break;
        }

        if (!validStatuses.contains(Enumerations.PublicationStatus.valueOf(updatedStatus))) {
            throw new ServiceException(String.format("Could not change status for questionnaire with id %s!", questionnaire.getId()), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }
    }

    public void retireQuestionnaire(String id) throws ServiceException {
        String qualifiedId = FhirUtils.qualifyId(id, ResourceType.Questionnaire);
        FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(List.of(qualifiedId));

        Optional<Questionnaire> questionnaire = lookupResult.getQuestionnaire(qualifiedId);
        if (!questionnaire.isPresent()) {
            throw new ServiceException(String.format("Could not lookup questionnaire with id %s!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }

        var activeCarePlansWithQuestionnaire = fhirClient.lookupActiveCarePlansWithQuestionnaire(qualifiedId).getCarePlans();
        if (!activeCarePlansWithQuestionnaire.isEmpty()) {
            throw new ServiceException(String.format("Questionnaire with id %s if used by active careplans!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN);
        }

        Questionnaire retiredQuestionnaire = questionnaire.get().setStatus(Enumerations.PublicationStatus.RETIRED);
        fhirClient.updateQuestionnaire(retiredQuestionnaire);
    }


    public List<PlanDefinition> getPlanDefinitionsThatIncludes(String questionnaireId) throws ServiceException {
        String qualifiedId = FhirUtils.qualifyId(questionnaireId, ResourceType.Questionnaire);

        FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(List.of(qualifiedId));

        if(lookupResult.getQuestionnaires().isEmpty()) {
            throw new ServiceException(String.format("Could not find questionnaires with tht requested id: %s", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }

        lookupResult.merge(fhirClient.lookupActivePlanDefinitionsUsingQuestionnaireWithId(qualifiedId));
        return lookupResult.getPlanDefinitions();
}
}
