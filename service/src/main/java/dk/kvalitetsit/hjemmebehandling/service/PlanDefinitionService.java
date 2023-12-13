package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Number;
import dk.kvalitetsit.hjemmebehandling.constants.PlanDefinitionStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireWrapperModel;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Text;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Question;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

    public List<PlanDefinitionModel> getPlanDefinitions(Collection<String> statusesToInclude) throws ServiceException {
        FhirLookupResult lookupResult = fhirClient.lookupPlanDefinitionsByStatus(statusesToInclude);
        return lookupResult.getPlanDefinitions().stream()
                .map(pd -> fhirMapper.mapPlanDefinition(pd, lookupResult))
                .collect(Collectors.toList());
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

    private void validateReferences(PlanDefinitionModel planDefinition) throws AccessValidationException {
        // Validate questionnaires
        if (planDefinition.getQuestionnaires() != null && !planDefinition.getQuestionnaires().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupQuestionnairesById(planDefinition.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire().getId().toString()).collect(Collectors.toList()));
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
        PlanDefinition planDefinition = planDefinitionResult.getPlanDefinition(qualifiedId).get();

        // Validate that the client is allowed to update the planDefinition.
        validateAccess(planDefinition);

        PlanDefinitionModel planDefinitionModel = fhirMapper.mapPlanDefinition(planDefinition, planDefinitionResult);

        // if questionnaire(s) has been removed, validate that they're not in use
        List<String> currentQuestionnaires = planDefinitionModel.getQuestionnaires().stream().map(q -> q.getQuestionnaire().getId().toString()).toList();
        boolean aQuestionnaireWasRemoved = currentQuestionnaires.stream()
                .anyMatch(questionnaireId -> !questionnaireIds.contains(questionnaireId));

        if (aQuestionnaireWasRemoved) {
            var activeCarePlansWithQuestionnaire = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId).getCarePlans();

            if (!activeCarePlansWithQuestionnaire.isEmpty()) {
                throw new ServiceException(String.format("Questionnaire with id %s if used by active careplans!", qualifiedId), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN);
            }
        }

        // if new questionnaire(s) has been added, validate that they're not in use
        List<String> newQuestionnaires = questionnaireIds.stream().filter(qId -> !currentQuestionnaires.contains(qId)).toList();
        if (!newQuestionnaires.isEmpty()) {
            FhirLookupResult carePlanResult = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
            boolean newQuestionnaireInActiveUse = carePlanResult.getCarePlans().stream()
                    .map(carePlan -> fhirMapper.mapCarePlan(carePlan, carePlanResult))
                    .flatMap(carePlanModel -> carePlanModel.getQuestionnaires().stream().map(questionnaireWrapperModel -> questionnaireWrapperModel.getQuestionnaire().getId()))
                    .anyMatch(questionnaireId -> newQuestionnaires.contains(questionnaireId.toString()));

            if (newQuestionnaireInActiveUse) {
                throw new ServiceException(String.format("A questionnaire with id %s if used by active careplans!", newQuestionnaires), ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN);
            }


        }

        // Update carePlan
        List<QuestionnaireModel> questionnaires = questionnaireResult.getQuestionnaires().stream()
                .map(fhirMapper::mapQuestionnaire)
                .collect(Collectors.toList());
        updatePlanDefinitionModel(planDefinitionModel, name, status, questionnaires, thresholds);

        // Save the updated PlanDefinition
        fhirClient.updatePlanDefinition(fhirMapper.mapPlanDefinitionModel(planDefinitionModel));


        // if new questionnaire(s) has been added, add them to appropriate careplans with an empty schedule
        if (!newQuestionnaires.isEmpty()) {
            // get new questionnaires as list. We are going to add theese to each active careplan that references the edited plandefinition
            List<QuestionnaireModel> newQuestionnaireList = questionnaires.stream()
                    .filter(q -> newQuestionnaires.contains(q.getId().toString()))
                    .toList();

            // get careplans we are adding the questionnaire(s) to
            FhirLookupResult carePlanResult = fhirClient.lookupActiveCarePlansWithPlanDefinition(qualifiedId);
            carePlanResult.getCarePlans().forEach(carePlan -> {
                CarePlanModel carePlanModel = fhirMapper.mapCarePlan(carePlan, carePlanResult);

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

    private void updatePlanDefinitionModel(PlanDefinitionModel planDefinitionModel, String name, PlanDefinitionStatus status, List<QuestionnaireModel> questionnaires, List<ThresholdModel> thresholds) {
        // update name
        if (name != null && !name.isEmpty()) {
            planDefinitionModel.setTitle(name);
        }
        if (status != null) {
            planDefinitionModel.setStatus(status);
        }

        // update questionnaires
        if (!questionnaires.isEmpty()) {
            List<QuestionnaireWrapperModel> questionnaireWrapperModels = new ArrayList<>();
            for (QuestionnaireModel questionnaire : questionnaires) {
                QuestionnaireWrapperModel wrapper = new QuestionnaireWrapperModel();
                wrapper.setQuestionnaire(questionnaire);

                /* This has been temporarily excluded
                List<ThresholdModel> questionnaireThresholds = questionnaire.getQuestions()
                        .stream()
                        .filter(questionModel -> Objects.nonNull(questionModel.getThresholds()))
                        .flatMap(q -> q.getThresholds().stream())
                        .toList();

                wrapper.setThresholds(questionnaireThresholds);
                */


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
                Optional<QuestionnaireWrapperModel> questionnaireWrapperModel = planDefinitionModel.getQuestionnaires().stream()
                        .filter(qw -> {
                            return qw.getQuestionnaire().getQuestions().stream()
                                    .anyMatch(q -> q.getLinkId().equals(thresholdModel.getQuestionnaireItemLinkId()));
                        })
                        .findFirst();

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
        return lookupResult.getCarePlans().stream()
                .map(carePlan -> fhirMapper.mapCarePlan(carePlan, lookupResult))
                .collect(Collectors.toList());
    }


}
