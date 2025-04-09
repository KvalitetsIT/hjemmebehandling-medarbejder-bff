package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ResourceType;
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
public class PlanDefinitionService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionService.class);

    private final FhirClient fhirClient;
    private final FhirMapper fhirMapper;
    private final DateProvider dateProvider;

    public PlanDefinitionService(FhirClient fhirClient, FhirMapper fhirMapper, AccessValidator accessValidator, DateProvider dateProvider) {
        super(accessValidator);
        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.dateProvider = dateProvider;
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

    public List<PlanDefinitionModel> getPlanDefinitions(Collection<String> statusesToInclude) throws ServiceException {
        FhirLookupResult lookupResult = fhirClient.lookupPlanDefinitionsByStatus(statusesToInclude);
        return lookupResult.getPlanDefinitions().stream().map(pd -> fhirMapper.mapPlanDefinitionResult(pd, lookupResult)).toList();
    }

    public String createPlanDefinition(PlanDefinitionModel planDefinition) throws ServiceException, AccessValidationException {

        // Check that the referenced questionnaires and plandefinitions are valid for the client to access (and thus use).
        validateReferences(planDefinition);

        // Initialize basic attributes for a new PlanDefinition: Id, dates and so on.
        PlanDefinitionModel.Builder
                .from(planDefinition)
                .created(dateProvider.today().toInstant())
                .build();

        try {
            return fhirClient.save(fhirMapper.mapPlanDefinitionModel(planDefinition)).getId();
        } catch (Exception e) {
            throw new ServiceException("Error saving PlanDefinition", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateReferences(PlanDefinitionModel planDefinition) throws AccessValidationException, ServiceException {
        // Validate questionnaires
        if (planDefinition.questionnaires() != null && !planDefinition.questionnaires().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(planDefinition.questionnaires().stream().map(qw -> qw.questionnaire().id().toString()).toList());
            validateAccess(lookupResult.getQuestionnaires());
        }
    }

    public void updatePlanDefinition(String id, String name, PlanDefinitionStatus status, List<String> questionnaireIds, List<ThresholdModel> thresholds) throws ServiceException, AccessValidationException {

        FhirLookupResult questionnaireResult = fhirClient.lookupQuestionnairesById(questionnaireIds);
        if (questionnaireResult.getQuestionnaires().size() != questionnaireIds.size()) {
            throw new ServiceException(
                    "Could not look up questionnaires to update!",
                    ErrorKind.BAD_REQUEST,
                    ErrorDetails.QUESTIONNAIRES_MISSING_FOR_CAREPLAN
            );
        }

        validateAccess(questionnaireResult.getQuestionnaires());

        String qualifiedId = FhirUtils.qualifyId(id, ResourceType.PlanDefinition);

        FhirLookupResult lookupResult = fhirClient.lookupPlanDefinition(qualifiedId);

        PlanDefinition planDefinition = lookupResult.getPlanDefinition(qualifiedId).orElseThrow(() -> new ServiceException(
                "Could not look up plan definitions to update!",
                ErrorKind.BAD_REQUEST,
                ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST
        ));

        validateAccess(planDefinition);

        PlanDefinitionModel planDefinitionModel = fhirMapper.mapPlanDefinitionResult(planDefinition, lookupResult);

        List<String> currentQuestionnaires = planDefinitionModel.questionnaires().stream()
                .map(q -> q.questionnaire().id().toString())
                .toList();

        List<String> removedQuestionnaireIds = currentQuestionnaires.stream()
                .filter(qid -> !questionnaireIds.contains(qid))
                .toList();

        if (!removedQuestionnaireIds.isEmpty()) {
            List<CarePlan> activeCarePlans = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId).getCarePlans();
            for (CarePlan carePlan : activeCarePlans) {
                String carePlanId = carePlan.getIdElement().toUnqualifiedVersionless().getValue();

                if (questionnaireHasExceededDeadline(carePlan, removedQuestionnaireIds)) {
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

        List<String> newQuestionnaires = questionnaireIds.stream()
                .filter(qid -> !currentQuestionnaires.contains(qid))
                .toList();

        if (!newQuestionnaires.isEmpty()) {
            FhirLookupResult carePlanResult = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
            String orgId = fhirClient.getOrganizationId();

            boolean inUse = carePlanResult.getCarePlans().stream()
                    .map(carePlan -> fhirMapper.mapCarePlan(carePlan, carePlanResult, orgId))
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

        List<QuestionnaireModel> mappedQuestionnaires = questionnaireResult.getQuestionnaires().stream()
                .map(fhirMapper::mapQuestionnaire)
                .toList();

        List<QuestionnaireWrapperModel> updatedWrappers = mappedQuestionnaires.stream()
                .map(q -> new QuestionnaireWrapperModel(q, null, null, thresholds)) // use appropriate defaults/nulls if needed
                .toList();

        planDefinitionModel = new PlanDefinitionModel(
                planDefinitionModel.id(),
                planDefinitionModel.organizationId(),
                name,
                planDefinitionModel.title(),
                status,
                planDefinitionModel.created(),
                Instant.now(),
                updatedWrappers
        );

        fhirClient.update(fhirMapper.mapPlanDefinitionModel(planDefinitionModel));

        // if questionnaire(s) has been removed, remove them from appropriate careplans
        if (!removedQuestionnaireIds.isEmpty()) {
            // get careplans we are removing the questionnaire(s) to
            FhirLookupResult carePlanResult = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
            var orgId = fhirClient.getOrganizationId();
            carePlanResult.getCarePlans().forEach(carePlan -> {
                CarePlanModel carePlanModel = fhirMapper.mapCarePlan(carePlan, carePlanResult, orgId);
                var questionnaires = carePlanModel.questionnaires().stream().filter(qw -> removedQuestionnaireIds.contains(qw.questionnaire().id().toString())).toList();
                fhirClient.update(fhirMapper.mapCarePlanModel(CarePlanModel.Builder.from(carePlanModel).questionnaires(questionnaires).build()));
            });
        }

        // if new questionnaire(s) has been added, add them to appropriate careplans with an empty schedule
        if (!newQuestionnaires.isEmpty()) {
            List<QuestionnaireModel> newQuestionnaireModels = mappedQuestionnaires.stream()
                    .filter(q -> newQuestionnaires.contains(q.id().toString()))
                    .toList();

            FhirLookupResult carePlanResult = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
            String orgId = fhirClient.getOrganizationId();

            carePlanResult.getCarePlans().forEach(carePlan -> {
                CarePlanModel model = fhirMapper.mapCarePlan(carePlan, carePlanResult, orgId);
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

                fhirClient.update(fhirMapper.mapCarePlanModel(updatedModel));
            });
        }
    }

    @NotNull
    private List<String> validateRemovedQuestionnaires(List<String> questionnaireIds, List<String> currentQuestionnaires, String qualifiedId) throws ServiceException {
        List<String> removedQuestionnaireIds = currentQuestionnaires.stream().filter(questionnaireId -> !questionnaireIds.contains(questionnaireId)).toList();

        if (!removedQuestionnaireIds.isEmpty()) {
            var activeCarePlansWithQuestionnaire = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId).getCarePlans();

            // check om der er ubehandlede besvarelser relateret til fjernede spørgeskemaer

            for (CarePlan carePlan : activeCarePlansWithQuestionnaire) {
                String carePlanId = carePlan.getIdElement().toUnqualifiedVersionless().getValue();

                // tjek om et fjernet spørgeskema har blå alarm
                boolean removedQuestionnaireWithExceededDeadline = questionnaireHasExceededDeadline(carePlan, removedQuestionnaireIds);
                if (removedQuestionnaireWithExceededDeadline)
                    throw new ServiceException(String.format("Careplan with id %s has missing scheduled questionnaire-responses!", carePlanId), ErrorKind.BAD_REQUEST, ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES);

                // check om der er ubehandlede besvarelser relateret til fjernede spørgeskemaer
                boolean removedQuestionnaireWithNotExaminedResponses = questionnaireHasUnexaminedResponses(carePlanId, removedQuestionnaireIds);
                if (removedQuestionnaireWithNotExaminedResponses)
                    throw new ServiceException(String.format("Careplan with id %s still has unhandled questionnaire-responses!", carePlanId), ErrorKind.BAD_REQUEST, ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES);
            }
        }
        return removedQuestionnaireIds;
    }

    private List<String> validateQuestionnaires(List<String> questionnaireIds, List<String> currentQuestionnaires, String qualifiedId) throws ServiceException {

        List<String> newQuestionnaires = questionnaireIds.stream().filter(qId -> !currentQuestionnaires.contains(qId)).toList();
        if (!newQuestionnaires.isEmpty()) {
            FhirLookupResult carePlanResult = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
            var orgId = fhirClient.getOrganizationId();
            boolean newQuestionnaireInActiveUse = carePlanResult.getCarePlans().stream().map(carePlan -> fhirMapper.mapCarePlan(carePlan, carePlanResult, orgId)).flatMap(carePlanModel -> carePlanModel.questionnaires().stream().map(questionnaireWrapperModel -> questionnaireWrapperModel.questionnaire().id())).anyMatch(questionnaireId -> newQuestionnaires.contains(questionnaireId.toString()));

            if (newQuestionnaireInActiveUse) {
                throw new ServiceException(String.format("A questionnaire with id %s if used by active careplans!", newQuestionnaires), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN);
            }
        }
        return newQuestionnaires;
    }

    public void retirePlanDefinition(String id) throws ServiceException {
        String qualifiedId = FhirUtils.qualifyId(id, ResourceType.PlanDefinition);
        FhirLookupResult lookupResult = fhirClient.lookupPlanDefinition(qualifiedId);

        Optional<PlanDefinition> planDefinition = lookupResult.getPlanDefinition(qualifiedId);
        if (planDefinition.isEmpty()) {
            throw new ServiceException(String.format("Could not lookup plandefinition with id %s!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST);
        }

        var activeCarePlansWithPlanDefinition = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId).getCarePlans();
        if (!activeCarePlansWithPlanDefinition.isEmpty()) {
            throw new ServiceException(String.format("Plandefinition with id %s if used by active careplans!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.PLANDEFINITION_IS_IN_ACTIVE_USE_BY_CAREPLAN);
        }

        PlanDefinition retiredPlanDefinition = planDefinition.get().setStatus(Enumerations.PublicationStatus.RETIRED);
        fhirClient.update(retiredPlanDefinition);
    }

    public List<CarePlanModel> getCarePlansThatIncludes(String planDefinitionId) throws ServiceException {
        String qualifiedId = FhirUtils.qualifyId(planDefinitionId, ResourceType.PlanDefinition);

        FhirLookupResult lookupResult = fhirClient.lookupPlanDefinition(qualifiedId);

        if (lookupResult.getPlanDefinitions().isEmpty()) {
            throw new ServiceException(String.format("Could not find plandefinition with tht requested id: %s", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST);
        }

        lookupResult.merge(fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId));
        var orgId = fhirClient.getOrganizationId();
        return lookupResult.getCarePlans().stream().map(carePlan -> fhirMapper.mapCarePlan(carePlan, lookupResult, orgId)).toList();
    }

    private boolean questionnaireHasExceededDeadline(CarePlan carePlan, List<String> questionnaireIds) {
        return carePlan.getActivity()
                .stream()
                .filter(carePlanActivityComponent -> questionnaireIds.contains(carePlanActivityComponent.getDetail().getInstantiatesCanonical().getFirst().getValue()))
                .anyMatch(carePlanActivityComponent -> ExtensionMapper.extractActivitySatisfiedUntil(carePlanActivityComponent.getDetail().getExtension()).isBefore(dateProvider.now()));
    }

    private boolean questionnaireHasUnexaminedResponses(String carePlanId, List<String> questionnaireIds) throws ServiceException {
        return fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), carePlanId).getQuestionnaireResponses()
                .stream()
                .anyMatch(questionnaireResponse -> questionnaireIds.contains(questionnaireResponse.getQuestionnaire()));
    }
}
