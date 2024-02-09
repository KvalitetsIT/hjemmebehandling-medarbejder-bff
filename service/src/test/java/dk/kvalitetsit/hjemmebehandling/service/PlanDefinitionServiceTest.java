package dk.kvalitetsit.hjemmebehandling.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.PlanDefinitionStatus;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.ExtensionMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
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

    @Mock
    private DateProvider dateProvider;

    private static final String PLANDEFINITION_ID_1 = "PlanDefinition/plandefinition-1";
    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";
    private static final String QUESTIONNAIRE_ID_2 = "Questionnaire/questionnaire-2";


    @Test
    public void getPlanDefinitions_sucecss() throws Exception {
        // Arrange
        PlanDefinition planDefinition = new PlanDefinition();
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        FhirLookupResult lookupResult = FhirLookupResult.fromResource(planDefinition);

        Mockito
                .when(fhirClient.lookupPlanDefinitionsByStatus(List.of()))
                .thenReturn(lookupResult);
        Mockito
                .when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult))
                .thenReturn(planDefinitionModel);

        // Act
        List<PlanDefinitionModel> result = subject.getPlanDefinitions(List.of());

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

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of())).thenReturn(FhirLookupResult.fromBundle(new Bundle()));
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        // Act
        subject.updatePlanDefinition(id, "a new name", null, List.of(), List.of());

        // Assert
        assertEquals("a new name", planDefinitionModel.getTitle());
    }

    @Test
    public void patchPlanDefinition_status() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of())).thenReturn(FhirLookupResult.fromBundle(new Bundle()));
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        // Act
        subject.updatePlanDefinition(id, "", PlanDefinitionStatus.DRAFT, List.of(), List.of());

        // Assert
        assertEquals( PlanDefinitionStatus.DRAFT, planDefinitionModel.getStatus());


        subject.updatePlanDefinition(id, "", PlanDefinitionStatus.ACTIVE, List.of(), List.of());

        // Assert
        assertEquals( PlanDefinitionStatus.ACTIVE, planDefinitionModel.getStatus());


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

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of())).thenReturn(FhirLookupResult.fromBundle(new Bundle()));
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        // Act
        subject.updatePlanDefinition(id, null, null, List.of(), List.of());

        // Assert
        assertEquals(name, planDefinitionModel.getName());
    }

    @Test
    public void patchPlanDefinition_threshold() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        planDefinition.getAction().add( buildPlanDefinitionAction(QUESTIONNAIRE_ID_1) );

        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);
        QuestionModel temperatureQuestion = buildMeasurementQuestionModel();

        List<QuestionModel> questions = questionnaireModel.getQuestions();
        questions.add(temperatureQuestion);
        planDefinitionModel.getQuestionnaires().add(buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResources(questionnaire));
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel);

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);


        ThresholdModel thresholdModel = buildThresholdModel(temperatureQuestion.getLinkId());

        // Act
        subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_1), List.of(thresholdModel));

        // Assert
        assertEquals(1, planDefinitionModel.getQuestionnaires().size());
        assertEquals(2, planDefinitionModel.getQuestionnaires().get(0).getThresholds().size());
        assertTrue(planDefinitionModel.getQuestionnaires().get(0).getThresholds().stream().anyMatch(t -> t.getQuestionnaireItemLinkId().equals(thresholdModel.getQuestionnaireItemLinkId())));

    }

    private PlanDefinition.PlanDefinitionActionComponent buildPlanDefinitionAction(String questionnaireId) {
        PlanDefinition.PlanDefinitionActionComponent action = new PlanDefinition.PlanDefinitionActionComponent();

        CanonicalType definitionCanonical = new CanonicalType(questionnaireId);
        action.setDefinition(definitionCanonical);

        return action;
    }

    @Test
    public void patchPlanDefinition_threshold_onMultipleQuestionnaires() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        Questionnaire questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        Questionnaire questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);
        planDefinition.getAction().add( buildPlanDefinitionAction(QUESTIONNAIRE_ID_1) );
        planDefinition.getAction().add( buildPlanDefinitionAction(QUESTIONNAIRE_ID_2) );

        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        QuestionnaireModel questionnaireModel1 = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaireModel2 = buildQuestionnaireModel(QUESTIONNAIRE_ID_2);
        QuestionModel temperatureQuestion1 = buildMeasurementQuestionModel();
        QuestionModel temperatureQuestion2 = buildMeasurementQuestionModel();
        questionnaireModel1.getQuestions().add(temperatureQuestion1);
        questionnaireModel2.getQuestions().add(temperatureQuestion2);
        planDefinitionModel.getQuestionnaires().add(buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1));
        planDefinitionModel.getQuestionnaires().add(buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2));

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1, QUESTIONNAIRE_ID_2))).thenReturn(FhirLookupResult.fromResources(questionnaire1, questionnaire2));
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire1)).thenReturn(questionnaireModel1);
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire2)).thenReturn(questionnaireModel2);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        ThresholdModel thresholdModel1 = buildThresholdModel(temperatureQuestion1.getLinkId());
        ThresholdModel thresholdModel2 = buildThresholdModel(temperatureQuestion2.getLinkId());

        // Act
        subject.updatePlanDefinition(id, null,null, List.of(QUESTIONNAIRE_ID_1, QUESTIONNAIRE_ID_2), List.of(thresholdModel1, thresholdModel2));

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

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of())).thenReturn(FhirLookupResult.fromBundle(new Bundle()));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(FhirLookupResult.fromResources());
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        ThresholdModel newThresholdModel = buildThresholdModel(temperatureQuestion.getLinkId());
        newThresholdModel.setValueQuantityLow(thresholdModel.getValueQuantityLow()-1);

        // Act
        subject.updatePlanDefinition(id, null, null, List.of(), List.of(newThresholdModel));

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

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResource(questionnaire));
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel1);

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(FhirLookupResult.fromResources());

        // Act
        subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());

        // Assert
        assertEquals(1, planDefinitionModel.getQuestionnaires().size());
    }

    @Test
    public void patchPlanDefinition_questionnaire_addNew_activeCarePlanExists_without_same_questionnaire() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaireModel1 = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);
        FhirLookupResult questionnaireResult = FhirLookupResult.fromResource(questionnaire);

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(questionnaireResult);
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel1);

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);


        CarePlan existingCarePlan = new CarePlan();
        CarePlanModel existingCarePlanModel = new CarePlanModel();
        existingCarePlanModel.setQuestionnaires(new ArrayList<>());

        FhirLookupResult carePlanLookupResult = FhirLookupResult.fromResources(existingCarePlan);
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(carePlanLookupResult);
        Mockito.when(fhirMapper.mapCarePlan(existingCarePlan, carePlanLookupResult)).thenReturn(existingCarePlanModel);

        // Act
        subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());

        // Assert
        assertEquals(1, planDefinitionModel.getQuestionnaires().size());
        assertEquals(1, existingCarePlanModel.getQuestionnaires().size());
        assertNotNull(existingCarePlanModel.getQuestionnaires().get(0).getFrequency());
        assertTrue(existingCarePlanModel.getQuestionnaires().get(0).getFrequency().getWeekdays().isEmpty());
        assertNotNull(existingCarePlanModel.getQuestionnaires().get(0).getSatisfiedUntil());
    }

    @Test
    public void patchPlanDefinition_questionnaire_addNew_activeCarePlanExists_with_same_questionnaire() throws AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);
        FhirLookupResult questionnaireResult = FhirLookupResult.fromResource(questionnaire);

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(questionnaireResult);

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        CarePlan existingCarePlan = new CarePlan();
        CarePlanModel existingCarePlanModel = new CarePlanModel();
        existingCarePlanModel.setQuestionnaires(List.of(questionnaireWrapperModel));

        FhirLookupResult carePlanLookupResult = FhirLookupResult.fromResources(existingCarePlan);
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(carePlanLookupResult);
        Mockito.when(fhirMapper.mapCarePlan(existingCarePlan, carePlanLookupResult)).thenReturn(existingCarePlanModel);

        // Act
        try {
            subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());
            fail();
        }
        catch (ServiceException se) {
            // Assert
            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN, se.getErrorDetails());
        }
    }

    @Test
    public void patchPlanDefinition_questionnaire_remove() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        Questionnaire questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel1 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel2 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2);
        planDefinitionModel.setQuestionnaires(List.of(questionnaireWrapperModel1, questionnaireWrapperModel2));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResources(questionnaire1));
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire1)).thenReturn(questionnaireWrapperModel1.getQuestionnaire());

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(FhirLookupResult.fromResources());

        // Act
        subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());

        // Assert
        assertEquals(1, planDefinitionModel.getQuestionnaires().size());
    }

    /**
     * Given:
     *  PlanDefinition with questionnaires1 and questionnaire2
     *  CarePlan based on PlanDefinition with questionnaire1
     * When:
     *  Remove questionnaire2 from PlanDefinition
     * Then:
     *  PlanDefinition only has questionnaire1
     *  CarePlan still have questionnaire1
     * @throws ServiceException
     * @throws AccessValidationException
     */
    @Test
    public void patchPlanDefinition_questionnaire_remove_activeCarePlanExists_without_questionnaire() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        Questionnaire questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel1 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel2 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2);
        planDefinitionModel.setQuestionnaires(List.of(questionnaireWrapperModel1, questionnaireWrapperModel2));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResources(questionnaire1));
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire1)).thenReturn(questionnaireWrapperModel1.getQuestionnaire());

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        CarePlan existingCarePlan = new CarePlan();
        existingCarePlan.setId("CarePlan/careplan-1");
        CarePlanModel existingCarePlanModel = new CarePlanModel();
        existingCarePlanModel.setQuestionnaires(Arrays.asList(questionnaireWrapperModel1));

        FhirLookupResult carePlanLookupResult = FhirLookupResult.fromResources(existingCarePlan);
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(carePlanLookupResult);
        Mockito.when(fhirMapper.mapCarePlan(existingCarePlan, carePlanLookupResult)).thenReturn(existingCarePlanModel);

        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), existingCarePlan.getId())).thenReturn(FhirLookupResult.fromResources());
        // Act
        subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());

        // Assert
        assertEquals(1, planDefinitionModel.getQuestionnaires().size());
        assertEquals(QUESTIONNAIRE_ID_1, planDefinitionModel.getQuestionnaires().get(0).getQuestionnaire().getId().toString());

        assertEquals(1, existingCarePlanModel.getQuestionnaires().size());
        assertEquals(QUESTIONNAIRE_ID_1, existingCarePlanModel.getQuestionnaires().get(0).getQuestionnaire().getId().toString());

    }

    /**
     * Given:
     *  PlanDefinition with questionnaires1 and questionnaire2
     *  CarePlan based on PlanDefinition with questionnaire1 and questionnaire2
     * When:
     *  Remove questionnaire1 from PlanDefinition
     * Then:
     *  PlanDefinition only has questionnaire2
     *  CarePlan still have questionnaire2
     * @throws ServiceException
     * @throws AccessValidationException
     */
    @Test
    public void patchPlanDefinition_questionnaire_remove_activeCarePlanExists_with_questionnaire() throws ServiceException, AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        Questionnaire questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);
        QuestionnaireWrapperModel questionnaireWrapperModel1 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel2 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2);

        planDefinitionModel.setQuestionnaires(new ArrayList<>(Arrays.asList(questionnaireWrapperModel1, questionnaireWrapperModel2)));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_2))).thenReturn(FhirLookupResult.fromResources(questionnaire2));
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire2)).thenReturn(questionnaireWrapperModel2.getQuestionnaire());

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        CarePlan existingCarePlan = new CarePlan();
        existingCarePlan.setId("CarePlan/careplan-1");
        CarePlanModel existingCarePlanModel = new CarePlanModel();
        existingCarePlanModel.setQuestionnaires(new ArrayList<>(Arrays.asList(questionnaireWrapperModel1, questionnaireWrapperModel2)));

        FhirLookupResult carePlanLookupResult = FhirLookupResult.fromResources(existingCarePlan);
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(carePlanLookupResult);
        Mockito.when(fhirMapper.mapCarePlan(existingCarePlan, carePlanLookupResult)).thenReturn(existingCarePlanModel);

        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), existingCarePlan.getId())).thenReturn(FhirLookupResult.fromResources());
        // Act
        subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_2), List.of());

        // Assert
        assertEquals(1, planDefinitionModel.getQuestionnaires().size());
        assertEquals(QUESTIONNAIRE_ID_2, planDefinitionModel.getQuestionnaires().get(0).getQuestionnaire().getId().toString());

        assertEquals(1, existingCarePlanModel.getQuestionnaires().size());
        assertEquals(QUESTIONNAIRE_ID_2, existingCarePlanModel.getQuestionnaires().get(0).getQuestionnaire().getId().toString());
    }

    /**
     * Given:
     *  PlanDefinition with questionnaires1 and questionnaire2
     *  CarePlan based on PlanDefinition with questionnaire1 and questionnaire2
     *   - careplan has missing scheduled responses on questionnaire1 (blå alarm)
     * When:
     *  Remove questionnaire1 from PlanDefinition
     * Then:
     *  Error is thrown
     * @throws ServiceException
     * @throws AccessValidationException
     */
    @Test
    public void patchPlanDefinition_questionnaire_remove_activeCarePlanExists_with_questionnaire_thatHas_missingScheduledResponses() throws  AccessValidationException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        Questionnaire questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);
        QuestionnaireWrapperModel questionnaireWrapperModel1 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel2 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2);

        planDefinitionModel.setQuestionnaires(new ArrayList<>(Arrays.asList(questionnaireWrapperModel1, questionnaireWrapperModel2)));

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_2))).thenReturn(FhirLookupResult.fromResources(questionnaire2));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        // setup and mock careplen
        CarePlan existingCarePlan = new CarePlan();
        existingCarePlan.setId("CarePlan/careplan-1");
        CarePlan.CarePlanActivityComponent activityComponent1 = new CarePlan.CarePlanActivityComponent();
        activityComponent1.getDetail().addInstantiatesCanonical(QUESTIONNAIRE_ID_1).addExtension(ExtensionMapper.mapActivitySatisfiedUntil(Instant.now()));
        existingCarePlan.addActivity(activityComponent1);
        CarePlan.CarePlanActivityComponent activityComponent2 = new CarePlan.CarePlanActivityComponent();
        activityComponent2.getDetail().addInstantiatesCanonical(QUESTIONNAIRE_ID_2).addExtension(ExtensionMapper.mapActivitySatisfiedUntil(Instant.now()));
        existingCarePlan.addActivity(activityComponent2);
        existingCarePlan.addExtension(ExtensionMapper.mapCarePlanSatisfiedUntil(Instant.now()));

        CarePlanModel existingCarePlanModel = new CarePlanModel();
        existingCarePlanModel.setQuestionnaires(new ArrayList<>(Arrays.asList(questionnaireWrapperModel1, questionnaireWrapperModel2)));
        FhirLookupResult carePlanLookupResult = FhirLookupResult.fromResources(existingCarePlan);
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(carePlanLookupResult);

        Mockito.when(dateProvider.now()).thenReturn(Instant.now());

        // Act
        try {
            subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_2), List.of());
            fail();
        }
        catch (ServiceException se) {
            // Assert
            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES, se.getErrorDetails());
        }
    }


    /**
     * Given:
     *  PlanDefinition with questionnaires1 and questionnaire2
     *  CarePlan based on PlanDefinition with questionnaire1 and questionnaire2
     *   - careplan has unhandled responses on questionnaire1
     * When:
     *  Remove questionnaire1 from PlanDefinition
     * Then:
     *  Error is thrown
     * @throws ServiceException
     * @throws AccessValidationException
     */
    @Test
    public void patchPlanDefinition_questionnaire_remove_activeCarePlanExists_with_questionnaire_thatHas_unhandledResponses() throws  AccessValidationException {
        // Arrange
        Instant before = Instant.now();
        Instant after = Instant.now();

        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        Questionnaire questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaireModel1 = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);
        Questionnaire questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);
        QuestionnaireModel questionnaireModel2 = buildQuestionnaireModel(QUESTIONNAIRE_ID_2);
        QuestionnaireWrapperModel questionnaireWrapperModel1 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel2 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2);

        planDefinitionModel.setQuestionnaires(new ArrayList<>(Arrays.asList(questionnaireWrapperModel1, questionnaireWrapperModel2)));

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_2))).thenReturn(FhirLookupResult.fromResources(questionnaire2));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        // setup and mock careplen
        CarePlan existingCarePlan = new CarePlan();
        existingCarePlan.setId("CarePlan/careplan-1");
        CarePlan.CarePlanActivityComponent activityComponent1 = new CarePlan.CarePlanActivityComponent();
        activityComponent1.getDetail().addInstantiatesCanonical(QUESTIONNAIRE_ID_1).addExtension(ExtensionMapper.mapActivitySatisfiedUntil(Instant.MAX));
        existingCarePlan.addActivity(activityComponent1);
        CarePlan.CarePlanActivityComponent activityComponent2 = new CarePlan.CarePlanActivityComponent();
        activityComponent2.getDetail().addInstantiatesCanonical(QUESTIONNAIRE_ID_2).addExtension(ExtensionMapper.mapActivitySatisfiedUntil(Instant.MAX));
        existingCarePlan.addActivity(activityComponent2);
        existingCarePlan.addExtension(ExtensionMapper.mapCarePlanSatisfiedUntil(after));

        CarePlanModel existingCarePlanModel = new CarePlanModel();
        existingCarePlanModel.setQuestionnaires(new ArrayList<>(Arrays.asList(questionnaireWrapperModel1, questionnaireWrapperModel2)));
        FhirLookupResult carePlanLookupResult = FhirLookupResult.fromResources(existingCarePlan);
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(carePlanLookupResult);


        Mockito.when(dateProvider.now()).thenReturn(before);

        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        questionnaireResponse.setQuestionnaire(QUESTIONNAIRE_ID_1);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), existingCarePlan.getId())).thenReturn(FhirLookupResult.fromResources(questionnaireResponse));

        // Act
        try {
            subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_2), List.of());
            fail();
        }
        catch (ServiceException se) {
            // Assert
            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES, se.getErrorDetails());
        }
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

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of())).thenReturn(FhirLookupResult.fromBundle(new Bundle()));

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(FhirLookupResult.fromResources());
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        // Act
        subject.updatePlanDefinition(id, null,null, List.of(), List.of());

        // Assert
        assertEquals(1, planDefinitionModel.getQuestionnaires().size());
    }

    @Test
    public void retirePlanDefinition_noActiveCarePlanReferences_isRetired() throws ServiceException {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, Enumerations.PublicationStatus.ACTIVE);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition);

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);

        // Act
        subject.retirePlanDefinition(id);

        // Assert
        assertEquals(Enumerations.PublicationStatus.RETIRED, planDefinition.getStatus());
    }

    @Test
    public void retirePlanDefinition_activeCarePlanReferences_throwsError() {
        // Arrange
        String id = "plandefinition-1";
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, Enumerations.PublicationStatus.ACTIVE);
        CarePlan activeCarePlan = new CarePlan();
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition, activeCarePlan);

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(lookupResult);

        // Act
        try {
            subject.retirePlanDefinition(id);
            fail();
        }
        catch (ServiceException se) {
            // Assert
            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.PLANDEFINITION_IS_IN_ACTIVE_USE_BY_CAREPLAN, se.getErrorDetails());
        }
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
        questionModel.setLinkId(IdType.newRandomUuid().getValueAsString());
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
        return buildPlanDefinition(planDefinitionId, Enumerations.PublicationStatus.ACTIVE);
    }

    private PlanDefinition buildPlanDefinition(String planDefinitionId, Enumerations.PublicationStatus status) {
        PlanDefinition planDefinition = new PlanDefinition();

        planDefinition.setId(planDefinitionId);
        planDefinition.setStatus(status);

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