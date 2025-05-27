package dk.kvalitetsit.hjemmebehandling.service.implementation;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.*;
import dk.kvalitetsit.hjemmebehandling.service.PlanDefinitionService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class ConcretePlanDefinitionService implements PlanDefinitionService {

    private final DateProvider dateProvider;

    private final PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository;
    private final QuestionnaireRepository<QuestionnaireModel> questionnaireRepository;
    private final CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository;
    private final QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository;

    public ConcretePlanDefinitionService(
            PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository,
            DateProvider dateProvider,
            QuestionnaireRepository<QuestionnaireModel> questionnaireRepository,
            CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository,
            QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository,
            OrganizationRepository<OrganizationModel> organizationRepository
    ) {

        this.planDefinitionRepository = planDefinitionRepository;
        this.dateProvider = dateProvider;
        this.questionnaireRepository = questionnaireRepository;
        this.carePlanRepository = carePlanRepository;
        this.questionnaireResponseRepository = questionnaireResponseRepository;
    }

    @NotNull
    private static List<QuestionnaireWrapperModel> getQuestionnaireWrapperModels(List<ThresholdModel> thresholds, CarePlanModel model, List<QuestionnaireModel> newQuestionnaireModels) {
        var newQuestionnaires = newQuestionnaireModels.stream().map(qModel -> new QuestionnaireWrapperModel(
                qModel,
                new FrequencyModel(List.of(), LocalTime.parse("11:00")),
                Instant.MAX,
                thresholds // or pass new threshold list if applicable
        )).toList();

        return Stream.of(model.questionnaires(), newQuestionnaires)
                .flatMap(Collection::stream)
                .toList();
    }

    public List<PlanDefinitionModel> getPlanDefinitions(Collection<Status> statusesToInclude) throws ServiceException, AccessValidationException {
        return planDefinitionRepository.lookupPlanDefinitionsByStatus(statusesToInclude);
    }

    public QualifiedId.PlanDefinitionId createPlanDefinition(PlanDefinitionModel planDefinition) throws ServiceException, AccessValidationException {

        // Check that the referenced questionnaires and planDefinitions are valid for the client to access (and thus use).
        validateReferences(planDefinition);

        // Initialize basic attributes for a new PlanDefinition: Id, dates and so on.
        PlanDefinitionModel.Builder
                .from(planDefinition)
                .created(dateProvider.today().toInstant())
                .build();

        try {
            return planDefinitionRepository.save(planDefinition);
        } catch (Exception e) {
            throw new ServiceException("Error saving plan definition", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateReferences(PlanDefinitionModel planDefinition) throws ServiceException, AccessValidationException {
        // Validate questionnaires
        if (planDefinition.questionnaires() != null && !planDefinition.questionnaires().isEmpty()) {
            var ids = planDefinition.questionnaires().stream().map(qw -> qw.questionnaire().id()).toList();
            questionnaireRepository.fetch(ids);
        }
    }

    // TODO: Breakdown this method into multiple methods it is way too long
    // Proposition: Take a plandefinition as paramter. Substitute existing plandefintion if fields are defined
    // Limitation: If fields are to be updated to null
    // Solution:
    //         public void updatePlanDefinition(PlanDefintionModel patch ) throws ServiceException, AccessValidationException {
    //         var existingPlanDefinition = ...
    //         existingPlanDefinition.name = patch.name == null ? null : patch.name.orElse(existingPlanDefinition.name)

    public void updatePlanDefinition(
            QualifiedId.PlanDefinitionId id,
            String name,
            Status status,
            List<QualifiedId.QuestionnaireId> questionnaireIds,
            List<ThresholdModel> thresholds
    ) throws ServiceException, AccessValidationException {

        List<QuestionnaireModel> questionnaires = questionnaireRepository.fetch(questionnaireIds);

        if (questionnaires.size() != questionnaireIds.size()) throw new ServiceException(
                "Could not look up questionnaires to update",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.QUESTIONNAIRES_MISSING_FOR_CAREPLAN
        );

        PlanDefinitionModel planDefinition = planDefinitionRepository.fetch(id).orElseThrow(() -> new ServiceException(
                "Could not look up plan definitions to update",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST
        ));

        List<QualifiedId.QuestionnaireId> currentQuestionnaires = planDefinition.questionnaires().stream()
                .map(QuestionnaireWrapperModel::questionnaire)
                .map(QuestionnaireModel::id)
                .toList();

        List<QualifiedId.QuestionnaireId> removedQuestionnaireIds = currentQuestionnaires.stream()
                .filter(qid -> !questionnaireIds.contains(qid))
                .toList();

        List<QualifiedId.QuestionnaireId> newQuestionnaires = questionnaireIds.stream()
                .filter(qid -> !currentQuestionnaires.contains(qid))
                .toList();

        boolean hasRemovedQuestionnaires = !removedQuestionnaireIds.isEmpty();
        boolean hasAddedQuestionnaires = !newQuestionnaires.isEmpty();

        if (hasRemovedQuestionnaires || hasAddedQuestionnaires) {

            List<CarePlanModel> activeCarePlans = carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(id);

            for (CarePlanModel carePlan : activeCarePlans) {
                if (hasRemovedQuestionnaires) {

                    if (questionnaireHasExceededDeadline(carePlan, removedQuestionnaireIds)) throw new ServiceException(
                            String.format("CarePlan with id '%s' has missing scheduled questionnaire-responses", carePlan.id()),
                            ErrorKind.BAD_REQUEST,
                            ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES
                    );

                    if (questionnaireHasUnexaminedResponses(carePlan.id(), removedQuestionnaireIds))
                        throw new ServiceException(
                                String.format("CarePlan with id '%s' still has unhandled questionnaire-responses", carePlan.id()),
                                ErrorKind.BAD_REQUEST,
                                ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES
                        );

                    var updatedQuestionnaires = carePlan.questionnaires().stream()
                            .filter(qw -> removedQuestionnaireIds.contains(qw.questionnaire().id()))
                            .toList();

                    carePlan = CarePlanModel.Builder.from(carePlan)
                            .questionnaires(updatedQuestionnaires)
                            .build();
                }

                // if new questionnaire(s) has been added, add them to appropriate careplans with an empty schedule
                if (hasAddedQuestionnaires) {
                    boolean inUse = activeCarePlans.stream()
                            .flatMap(model -> model.questionnaires().stream())
                            .map(qw -> qw.questionnaire().id())
                            .anyMatch(newQuestionnaires::contains);

                    if (inUse) throw new ServiceException(
                            String.format("A questionnaire with id %s is used by active carePlans", newQuestionnaires),
                            ErrorKind.BAD_REQUEST,
                            ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN
                    );

                    List<QuestionnaireModel> newQuestionnaireModels = questionnaires.stream()
                            .filter(q -> newQuestionnaires.contains(q.id()))
                            .toList();

                    List<QuestionnaireWrapperModel> updatedQuestionnaires = getQuestionnaireWrapperModels(thresholds, carePlan, newQuestionnaireModels);

                    carePlan = CarePlanModel.Builder.from(carePlan)
                            .questionnaires(updatedQuestionnaires)
                            .build();
                }
                carePlanRepository.update(carePlan);
            }
        }

        List<QuestionnaireWrapperModel> updatedWrappers = questionnaires.stream()
                .map(q -> new QuestionnaireWrapperModel(q, null, null, thresholds)) // use appropriate defaults/nulls if needed
                .toList();

        planDefinition = PlanDefinitionModel.Builder.from(planDefinition)
                .name(Optional.ofNullable(name).orElse(planDefinition.name()))
                .status(Optional.ofNullable(status).orElse(planDefinition.status()))
                .lastUpdated(Instant.now())
                .questionnaires(updatedWrappers)
                .build();

        planDefinitionRepository.update(planDefinition);


    }


    public void retirePlanDefinition(QualifiedId.PlanDefinitionId id) throws ServiceException, AccessValidationException {
        Optional<PlanDefinitionModel> result = planDefinitionRepository.fetch(id);

        if (result.isEmpty()) throw new ServiceException(
                String.format("Could not lookup plan definition with id '%s'", id),
                ErrorKind.BAD_REQUEST,
                ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST
        );

        var activeCarePlansWithPlanDefinition = carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(id);

        if (!activeCarePlansWithPlanDefinition.isEmpty()) throw new ServiceException(
                String.format("Plan definition with id '%s' if used by active carePlans", id),
                ErrorKind.BAD_REQUEST,
                ErrorDetails.PLANDEFINITION_IS_IN_ACTIVE_USE_BY_CAREPLAN
        );


        PlanDefinitionModel retiredPlanDefinition = PlanDefinitionModel.Builder.from(result.get()).status(Status.RETIRED).build();
        planDefinitionRepository.update(retiredPlanDefinition);
    }


    public List<CarePlanModel> getCarePlansThatIncludes(QualifiedId.PlanDefinitionId id) throws ServiceException, AccessValidationException {
        Optional<PlanDefinitionModel> result = planDefinitionRepository.fetch(id);

        if (result.isEmpty()) throw new ServiceException(
                String.format("Could not find plan definition with tht requested id: '%s'", id),
                ErrorKind.BAD_REQUEST,
                ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST
        );

        return carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(id);
    }

    private boolean questionnaireHasExceededDeadline(CarePlanModel carePlan, List<QualifiedId.QuestionnaireId> questionnaireIds) {
        return carePlan.questionnaires().stream()
                .filter(wrapper -> questionnaireIds.contains(wrapper.questionnaire().id()))
                .map(QuestionnaireWrapperModel::satisfiedUntil)
                .anyMatch(satisfiedUntil -> satisfiedUntil.isBefore(dateProvider.now()));
    }

    private boolean questionnaireHasUnexaminedResponses(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds) throws ServiceException, AccessValidationException {
        return questionnaireResponseRepository.fetch(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), carePlanId)
                .stream()
                .map(QuestionnaireResponseModel::questionnaireId)
                .anyMatch(questionnaireIds::contains);
    }
}
