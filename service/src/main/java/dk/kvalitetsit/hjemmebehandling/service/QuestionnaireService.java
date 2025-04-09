package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
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

public class QuestionnaireService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireService.class);

    private final FhirClient fhirClient;

    private final FhirMapper fhirMapper;

    public QuestionnaireService(FhirClient fhirClient, FhirMapper fhirMapper, AccessValidator accessValidator) {
        super(accessValidator);
        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
    }

    public Optional<QuestionnaireModel> getQuestionnaireById(String questionnaireId) throws ServiceException, AccessValidationException {
        FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(List.of(questionnaireId));

        Optional<Questionnaire> questionnaire = lookupResult.getQuestionnaire(questionnaireId);
        if (questionnaire.isEmpty()) {
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

        return lookupResult.getQuestionnaires().stream().map(fhirMapper::mapQuestionnaire).toList();
    }

    public QuestionnaireModel createQuestionnaire(QuestionnaireModel questionnaire) throws ServiceException {
        // Initialize basic attributes for a new CarePlan: Id, status and so on.
        var initializedQuestionnaire = initializeAttributesForNewQuestionnaire(questionnaire);
        var mappedQuestionnaire = fhirMapper.mapQuestionnaireModel(initializedQuestionnaire);

        var savedQuestionnaire = fhirClient.save(mappedQuestionnaire);
        return fhirMapper.mapQuestionnaire(savedQuestionnaire);
    }

    private QuestionnaireModel initializeAttributesForNewQuestionnaire(QuestionnaireModel questionnaire) {
        // Ensure that no id is present on the questionnaire - the FHIR server will generate that for us.

        var updatedQuestions = Optional.ofNullable(questionnaire.questions()).map(questions -> questions
                .stream()
                .map(q -> Optional.ofNullable(q.linkId())
                        .map(x -> q)
                        .orElse(QuestionModel.Builder
                                .from(q)
                                .linkId(IdType.newRandomUuid().getValueAsString())
                                .build()
                        )
                ).toList()).orElse(null);

        var callToAction = Optional.ofNullable(questionnaire.callToAction()).map(x -> QuestionModel.Builder.from(x)
                .linkId(Systems.CALL_TO_ACTION_LINK_ID)
                .build()
        ).orElse(null);


        return QuestionnaireModel.builder()
                .id(null)
                .questions(updatedQuestions)
                .callToAction(callToAction)
                .build();

    }


    public void updateQuestionnaire(String questionnaireId, String updatedTitle, String updatedDescription, String updatedStatus, List<QuestionModel> updatedQuestions, QuestionModel updatedCallToAction) throws ServiceException, AccessValidationException {

        // Look up the Questionnaire, throw an exception in case it does not exist.
        FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(List.of(questionnaireId));
        if (lookupResult.getQuestionnaires().size() != 1 || lookupResult.getQuestionnaire(questionnaireId).isEmpty()) {
            throw new ServiceException(String.format("Could not lookup questionnaire with id %s!", questionnaireId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }
        Questionnaire questionnaire = lookupResult.getQuestionnaire(questionnaireId).get();

        // Validate that the client is allowed to update the questionnaire.
        validateAccess(questionnaire);

        // Validate that status change is legal
        validateStatusChangeIsLegal(questionnaire, updatedStatus);

        // Update questionnaire
        var questionnaireModel = fhirMapper.mapQuestionnaire(questionnaire);
        var updateQuestionnaireModel = updateQuestionnaireModel(questionnaireModel, updatedTitle, updatedDescription, updatedStatus, updatedQuestions, updatedCallToAction);

        // Save the updated Questionnaire
        fhirClient.update(fhirMapper.mapQuestionnaireModel(updateQuestionnaireModel));
    }

    private QuestionnaireModel updateQuestionnaireModel(
            QuestionnaireModel questionnaireModel,
            String updatedTitle,
            String updatedDescription,
            String updatedStatus,
            List<QuestionModel> updatedQuestions,
            QuestionModel updatedCallToAction
    ) {
        // Ensure all questions have a unique ID
        List<QuestionModel> processedQuestions = updatedQuestions.stream()
                .map(q -> q.linkId() == null
                                ? new QuestionModel(
                                IdType.newRandomUuid().getValueAsString(), q.text(), q.abbreviation(), q.helperText(),
                                q.required(), q.questionType(), q.measurementType(), q.options(),
                                q.enableWhens(), q.thresholds(), q.subQuestions(), q.deprecated()
                        ) : q
                ).toList();

        // Ensure call-to-action has a unique ID
        QuestionModel processedCallToAction = updatedCallToAction.linkId() == null
                ? new QuestionModel(
                Systems.CALL_TO_ACTION_LINK_ID, updatedCallToAction.text(), updatedCallToAction.abbreviation(),
                updatedCallToAction.helperText(), updatedCallToAction.required(), updatedCallToAction.questionType(),
                updatedCallToAction.measurementType(), updatedCallToAction.options(), updatedCallToAction.enableWhens(),
                updatedCallToAction.thresholds(), updatedCallToAction.subQuestions(), updatedCallToAction.deprecated()
        ) : updatedCallToAction;


        return QuestionnaireModel.from(questionnaireModel)
                .title(updatedTitle)
                .description(updatedDescription)
                .status(QuestionnaireStatus.valueOf(updatedStatus))
                .questions(processedQuestions)
                .callToAction(processedCallToAction)
                .build();
    }

    private void validateStatusChangeIsLegal(Questionnaire questionnaire, String updatedStatus) throws ServiceException {
        List<Enumerations.PublicationStatus> validStatuses = switch (questionnaire.getStatus()) {
            case ACTIVE -> List.of(Enumerations.PublicationStatus.ACTIVE, Enumerations.PublicationStatus.RETIRED);
            case DRAFT -> List.of(Enumerations.PublicationStatus.DRAFT, Enumerations.PublicationStatus.ACTIVE);
            default -> List.of();
        };

        if (!validStatuses.contains(Enumerations.PublicationStatus.valueOf(updatedStatus))) {
            throw new ServiceException(String.format("Could not change status for questionnaire with id %s!", questionnaire.getId()), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }
    }

    public void retireQuestionnaire(String id) throws ServiceException {
        String qualifiedId = FhirUtils.qualifyId(id, ResourceType.Questionnaire);
        FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(List.of(qualifiedId));

        Optional<Questionnaire> questionnaire = lookupResult.getQuestionnaire(qualifiedId);
        if (questionnaire.isEmpty()) {
            throw new ServiceException(String.format("Could not lookup questionnaire with id %s!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }

        var activeCarePlansWithQuestionnaire = fhirClient.lookupActiveCarePlansWithQuestionnaire(qualifiedId).getCarePlans();
        if (!activeCarePlansWithQuestionnaire.isEmpty()) {
            throw new ServiceException(String.format("Questionnaire with id %s if used by active careplans!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN);
        }

        Questionnaire retiredQuestionnaire = questionnaire.get().setStatus(Enumerations.PublicationStatus.RETIRED);
        fhirClient.update(retiredQuestionnaire);
    }


    public List<PlanDefinition> getPlanDefinitionsThatIncludes(String questionnaireId) throws ServiceException {
        String qualifiedId = FhirUtils.qualifyId(questionnaireId, ResourceType.Questionnaire);

        FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(List.of(qualifiedId));

        if (lookupResult.getQuestionnaires().isEmpty()) {
            throw new ServiceException(String.format("Could not find questionnaires with tht requested id: %s", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }

        lookupResult.merge(fhirClient.lookupActivePlanDefinitionsUsingQuestionnaireWithId(qualifiedId));
        return lookupResult.getPlanDefinitions();
    }
}
