package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.PlanDefinitionStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireWrapperModel;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Wrapper;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PlanDefinitionService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionService.class);

    private FhirClient fhirClient;
    private FhirMapper fhirMapper;
    private DateProvider dateProvider;

    public PlanDefinitionService(FhirClient fhirClient, FhirMapper fhirMapper, AccessValidator accessValidator, DateProvider dateProvider) {
        super(accessValidator);

        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.dateProvider = dateProvider;
    }

    public List<PlanDefinitionModel> getPlanDefinitions(Optional<Collection<String>> statusesToInclude) throws ServiceException {
        FhirLookupResult lookupResult = fhirClient.lookupPlanDefinitions(statusesToInclude);

        return lookupResult.getPlanDefinitions().stream().map(pd -> fhirMapper.mapPlanDefinition(pd, lookupResult)).collect(Collectors.toList());
    }

    public String createPlanDefinition(PlanDefinitionModel planDefinition) throws ServiceException, AccessValidationException {

        // Check that the referenced questionnaires and plandefinitions are valid for the client to access (and thus use).
        validateReferences(planDefinition);

        // Initialize basic attributes for a new PlanDefinition: Id, dates and so on.
        initializeAttributesForNewPlanDefinition(planDefinition);

        try {
            return fhirClient.savePlanDefinition(fhirMapper.mapPlanDefinitionModel(planDefinition));
        }
        catch(Exception e) {
            throw new ServiceException("Error saving PlanDefinition", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateReferences(PlanDefinitionModel planDefinition) throws AccessValidationException {
        // Validate questionnaires
        if(planDefinition.getQuestionnaires() != null && !planDefinition.getQuestionnaires().isEmpty()) {
            FhirLookupResult lookupResult = fhirClient.lookupQuestionnaires(planDefinition.getQuestionnaires().stream().map(qw -> qw.getQuestionnaire().getId().toString()).collect(Collectors.toList()));
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

    public void updatePlanDefinition(String id, String name, List<String> questionnaireIds, List<ThresholdModel> thresholds) throws ServiceException, AccessValidationException {
        // Look up the questionnaires to verify that they exist, throw an exception in case they don't.
        FhirLookupResult questionnaireResult = fhirClient.lookupQuestionnaires(questionnaireIds);
        if(questionnaireResult.getQuestionnaires().size() != questionnaireIds.size()) {
            throw new ServiceException("Could not look up questionnaires to update!", ErrorKind.BAD_REQUEST, ErrorDetails.QUESTIONNAIRES_MISSING_FOR_CAREPLAN);
        }

        // Validate that the client is allowed to reference the questionnaires.
        validateAccess(questionnaireResult.getQuestionnaires());

        // Look up the plan definition to verify that it exist, throw an exception in case it don't.
        String qualifiedId = FhirUtils.qualifyId(id, ResourceType.PlanDefinition);
        FhirLookupResult planDefinitionResult = fhirClient.lookupPlanDefinition(qualifiedId);
        if(planDefinitionResult.getPlanDefinitions().isEmpty()) {
            throw new ServiceException("Could not look up plan definitions to update!", ErrorKind.BAD_REQUEST, ErrorDetails.PLANDEFINITION_DOES_NOT_EXIST);
        }
        PlanDefinition planDefinition = planDefinitionResult.getPlanDefinition(qualifiedId).get();

        // Validate that the client is allowed to update the planDefinition.
        validateAccess(planDefinition);

        // Update carePlan
        PlanDefinitionModel planDefinitionModel = fhirMapper.mapPlanDefinition(planDefinition, planDefinitionResult);
        List<QuestionnaireModel> questionnaires = questionnaireResult.getQuestionnaires().stream()
            .map(q -> fhirMapper.mapQuestionnaire(q))
            .collect(Collectors.toList());
        updatePlanDefinitionModel(planDefinitionModel, name, questionnaires, thresholds);

        // Save the updated PlanDefinition
        fhirClient.updatePlanDefinition(fhirMapper.mapPlanDefinitionModel(planDefinitionModel));
    }

    private void updatePlanDefinitionModel(PlanDefinitionModel planDefinitionModel, String name, List<QuestionnaireModel> questionnaires, List<ThresholdModel> thresholds) {
        // update name
        if (name != null && !name.isEmpty()) {
            planDefinitionModel.setName(name);
        }

        // update questionnaires
        if (!questionnaires.isEmpty()) {
            List<QuestionnaireWrapperModel> questionnaireWrapperModels = new ArrayList<>();
            for (QuestionnaireModel questionnaire : questionnaires) {
                QuestionnaireWrapperModel wrapper = new QuestionnaireWrapperModel();
                wrapper.setQuestionnaire(questionnaire);

                List<ThresholdModel> questionnaireThresholds = new ArrayList<>();
                questionnaireThresholds.addAll(questionnaire.getQuestions().stream()
                    .filter(questionModel -> Objects.nonNull(questionModel.getThresholds()))
                    .flatMap(q -> q.getThresholds().stream())
                    .collect(Collectors.toList()));

                wrapper.setThresholds(questionnaireThresholds);

                questionnaireWrapperModels.add(wrapper);
            }
            planDefinitionModel.setQuestionnaires(questionnaireWrapperModels);
        }

        // update thresholds
        if (thresholds != null && !thresholds.isEmpty()) {
            // if no questionnaires is beeing updated, the threshold may be present already, remove it if it exists
            Set<String> linkIdUpdates = thresholds.stream().map(t -> t.getQuestionnaireItemLinkId()).collect(Collectors.toSet());
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

                if (questionnaireWrapperModel.isPresent()) {
                    questionnaireWrapperModel.get().getThresholds().add(thresholdModel);
                }
            }

        }
    }
}
