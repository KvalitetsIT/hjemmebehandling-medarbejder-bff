package dk.kvalitetsit.hjemmebehandling.service.implementation;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.model.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.repository.CarePlanRepository;
import dk.kvalitetsit.hjemmebehandling.repository.PlanDefinitionRepository;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.IdType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static dk.kvalitetsit.hjemmebehandling.model.constants.Status.RETIRED;

public class ConcreteQuestionnaireService implements QuestionnaireService {
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
    }

    public List<QuestionnaireModel> getQuestionnaires(Collection<String> statusesToInclude) throws ServiceException, AccessValidationException {
        return questionnaireRepository.fetch(statusesToInclude);
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
                .questions(updatedQuestions)
                .callToAction(callToAction)
                .build();
    }

    @Override
    public void updateQuestionnaire(
            QualifiedId.QuestionnaireId questionnaireId, String updatedTitle, String updatedDescription, Status updatedStatus, List<QuestionModel> updatedQuestions, QuestionModel updatedCallToAction)
            throws ServiceException, AccessValidationException {

        // Look up the Questionnaire, throw an exception in case it does not exist.
        QuestionnaireModel questionnaire = questionnaireRepository.fetch(questionnaireId).orElseThrow(() -> new ServiceException(
                String.format("Could not lookup questionnaire with id %s!", questionnaireId),
                ErrorKind.BAD_REQUEST,
                ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST
        ));


        // Validate that status change is legal
        validateStatusChangeIsLegal(questionnaire, updatedStatus);

        // Ensure all questions have a unique ID
        List<QuestionModel> processedQuestions = Optional.ofNullable(updatedQuestions)
                .map(x -> x.stream().map(q -> q.linkId() == null ? QuestionModel.Builder.from(q).linkId(IdType.newRandomUuid().getValueAsString()).build() : q).toList())
                .orElse(List.of());

        // Ensure call-to-action has a unique ID
        QuestionModel processedCallToAction = updatedCallToAction.linkId() == null ? QuestionModel.Builder.from(updatedCallToAction).linkId(Systems.CALL_TO_ACTION_LINK_ID).build() : updatedCallToAction;

        var updateQuestionnaireModel = QuestionnaireModel.Builder
                .from(questionnaire)
                .title(updatedTitle)
                .description(updatedDescription)
                .status(updatedStatus)
                .questions(processedQuestions)
                .callToAction(processedCallToAction)
                .build();

        // Save the updated Questionnaire
        questionnaireRepository.update(updateQuestionnaireModel);
    }


    private void validateStatusChangeIsLegal(QuestionnaireModel questionnaire, Status updatedStatus) throws ServiceException {
        List<Status> validStatuses = switch (questionnaire.status()) {
            case ACTIVE -> List.of(Status.ACTIVE, Status.RETIRED);
            case DRAFT -> List.of(Status.DRAFT, Status.ACTIVE);
            default -> List.of();
        };

        if (!validStatuses.contains(updatedStatus)) {
            throw new ServiceException(String.format("Could not change status for questionnaire with id %s!", questionnaire.id()), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }
    }

    public void retireQuestionnaire(QualifiedId.QuestionnaireId id) throws ServiceException, AccessValidationException {

        Optional<QuestionnaireModel> questionnaire = questionnaireRepository.fetch(id);

        if (questionnaire.isEmpty()) {
            throw new ServiceException(String.format("Could not lookup questionnaire with id %s!", id), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }

        var activeCarePlansWithQuestionnaire = careplanRepository.fetchActiveCarePlansByQuestionnaireId(id);
        if (!activeCarePlansWithQuestionnaire.isEmpty()) {
            throw new ServiceException(String.format("Questionnaire with id %s if used by active careplans!", id), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN);
        }

        QuestionnaireModel retiredQuestionnaire = QuestionnaireModel.Builder
                .from(questionnaire.get())
                .status(RETIRED)
                .build();

        questionnaireRepository.update(retiredQuestionnaire);
    }


    public List<PlanDefinitionModel> getPlanDefinitionsThatIncludes(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException, AccessValidationException {
        Optional<QuestionnaireModel> result = questionnaireRepository.fetch(questionnaireId);
        if (result.isEmpty()) {
            throw new ServiceException(String.format("Could not find questionnaires with tht requested id: %s", questionnaireId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_DOES_NOT_EXIST);
        }
        return plandefinitionRepository.fetchActivePlanDefinitionsUsingQuestionnaireWithId(questionnaireId);
    }
}
