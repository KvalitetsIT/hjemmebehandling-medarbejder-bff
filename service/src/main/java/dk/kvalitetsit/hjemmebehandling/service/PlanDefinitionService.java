package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
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
import java.util.*;
import java.util.stream.Collectors;

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

    public List<PlanDefinitionModel> getPlanDefinitions(Collection<String> statusesToInclude) throws ServiceException {
        FhirLookupResult lookupResult = fhirClient.lookupPlanDefinitionsByStatus(statusesToInclude);
        return lookupResult.getPlanDefinitions().stream().map(pd -> fhirMapper.mapPlanDefinitionResult(pd, lookupResult)).toList();
    }

    public String createPlanDefinition(PlanDefinitionModel planDefinition) throws ServiceException, AccessValidationException {

        // Check that the referenced questionnaires and plandefinitions are valid for the client to access (and thus use).
        validateReferences(planDefinition);

        // Initialize basic attributes for a new PlanDefinition: Id, dates and so on.
        initializeAttributesForNewPlanDefinition(planDefinition);

        try {
            return fhirClient.savePlanDefinition(fhirMapper.mapPlanDefinitionModel(planDefinition));
        } catch (Exception e) {
            throw new ServiceException("Error saving PlanDefinition", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateReferences(PlanDefinitionModel planDefinition) throws AccessValidationException, ServiceException {
        // Validate questionnaires
        if (planDefinition.getQuestionnaires() != null && !planDefinition.getQuestionnaires().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(planDefinition.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire().getId().toString()).toList());
            validateAccess(lookupResult.getQuestionnaires());
        }
    }

    private void initializeAttributesForNewPlanDefinition(PlanDefinitionModel planDefinition) {
        // Ensure that no id is present on the plandefinition - the FHIR server will generate that for us.
        planDefinition.setId(null);
        initializeTimestamps(planDefinition);
    }

    private void initializeTimestamps(PlanDefinitionModel planDefinition) {
        var today = dateProvider.today().toInstant();
        planDefinition.setCreated(today);
    }

    public void updatePlanDefinition(String id, String name, PlanDefinitionStatus status, List<String> questionnaireIds, List<ThresholdModel> thresholds) throws ServiceException, AccessValidationException {
        // Look up the questionnaires to verify that they exist, throw an exception in case they don't.
        FhirLookupResult questionnaireResult = fhirClient.lookupQuestionnairesById(questionnaireIds);
        if (questionnaireResult.getQuestionnaires().size() != questionnaireIds.size()) {
            throw new ServiceException("Could not look up questionnaires to update!", ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRES_MISSING_FOR_CAREPLAN);
        }

        // Validate that the client is allowed to reference the questionnaires.
        validateAccess(questionnaireResult.getQuestionnaires());

        // Look up the plan definition to verify that it exist, throw an exception in case it don't.
        String qualifiedId = FhirUtils.qualifyId(id, ResourceType.PlanDefinition);
        FhirLookupResult planDefinitionResult = fhirClient.lookupPlanDefinition(qualifiedId);
        if (planDefinitionResult.getPlanDefinitions().isEmpty()) {
            throw new ServiceException("Could not look up plan definitions to update!", ErrorKind.BAD_REQUEST, ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST);
        }

        // TODO: Handle 'Optional.get()' without 'isPresent()' check below
        PlanDefinition planDefinition = planDefinitionResult.getPlanDefinition(qualifiedId).get();
        // Validate that the client is allowed to update the planDefinition.
        validateAccess(planDefinition);

        PlanDefinitionModel planDefinitionModel = fhirMapper.mapPlanDefinitionResult(planDefinition, planDefinitionResult);

        planDefinitionModel.setTitle(name);
        planDefinitionModel.setStatus(status);


        // if questionnaire(s) has been removed, validate that they're not in use
        List<String> currentQuestionnaires = planDefinitionModel.getQuestionnaires().stream().map(q -> q.getQuestionnaire().getId().toString()).toList();
        List<String> removedQuestionnaireIds = validateRemovedQuestionnaires(questionnaireIds, currentQuestionnaires, qualifiedId);

        List<String> newQuestionnaires = validateQuestionnaires(questionnaireIds, currentQuestionnaires, qualifiedId);

        // Update carePlan
        List<QuestionnaireModel> questionnaires = questionnaireResult.getQuestionnaires().stream().map(fhirMapper::mapQuestionnaire).toList();

        updatePlanDefinitionModel(planDefinitionModel, questionnaires, thresholds);

        // Save the updated PlanDefinition
        fhirClient.updatePlanDefinition(fhirMapper.mapPlanDefinitionModel(planDefinitionModel));

        removeQuestionnaires(removedQuestionnaireIds, qualifiedId);

        addQuestionnaires(newQuestionnaires, questionnaires, qualifiedId);
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
            boolean newQuestionnaireInActiveUse = carePlanResult.getCarePlans().stream().map(carePlan -> fhirMapper.mapCarePlan(carePlan, carePlanResult, orgId)).flatMap(carePlanModel -> carePlanModel.getQuestionnaires().stream().map(questionnaireWrapperModel -> questionnaireWrapperModel.getQuestionnaire().getId())).anyMatch(questionnaireId -> newQuestionnaires.contains(questionnaireId.toString()));

            if (newQuestionnaireInActiveUse) {
                throw new ServiceException(String.format("A questionnaire with id %s if used by active careplans!", newQuestionnaires), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN);
            }
        }
        return newQuestionnaires;
    }

    private void removeQuestionnaires(List<String> removedQuestionnaireIds, String qualifiedId) throws ServiceException {
        // if questionnaire(s) has been removed, remove them from appropriate careplans
        if (!removedQuestionnaireIds.isEmpty()) {
            // get careplans we are removing the questionnaire(s) to
            FhirLookupResult carePlanResult = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
            var orgId = fhirClient.getOrganizationId();

            carePlanResult.getCarePlans()
                    .stream()
                    .map(carePlan -> fhirMapper.mapCarePlan(carePlan, carePlanResult, orgId))
                    .forEach(carePlanModel -> {

                        carePlanModel.setQuestionnaires(new ArrayList<>(carePlanModel.getQuestionnaires()));

                        // Remove questionnaires that match the removed IDs
                        carePlanModel.getQuestionnaires().removeIf(qw ->
                                removedQuestionnaireIds.contains(qw.getQuestionnaire().getId().toString()));


                        carePlanModel.getQuestionnaires().removeIf(qw -> removedQuestionnaireIds.contains(qw.getQuestionnaire().getId().toString()));
                        fhirClient.updateCarePlan(fhirMapper.mapCarePlanModel(carePlanModel));
                    });
        }
    }

    private void addQuestionnaires(List<String> newQuestionnaires, List<QuestionnaireModel> questionnaires, String qualifiedId) throws ServiceException {
        // if new questionnaire(s) has been added, add them to appropriate careplans with an empty schedule
        if (!newQuestionnaires.isEmpty()) {
            // get new questionnaires as list. We are going to add theese to each active careplan that references the edited plandefinition
            List<QuestionnaireModel> newQuestionnaireList = questionnaires.stream().filter(q -> newQuestionnaires.contains(q.getId().toString())).toList();

            // get careplans we are adding the questionnaire(s) to
            FhirLookupResult carePlanResult = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
            var orgId = fhirClient.getOrganizationId();
            carePlanResult.getCarePlans().forEach(carePlan -> {
                CarePlanModel carePlanModel = fhirMapper.mapCarePlan(carePlan, carePlanResult, orgId);

                // loop quesitonnaires and add
                newQuestionnaireList.forEach(questionnaireModel -> {
                    QuestionnaireWrapperModel qw = new QuestionnaireWrapperModel();
                    qw.setQuestionnaire(questionnaireModel);

                    FrequencyModel frequencyModel = new FrequencyModel();
                    frequencyModel.setTimeOfDay(LocalTime.parse("11:00"));
                    frequencyModel.setWeekdays(new ArrayList<>());
                    qw.setFrequency(frequencyModel);
                    qw.setSatisfiedUntil(Instant.MAX);

                    carePlanModel.getQuestionnaires().add(qw);
                });

                fhirClient.updateCarePlan(fhirMapper.mapCarePlanModel(carePlanModel));
            });
        }
    }

    private void updatePlanDefinitionModel(PlanDefinitionModel planDefinitionModel, List<QuestionnaireModel> questionnaires, List<ThresholdModel> thresholds) {
        // update questionnaires
        if (!questionnaires.isEmpty()) {
            List<QuestionnaireWrapperModel> questionnaireWrapperModels = new ArrayList<>();
            for (QuestionnaireModel questionnaire : questionnaires) {
                QuestionnaireWrapperModel wrapper = new QuestionnaireWrapperModel();
                wrapper.setQuestionnaire(questionnaire);

                List<ThresholdModel> questionnaireThresholds = new ArrayList<>(questionnaire.getQuestions().stream().filter(questionModel -> Objects.nonNull(questionModel.getThresholds())).flatMap(q -> q.getThresholds().stream()).toList());

                wrapper.setThresholds(questionnaireThresholds);

                questionnaireWrapperModels.add(wrapper);
            }
            planDefinitionModel.setQuestionnaires(questionnaireWrapperModels);
        }

        // update thresholds
        if (thresholds != null && !thresholds.isEmpty()) {
            // if no questionnaires is beeing updated, the threshold may be present already, remove it if it exists
            Set<String> linkIdUpdates = thresholds.stream().map(ThresholdModel::getQuestionnaireItemLinkId).collect(Collectors.toSet());
            planDefinitionModel.getQuestionnaires().forEach(qw -> {
                qw.getThresholds().removeIf(t -> linkIdUpdates.contains(t.getQuestionnaireItemLinkId()));
            });

            // add updated thresholds
            for (ThresholdModel thresholdModel : thresholds) {
                // add the new threshold to the correct quesionnaire containing the question (by linkId)
                Optional<QuestionnaireWrapperModel> questionnaireWrapperModel = planDefinitionModel.getQuestionnaires().stream().filter(qw -> qw.getQuestionnaire().getQuestions().stream().anyMatch(q -> {
                    boolean directMatch = q.getLinkId().equals(thresholdModel.getQuestionnaireItemLinkId());
                    boolean subQuesitonMatch = false;
                    if (q.getQuestionType() == QuestionType.GROUP) {
                        subQuesitonMatch = q.getSubQuestions().stream().anyMatch(sq -> sq.getLinkId().equals(thresholdModel.getQuestionnaireItemLinkId()));
                    }
                    return directMatch || subQuesitonMatch;
                })).findFirst();

                questionnaireWrapperModel.ifPresent(wrapperModel -> wrapperModel.getThresholds().add(thresholdModel));
            }

        }
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
        fhirClient.updatePlanDefinition(retiredPlanDefinition);
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
