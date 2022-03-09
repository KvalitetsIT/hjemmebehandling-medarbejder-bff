package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireWrapperModel;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PlanDefinitionServiceTest {
    @InjectMocks
    private PlanDefinitionService subject;

    @Mock
    private FhirClient fhirClient;

    @Mock
    private FhirMapper fhirMapper;

    @Mock
    private AccessValidator accessValidator;

    private static final String PLANDEFINITION_ID_1 = "PlanDefinition/plandefinition-1";
    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";
    private static final String QUESTIONNAIRE_ID_2 = "Questionnaire/questionnaire-2";

    @Test
    public void getPlanDefinitions_sucecss() throws Exception {
        // Arrange
        PlanDefinition planDefinition = new PlanDefinition();
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        FhirLookupResult lookupResult = FhirLookupResult.fromResource(planDefinition);

        Mockito.when(fhirClient.lookupPlanDefinitions()).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        // Act
        List<PlanDefinitionModel> result = subject.getPlanDefinitions();

        // Assert
        assertEquals(1, result.size());
        assertEquals(planDefinitionModel, result.get(0));
    }

    @Test
    public void patchPlanDefinition_name() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);

        Mockito.when(fhirClient.lookupQuestionnaires(List.of())).thenReturn(FhirLookupResult.fromBundle(new Bundle()));
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        // Act
        subject.updatePlanDefinition(id, "a new name", List.of(), List.of());

        // Assert
        assertEquals("a new name", planDefinitionModel.getName());
    }

    @Test
    public void patchPlanDefinition_name_existingIsUntouched() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        String name = "existing name";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        planDefinitionModel.setName(name);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);

        Mockito.when(fhirClient.lookupQuestionnaires(List.of())).thenReturn(FhirLookupResult.fromBundle(new Bundle()));
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        // Act
        subject.updatePlanDefinition(id, null, List.of(), List.of());

        // Assert
        assertEquals(name, planDefinitionModel.getName());
    }

    @Test
    public void patchPlanDefinition_threshold() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);

        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);
        QuestionModel temperatureQuestion = buildMeasurementQuestionModel();

        List<QuestionModel> questions = questionnaireModel.getQuestions();
        questions.add(temperatureQuestion);


        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);

        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResources(questionnaire));
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel);

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);


        ThresholdModel thresholdModel = buildThresholdModel(temperatureQuestion.getLinkId());

        // Act
        subject.updatePlanDefinition(id, null, List.of(QUESTIONNAIRE_ID_1), List.of(thresholdModel));

        // Assert
        assertEquals(1, planDefinitionModel.getQuestionnaires().size());
        assertEquals(2, planDefinitionModel.getQuestionnaires().get(0).getThresholds().size());
        assertTrue(planDefinitionModel.getQuestionnaires().get(0).getThresholds().stream().anyMatch(t -> t.getQuestionnaireItemLinkId().equals(thresholdModel.getQuestionnaireItemLinkId())));

    }

    @Test
    public void patchPlanDefinition_threshold_onMultipleQuestionnaires() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        Questionnaire questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        Questionnaire questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);

        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        QuestionnaireModel questionnaireModel1 = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaireModel2 = buildQuestionnaireModel(QUESTIONNAIRE_ID_2);
        QuestionModel temperatureQuestion = buildMeasurementQuestionModel();
        questionnaireModel1.getQuestions().add(temperatureQuestion);
        questionnaireModel2.getQuestions().add(temperatureQuestion);

        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1, QUESTIONNAIRE_ID_2))).thenReturn(FhirLookupResult.fromResources(questionnaire1, questionnaire2));
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire1)).thenReturn(questionnaireModel1);
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire2)).thenReturn(questionnaireModel2);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        ThresholdModel thresholdModel = buildThresholdModel(temperatureQuestion.getLinkId());

        // Act
        subject.updatePlanDefinition(id, null, List.of(QUESTIONNAIRE_ID_1, QUESTIONNAIRE_ID_2), List.of(thresholdModel));

        // Assert
        assertEquals(2, planDefinitionModel.getQuestionnaires().size());
        assertEquals(2, planDefinitionModel.getQuestionnaires().get(0).getThresholds().size());
        assertEquals(2, planDefinitionModel.getQuestionnaires().get(1).getThresholds().size());

    }

    @Test
    public void patchPlanDefinition_updateThreshold_onExistingQuestionnaire() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        QuestionnaireWrapperModel questionnaireWrapperModel = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionModel temperatureQuestion = buildMeasurementQuestionModel();
        ThresholdModel thresholdModel = buildMeasurementThresholdModel(temperatureQuestion.getLinkId());

        questionnaireWrapperModel.getQuestionnaire().getQuestions().add(temperatureQuestion);
        questionnaireWrapperModel.getThresholds().add(thresholdModel);

        planDefinitionModel.setQuestionnaires(List.of(questionnaireWrapperModel));

        Mockito.when(fhirClient.lookupQuestionnaires(List.of())).thenReturn(FhirLookupResult.fromBundle(new Bundle()));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        ThresholdModel newThresholdModel = buildThresholdModel(temperatureQuestion.getLinkId());
        newThresholdModel.setValueQuantityLow(thresholdModel.getValueQuantityLow()-1);

        // Act
        subject.updatePlanDefinition(id, null, List.of(), List.of(newThresholdModel));

        // Assert
        assertEquals(1, planDefinitionModel.getQuestionnaires().size());
        assertEquals(2, planDefinitionModel.getQuestionnaires().get(0).getThresholds().size());

        Optional<ThresholdModel> updatedThreshold = planDefinitionModel.getQuestionnaires().get(0).getThresholds().stream().filter(t -> t.getQuestionnaireItemLinkId().equals(temperatureQuestion.getLinkId())).findFirst();
        assertTrue(updatedThreshold.isPresent());

        assertNotEquals(thresholdModel.getValueQuantityLow(), newThresholdModel.getValueQuantityLow());
        assertEquals(newThresholdModel.getValueQuantityLow(), updatedThreshold.get().getValueQuantityLow());
    }

    @Test
    public void patchPlanDefinition_questionnaire_addNew() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaireModel1 = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);

        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResource(questionnaire));
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel1);

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        // Act
        subject.updatePlanDefinition(id, null, List.of(QUESTIONNAIRE_ID_1), List.of());

        // Assert
        assertEquals(1, planDefinitionModel.getQuestionnaires().size());
    }

    @Test
    public void patchPlanDefinition_questionnaire_existingIsUntouched() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        QuestionnaireWrapperModel questionnaireWrapperModel = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        planDefinitionModel.setQuestionnaires(List.of(questionnaireWrapperModel));

        //Questionnaire existingQuestionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);

        Mockito.when(fhirClient.lookupQuestionnaires(List.of())).thenReturn(FhirLookupResult.fromBundle(new Bundle()));

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        // Act
        subject.updatePlanDefinition(id, null, List.of(), List.of());

        // Assert
        assertEquals(1, planDefinitionModel.getQuestionnaires().size());
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel(String questionnaireId) {
        QuestionnaireWrapperModel questionnaireWrapperModel = new QuestionnaireWrapperModel();

        QuestionnaireModel questionnaireModel = buildQuestionnaireModel(questionnaireId);
        questionnaireWrapperModel.setQuestionnaire(questionnaireModel);

        List<ThresholdModel> thresholdModels = questionnaireModel.getQuestions().stream()
            .flatMap(q -> q.getThresholds().stream())
            .collect(Collectors.toList());
        questionnaireWrapperModel.setThresholds(thresholdModels);

        return questionnaireWrapperModel;
    }

    private QuestionnaireModel buildQuestionnaireModel(String questionnaireId) {
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        questionnaireModel.setQuestions(new ArrayList<>());

        questionnaireModel.setId(new QualifiedId(questionnaireId));

        QuestionModel questionModel = new QuestionModel();
        questionModel.setLinkId("question-1");
        questionnaireModel.getQuestions().add(questionModel);

        ThresholdModel thresholdModel = buildThresholdModel(questionModel.getLinkId());
        questionModel.setThresholds(List.of(thresholdModel));

        return questionnaireModel;
    }
    private ThresholdModel buildThresholdModel(String questionnaireLinkId) {
        ThresholdModel thresholdModel = new ThresholdModel();
        thresholdModel.setQuestionnaireItemLinkId(questionnaireLinkId);
        thresholdModel.setType(ThresholdType.NORMAL);
        thresholdModel.setValueBoolean(Boolean.TRUE);

        return thresholdModel;
    }

    private QuestionModel buildMeasurementQuestionModel() {
        QuestionModel questionModel = new QuestionModel();
        questionModel.setLinkId("temperature");
        questionModel.setText("Hvad er din temperatur?");
        questionModel.setQuestionType(QuestionType.QUANTITY);

        return questionModel;
    }

    private ThresholdModel buildMeasurementThresholdModel(String questionnaireLinkId) {
        ThresholdModel thresholdModel = new ThresholdModel();
        thresholdModel.setQuestionnaireItemLinkId(questionnaireLinkId);
        thresholdModel.setType(ThresholdType.NORMAL);
        thresholdModel.setValueQuantityLow(Double.valueOf("36.5"));
        thresholdModel.setValueQuantityHigh(Double.valueOf("37.5"));

        return thresholdModel;
    }

    private PlanDefinition buildPlanDefinition(String planDefinitionId) {
        PlanDefinition planDefinition = new PlanDefinition();

        planDefinition.setId(planDefinitionId);

        return planDefinition;
    }

    private PlanDefinitionModel buildPlanDefinitionModel(String questionnaireId, ThresholdModel questionnaireThreshold) {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        QuestionnaireWrapperModel questionnaireWrapperModel = new QuestionnaireWrapperModel();
        questionnaireWrapperModel.setQuestionnaire(new QuestionnaireModel());
        questionnaireWrapperModel.getQuestionnaire().setId(new QualifiedId(questionnaireId));
        questionnaireWrapperModel.setThresholds(List.of(questionnaireThreshold));

        planDefinitionModel.setQuestionnaires(List.of(questionnaireWrapperModel));

        return planDefinitionModel;
    }

    private Questionnaire buildQuestionnaire(String questionnaireId) {
        Questionnaire questionnaire = new Questionnaire();

        questionnaire.setId(questionnaireId);

//        Questionnaire.QuestionnaireItemComponent question1 = questionnaire.addItem();
//        question1.setLinkId("question-1")
//            .setText("Har du det godt?")
//            .setType(Questionnaire.QuestionnaireItemType.BOOLEAN);
//
//        Extension extension = questionnaire.addExtension();
//        extension.addExtension(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID, new StringType("question-1"));
//        extension.addExtension(Systems.THRESHOLD_TYPE, new StringType(ThresholdType.NORMAL.toString()));
//        extension.addExtension(Systems.THRESHOLD_VALUE_BOOLEAN, new BooleanType(true));

        return questionnaire;
    }

    private Questionnaire.QuestionnaireItemComponent buildMeasurementQuestion() {
        Questionnaire.QuestionnaireItemComponent question = new Questionnaire.QuestionnaireItemComponent();
        question.setLinkId("temperature").setText("Hvad er din temperatur?").setType(Questionnaire.QuestionnaireItemType.QUANTITY);

        return question;
    }


}