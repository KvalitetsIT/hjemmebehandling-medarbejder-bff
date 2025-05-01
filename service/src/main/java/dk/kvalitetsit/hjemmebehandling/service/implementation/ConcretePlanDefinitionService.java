package dk.kvalitetsit.hjemmebehandling.service.implementation;

import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.ExtensionMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.repository.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.validation.AccessValidatingService;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Organization;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class ConcretePlanDefinitionService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(ConcretePlanDefinitionService.class);

    private final PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository;

    private final FhirMapper fhirMapper;
    private final DateProvider dateProvider;
    private final QuestionnaireRepository<QuestionnaireModel> questionnaireRepository;
    private final CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository;
    private final QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository;
    private final OrganizationRepository<Organization> organizationRepository;

    public ConcretePlanDefinitionService(
            FhirMapper fhirMapper,
            AccessValidator accessValidator,
            PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository,
            DateProvider dateProvider,
            QuestionnaireRepository<QuestionnaireModel> questionnaireRepository,
            CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository,
            QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository,
            OrganizationRepository<Organization> organizationRepository
    ) {
        super(accessValidator);
        this.fhirMapper = fhirMapper;
        this.planDefinitionRepository = planDefinitionRepository;
        this.dateProvider = dateProvider;
        this.questionnaireRepository = questionnaireRepository;
        this.carePlanRepository = carePlanRepository;
        this.questionnaireResponseRepository = questionnaireResponseRepository;
        this.organizationRepository = organizationRepository;
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

    public List<PlanDefinitionModel> getPlanDefinitions(Collection<PlanDefinitionStatus> statusesToInclude) throws ServiceException {
        return planDefinitionRepository.lookupPlanDefinitionsByStatus(statusesToInclude);
    }

    public QualifiedId.PlanDefinitionId createPlanDefinition(PlanDefinitionModel planDefinition) throws ServiceException, AccessValidationException {

        // Check that the referenced questionnaires and plandefinitions are valid for the client to access (and thus use).
        validateReferences(planDefinition);

        // Initialize basic attributes for a new PlanDefinition: Id, dates and so on.
        PlanDefinitionModel.Builder
                .from(planDefinition)
                .created(dateProvider.today().toInstant())
                .build();

        try {
            return planDefinitionRepository.save(planDefinition);
        } catch (Exception e) {
            throw new ServiceException("Error saving PlanDefinition", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateReferences(PlanDefinitionModel planDefinition) throws AccessValidationException, ServiceException {
        // Validate questionnaires
        if (planDefinition.questionnaires() != null && !planDefinition.questionnaires().isEmpty()) {
            var ids = planDefinition.questionnaires().stream().map(qw -> qw.questionnaire().id()).toList();
            questionnaireRepository.fetch(ids);
        }
    }

    // TODO: Breakdown this method into multiple methods it is way too long
    public void updatePlanDefinition(
            QualifiedId.PlanDefinitionId id,
            String name,
            PlanDefinitionStatus status,
            List<QualifiedId.QuestionnaireId> questionnaireIds,
            List<ThresholdModel> thresholds
    ) throws ServiceException, AccessValidationException {

        List<QuestionnaireModel> questionnaires = questionnaireRepository.fetch(questionnaireIds);

        if (questionnaires.size() != questionnaireIds.size()) {
            throw new ServiceException(
                    "Could not look up questionnaires to update!",
                    ErrorKind.BAD_REQUEST,
                    ErrorDetails.QUESTIONNAIRES_MISSING_FOR_CAREPLAN
            );
        }


        PlanDefinitionModel planDefinition = planDefinitionRepository.fetch(id).orElseThrow(() -> new ServiceException(
                "Could not look up plan definitions to update!",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST
        ));


        List<QualifiedId.QuestionnaireId> currentQuestionnaires = planDefinition.questionnaires().stream()
                .map(q -> q.questionnaire().id())
                .toList();

        List<QualifiedId.QuestionnaireId> removedQuestionnaireIds = currentQuestionnaires.stream()
                .filter(qid -> !questionnaireIds.contains(qid))
                .toList();

        if (!removedQuestionnaireIds.isEmpty()) {
            List<CarePlanModel> activeCarePlans = carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(id);
            for (CarePlanModel carePlan : activeCarePlans) {
                QualifiedId.CarePlanId carePlanId = carePlan.id();

                if (questionnaireHasExceededDeadline(fhirMapper.mapCarePlanModel(carePlan), removedQuestionnaireIds)) {
                    throw new ServiceException(
                            String.format("Careplan with id %s has missing scheduled questionnaire-responses!", carePlanId),
                            ErrorKind.BAD_REQUEST,
                            ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES
                    );
                }

                if (questionnaireHasUnexaminedResponses(carePlanId, removedQuestionnaireIds)) {
                    throw new ServiceException(
                            String.format("Careplan with id %s still has unhandled questionnaire-responses!", carePlanId),
                            ErrorKind.BAD_REQUEST,
                            ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES
                    );
                }
            }
        }

        List<QualifiedId.QuestionnaireId> newQuestionnaires = questionnaireIds.stream()
                .filter(qid -> !currentQuestionnaires.contains(qid))
                .toList();

        if (!newQuestionnaires.isEmpty()) {
            List<CarePlanModel> result = carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(id);
            var orgId = organizationRepository.getOrganizationId();

            boolean inUse = result.stream()
                    .flatMap(model -> model.questionnaires().stream())
                    .map(qw -> qw.questionnaire().id().toString())
                    .anyMatch(newQuestionnaires::contains);

            if (inUse) {
                throw new ServiceException(
                        String.format("A questionnaire with id %s is used by active careplans!", newQuestionnaires),
                        ErrorKind.BAD_REQUEST,
                        ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN
                );
            }
        }

        List<QuestionnaireWrapperModel> updatedWrappers = questionnaires.stream()
                .map(q -> new QuestionnaireWrapperModel(q, null, null, thresholds)) // use appropriate defaults/nulls if needed
                .toList();

        planDefinition = new PlanDefinitionModel(
                planDefinition.id(),
                planDefinition.organizationId(),
                name,
                planDefinition.title(),
                status,
                planDefinition.created(),
                Instant.now(),
                updatedWrappers
        );

        planDefinitionRepository.update(planDefinition);

        // if questionnaire(s) has been removed, remove them from appropriate careplans
        if (!removedQuestionnaireIds.isEmpty()) {
            // get careplans we are removing the questionnaire(s) to
            List<CarePlanModel> carePlans = carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(id);
            var orgId = organizationRepository.getOrganizationId();

            for (var carePlan : carePlans) {
                var result = carePlan.questionnaires().stream()
                        .filter(qw -> removedQuestionnaireIds.contains(qw.questionnaire().id())).toList();
                carePlanRepository.update(CarePlanModel.Builder.from(carePlan).questionnaires(result).build());
            }
        }

        // if new questionnaire(s) has been added, add them to appropriate careplans with an empty schedule
        if (!newQuestionnaires.isEmpty()) {
            List<QuestionnaireModel> newQuestionnaireModels = questionnaires.stream()
                    .filter(q -> newQuestionnaires.contains(q.id()))
                    .toList();

            List<CarePlanModel> carePlans = carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(id);
            var orgId = organizationRepository.getOrganizationId();

            for (var model : carePlans){
                List<QuestionnaireWrapperModel> updatedQuestionnaires = getQuestionnaireWrapperModels(thresholds, model, newQuestionnaireModels);
                CarePlanModel updatedModel = new CarePlanModel(
                        model.id(),
                        model.organizationId(),
                        model.title(),
                        model.status(),
                        model.created(),
                        model.startDate(),
                        model.endDate(),
                        model.patient(),
                        updatedQuestionnaires,
                        model.planDefinitions(),
                        model.departmentName(),
                        model.satisfiedUntil()
                );

                carePlanRepository.update(updatedModel);
            }
        }
    }


    public void retirePlanDefinition(QualifiedId.PlanDefinitionId id) throws ServiceException {
        Optional<PlanDefinitionModel> result = planDefinitionRepository.fetch(id);

        if (result.isEmpty()) {
            throw new ServiceException(String.format("Could not lookup plandefinition with id %s!", id), ErrorKind.BAD_REQUEST, ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST);
        }

        var activeCarePlansWithPlanDefinition = carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(id);
        if (!activeCarePlansWithPlanDefinition.isEmpty()) {
            throw new ServiceException(String.format("Plandefinition with id %s if used by active careplans!", id), ErrorKind.BAD_REQUEST, ErrorDetails.PLANDEFINITION_IS_IN_ACTIVE_USE_BY_CAREPLAN);
        }

        PlanDefinitionModel retiredPlanDefinition = PlanDefinitionModel.Builder.from(result.get()).status(PlanDefinitionStatus.RETIRED).build();
        planDefinitionRepository.update(retiredPlanDefinition);
    }


    public List<CarePlanModel> getCarePlansThatIncludes(QualifiedId.PlanDefinitionId id) throws ServiceException {
        Optional<PlanDefinitionModel> result = planDefinitionRepository.fetch(id);

        if (result.isEmpty()) {
            throw new ServiceException(String.format("Could not find plandefinition with tht requested id: %s", id), ErrorKind.BAD_REQUEST, ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST);
        }

        List<CarePlanModel> activeCarePlans = carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(id);

        var orgId = organizationRepository.getOrganizationId();
        return activeCarePlans;
    }


    private boolean questionnaireHasExceededDeadline(CarePlan carePlan, List<QualifiedId.QuestionnaireId> questionnaireIds) {
        return carePlan.getActivity()
                .stream()
                .filter(carePlanActivityComponent -> questionnaireIds.contains(carePlanActivityComponent.getDetail().getInstantiatesCanonical().getFirst().getValue()))
                .anyMatch(carePlanActivityComponent -> ExtensionMapper.extractActivitySatisfiedUntil(carePlanActivityComponent.getDetail().getExtension()).isBefore(dateProvider.now()));
    }

    private boolean questionnaireHasUnexaminedResponses(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds) throws ServiceException {
        return questionnaireResponseRepository.fetch(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), carePlanId)
                .stream()
                .anyMatch(questionnaireIds::contains);
    }
}
