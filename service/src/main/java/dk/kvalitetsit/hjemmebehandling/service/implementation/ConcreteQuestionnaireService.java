package dk.kvalitetsit.hjemmebehandling.service.implementation;

import dk.kvalitetsit.hjemmebehandling.model.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.model.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.repository.CarePlanRepository;
import dk.kvalitetsit.hjemmebehandling.repository.PlanDefinitionRepository;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static dk.kvalitetsit.hjemmebehandling.model.constants.QuestionnaireStatus.RETIRED;

public class ConcreteQuestionnaireService implements QuestionnaireService {
    private static final Logger logger = LoggerFactory.getLogger(ConcreteQuestionnaireService.class);

    private final QuestionnaireRepository<QuestionnaireModel> questionnaireRepository;
    private final CarePlanRepository<CarePlanModel, PatientModel> careplanRepository;
    private final PlanDefinitionRepository<PlanDefinitionModel> plandefinitionRepository;

    public ConcreteQuestionnaireService(QuestionnaireRepository<QuestionnaireModel> questionnaireRepository, CarePlanRepository<CarePlanModel, PatientModel> careplanRepository, PlanDefinitionRepository<PlanDefinitionModel> plandefinitionRepository) {
        this.questionnaireRepository = questionnaireRepository;
        this.careplanRepository = careplanRepository;
        this.plandefinitionRepository = plandefinitionRepository;
    }

    public Optional<QuestionnaireModel> getQuestionnaireById(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException, AccessValidationException {
        return questionnaireRepository.fetch(questionnaireId);
        // Validate that the user is allowed to access the careplan.
        //validateAccess(questionnaire.get());
    }

    public List<QuestionnaireModel> getQuestionnaires(Collection<String> statusesToInclude) throws ServiceException {
        return questionnaireRepository.lookupQuestionnairesByStatus(statusesToInclude);
    }

    public QualifiedId.QuestionnaireId createQuestionnaire(QuestionnaireModel questionnaire) throws ServiceException {
        // Initialize basic attributes for a new CarePlan: Id, status and so on.
        var initializedQuestionnaire = initializeAttributesForNewQuestionnaire(questionnaire);
        return questionnaireRepository.save(initializedQuestionnaire);
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


    public void updateQuestionnaire(QualifiedId.QuestionnaireId questionnaireId, String updatedTitle, String updatedDescription, String updatedStatus, List<QuestionModel> updatedQuestions, QuestionModel updatedCallToAction) throws ServiceException, AccessValidationException {

        // Look up the Questionnaire, throw an exception in case it does not exist.
        Optional<QuestionnaireModel> result = questionnaireRepository.fetch(questionnaireId);

        if (result.isEmpty()) {
            throw new ServiceException(String.format("Could not lookup questionnaire with id %s!", questionnaireId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }
        QuestionnaireModel questionnaire = result.get();

        // Validate that the client is allowed to update the questionnaire.
        //validateAccess(questionnaire);

        // Validate that status change is legal
        //validateStatusChangeIsLegal(questionnaire, updatedStatus);


        var updateQuestionnaireModel = updateQuestionnaireModel(questionnaire, updatedTitle, updatedDescription, updatedStatus, updatedQuestions, updatedCallToAction);

        // Save the updated Questionnaire
        questionnaireRepository.update(updateQuestionnaireModel);
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


        return QuestionnaireModel.Builder
                .from(questionnaireModel)
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

    public void retireQuestionnaire(QualifiedId.QuestionnaireId id) throws ServiceException {

        Optional<QuestionnaireModel> questionnaire = questionnaireRepository.fetch(id);

        if (questionnaire.isEmpty()) {
            throw new ServiceException(String.format("Could not lookup questionnaire with id %s!", id), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }

        var activeCarePlansWithQuestionnaire = careplanRepository.fetchActiveCarePlansWithQuestionnaire(id);
        if (!activeCarePlansWithQuestionnaire.isEmpty()) {
            throw new ServiceException(String.format("Questionnaire with id %s if used by active careplans!", id), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN);
        }

        QuestionnaireModel retiredQuestionnaire = QuestionnaireModel.Builder
                .from(questionnaire.get())
                .status(RETIRED)
                .build();

        questionnaireRepository.update(retiredQuestionnaire);
    }


    public List<PlanDefinitionModel> getPlanDefinitionsThatIncludes(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException {
        Optional<QuestionnaireModel> result = questionnaireRepository.fetch(questionnaireId);
        if (result.isEmpty()) {
            throw new ServiceException(String.format("Could not find questionnaires with tht requested id: %s", questionnaireId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }
        return plandefinitionRepository.fetchActivePlanDefinitionsUsingQuestionnaireWithId(questionnaireId);
    }
}
