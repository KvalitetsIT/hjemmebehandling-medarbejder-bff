package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.ClientAdaptor;
import dk.kvalitetsit.hjemmebehandling.fhir.ExtensionMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PlanDefinitionServiceTest {
    private static final String PLANDEFINITION_ID_1 = "PlanDefinition/plandefinition-1";
    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";
    private static final String QUESTIONNAIRE_ID_2 = "Questionnaire/questionnaire-2";
    private static final String ORGANISATION_ID_1 = "";
    @InjectMocks
    private PlanDefinitionService subject;
    @Mock
    private ClientAdaptor fhirClient;

    @Mock
    private AccessValidator accessValidator;
    @Mock
    private DateProvider dateProvider;

    @Test
    public void getPlanDefinitions_success() throws Exception {
        PlanDefinitionModel planDefinition = PlanDefinitionModel.builder().build();
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().build();


        Mockito.when(fhirClient.lookupPlanDefinitionsByStatus(List.of())).thenReturn(List.of(planDefinition));

        List<PlanDefinitionModel> result = subject.getPlanDefinitions(List.of());

        assertEquals(1, result.size());
        assertEquals(planDefinitionModel, result.getFirst());
    }


    @Test
    public void patchPlanDefinition_name() throws ServiceException, AccessValidationException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = PlanDefinitionModel.builder().build();


        Mockito.when(fhirClient.lookupQuestionnairesById(List.of())).thenReturn(List.of());
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));


        var expectedName = "a new name";

        subject.updatePlanDefinition(id, expectedName, null, List.of(), List.of());

        ArgumentCaptor<PlanDefinitionModel> captor = ArgumentCaptor.forClass(PlanDefinitionModel.class);

        assertEquals(expectedName, captor.getValue().name());
    }

    @Test
    public void patchPlanDefinition_status() throws ServiceException, AccessValidationException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().build();


        Mockito.when(fhirClient.lookupQuestionnairesById(List.of())).thenReturn(List.of());
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));

        subject.updatePlanDefinition(id, "", PlanDefinitionStatus.DRAFT, List.of(), List.of());

        assertEquals(PlanDefinitionStatus.DRAFT, planDefinitionModel.status());

        subject.updatePlanDefinition(id, "", PlanDefinitionStatus.ACTIVE, List.of(), List.of());

        assertEquals(PlanDefinitionStatus.ACTIVE, planDefinitionModel.status());
    }


    @Test
    public void patchPlanDefinition_name_existingIsUntouched() throws ServiceException, AccessValidationException {
        String id = "plandefinition-1";
        String name = "existing name";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel
                .builder()
                .name(name)
                .build();

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of())).thenReturn(List.of());
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));

        subject.updatePlanDefinition(id, null, null, List.of(), List.of());

        assertEquals(name, planDefinitionModel.name());
    }

    @Test
    public void patchPlanDefinition_threshold() throws ServiceException, AccessValidationException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
//        planDefinition.getAction().add(buildPlanDefinitionAction(QUESTIONNAIRE_ID_1));

        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().build();
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);
        QuestionModel temperatureQuestion = buildMeasurementQuestionModel();

        List<QuestionModel> questions = questionnaireModel.questions();
        questions.add(temperatureQuestion);
        planDefinitionModel.questionnaires().add(buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1));

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));

        ThresholdModel thresholdModel = buildThresholdModel(temperatureQuestion.linkId());

        subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_1), List.of(thresholdModel));

        assertEquals(1, planDefinitionModel.questionnaires().size());
        assertEquals(2, planDefinitionModel.questionnaires().getFirst().thresholds().size());
        assertTrue(planDefinitionModel.questionnaires().getFirst().thresholds().stream().anyMatch(t -> t.questionnaireItemLinkId().equals(thresholdModel.questionnaireItemLinkId())));

    }

    private PlanDefinition.PlanDefinitionActionComponent buildPlanDefinitionAction(String questionnaireId) {
        PlanDefinition.PlanDefinitionActionComponent action = new PlanDefinition.PlanDefinitionActionComponent();
        CanonicalType definitionCanonical = new CanonicalType(questionnaireId);
        action.setDefinition(definitionCanonical);
        return action;
    }

    @Test
    public void patchPlanDefinition_threshold_onMultipleQuestionnaires() throws ServiceException, AccessValidationException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);
//        planDefinition.getAction().add(buildPlanDefinitionAction(QUESTIONNAIRE_ID_1));
//        planDefinition.getAction().add(buildPlanDefinitionAction(QUESTIONNAIRE_ID_2));

        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().build();
        QuestionnaireModel questionnaireModel1 = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaireModel2 = buildQuestionnaireModel(QUESTIONNAIRE_ID_2);
        QuestionModel temperatureQuestion1 = buildMeasurementQuestionModel();
        QuestionModel temperatureQuestion2 = buildMeasurementQuestionModel();
        questionnaireModel1.questions().add(temperatureQuestion1);
        questionnaireModel2.questions().add(temperatureQuestion2);
        planDefinitionModel.questionnaires().add(buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1));
        planDefinitionModel.questionnaires().add(buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2));

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1, QUESTIONNAIRE_ID_2))).thenReturn(List.of(questionnaire1, questionnaire2));

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));

        ThresholdModel thresholdModel1 = buildThresholdModel(temperatureQuestion1.linkId());
        ThresholdModel thresholdModel2 = buildThresholdModel(temperatureQuestion2.linkId());

        subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_1, QUESTIONNAIRE_ID_2), List.of(thresholdModel1, thresholdModel2));

        assertEquals(2, planDefinitionModel.questionnaires().size());
        assertEquals(2, planDefinitionModel.questionnaires().get(0).thresholds().size());
        assertEquals(2, planDefinitionModel.questionnaires().get(1).thresholds().size());

    }


    @Test
    public void patchPlanDefinition_updateThreshold_onExistingQuestionnaire() throws ServiceException, AccessValidationException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();

        QuestionModel temperatureQuestion = buildMeasurementQuestionModel();

        var defaultQuestionnaire = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);
        var questions = new ArrayList<>(defaultQuestionnaire.questions());
        questions.add(temperatureQuestion);

        QuestionnaireModel questionnaireModel = QuestionnaireModel.Builder.from(defaultQuestionnaire)
                .questions(questions)
                .build();

        ThresholdModel thresholdModel = buildMeasurementThresholdModel(temperatureQuestion.linkId());

        var thresholds = questionnaireModel.questions().stream()
                .flatMap(q -> q.thresholds().stream())
                .toList();

        thresholds.add(thresholdModel);

        var questionnaireWrapperModel = QuestionnaireWrapperModel.builder()
                .questionnaire(questionnaireModel)
                .thresholds(thresholds)
                .build();


        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel
                .builder()
                .questionnaires(List.of(questionnaireWrapperModel))
                .build();

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of())).thenReturn(List.of());


        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(List.of());

        ThresholdModel newThresholdModel = buildThresholdModel(temperatureQuestion.linkId(), thresholdModel.valueQuantityLow() - 1);


        subject.updatePlanDefinition(id, null, null, List.of(), List.of(newThresholdModel));

        assertEquals(1, planDefinitionModel.questionnaires().size());
        assertEquals(2, planDefinitionModel.questionnaires().getFirst().thresholds().size());

        Optional<ThresholdModel> updatedThreshold = planDefinitionModel.questionnaires().getFirst().thresholds().stream().filter(t -> t.questionnaireItemLinkId().equals(temperatureQuestion.linkId())).findFirst();
        assertTrue(updatedThreshold.isPresent());

        assertNotEquals(thresholdModel.valueQuantityLow(), newThresholdModel.valueQuantityLow());
        assertEquals(newThresholdModel.valueQuantityLow(), updatedThreshold.get().valueQuantityLow());
    }


    @Test
    public void patchPlanDefinition_questionnaire_addNew() throws ServiceException, AccessValidationException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().build();
        QuestionnaireModel questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaireModel1 = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(List.of());

        subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());


        assertEquals(1, planDefinitionModel.questionnaires().size());
    }

    @Test
    public void patchPlanDefinition_questionnaire_addNew_activeCarePlanExists_without_same_questionnaire() throws ServiceException, AccessValidationException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().build();
        QuestionnaireModel questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaireModel1 = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));


        CarePlanModel existingCarePlanModel = CarePlanModel.builder().build();

        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(List.of(existingCarePlanModel));
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);


        assertEquals(1, planDefinitionModel.questionnaires().size());
        assertEquals(1, existingCarePlanModel.questionnaires().size());
        assertNotNull(existingCarePlanModel.questionnaires().getFirst().frequency());
        assertTrue(existingCarePlanModel.questionnaires().getFirst().frequency().weekdays().isEmpty());
        assertNotNull(existingCarePlanModel.questionnaires().getFirst().satisfiedUntil());
    }

    @Test
    public void patchPlanDefinition_questionnaire_addNew_activeCarePlanExists_with_same_questionnaire() throws AccessValidationException, ServiceException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().build();
        QuestionnaireModel questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);


        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));


        CarePlanModel existingCarePlanModel = CarePlanModel.builder().questionnaires(List.of(questionnaireWrapperModel)).build();

        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(List.of(existingCarePlanModel));
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);

        try {
            subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());
            fail();
        } catch (ServiceException se) {

            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN, se.getErrorDetails());
        }
    }

    @Test
    public void patchPlanDefinition_questionnaire_remove() throws ServiceException, AccessValidationException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);


        var questionnaires = List.of(buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1), buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2));

        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder()
                .questionnaires(questionnaires)
                .build();

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire1));
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(List.of());

        subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());

        assertEquals(1, planDefinitionModel.questionnaires().size());
    }

    /**
     * Given:
     * PlanDefinition with questionnaires1 and questionnaire2
     * CarePlan based on PlanDefinition with questionnaire1
     * When:
     * Remove questionnaire2 from PlanDefinition
     * Then:
     * PlanDefinition only has questionnaire1
     * CarePlan still have questionnaire1
     */
    @Test
    public void patchPlanDefinition_questionnaire_remove_activeCarePlanExists_without_questionnaire() throws ServiceException, AccessValidationException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel1 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel2 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2);

        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel
                .builder()
                .questionnaires(List.of(questionnaireWrapperModel1, questionnaireWrapperModel2))
                .build();

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire1));
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));

        CarePlanModel existingCarePlan = CarePlanModel.builder()
                .id(new QualifiedId("CarePlan/careplan-1"))
                .build();

        CarePlanModel existingCarePlanModel = CarePlanModel.builder()
                .questionnaires(List.of(questionnaireWrapperModel1))
                .build();

        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(List.of(existingCarePlan));
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), existingCarePlan.id().toString())).thenReturn(List.of());

        subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());

        assertEquals(1, planDefinitionModel.questionnaires().size());
        assertEquals(QUESTIONNAIRE_ID_1, planDefinitionModel.questionnaires().getFirst().questionnaire().id().toString());

        assertEquals(1, existingCarePlanModel.questionnaires().size());
        assertEquals(QUESTIONNAIRE_ID_1, existingCarePlanModel.questionnaires().getFirst().questionnaire().id().toString());
    }

    /**
     * Given:
     * PlanDefinition with questionnaires1 and questionnaire2
     * CarePlan based on PlanDefinition with questionnaire1 and questionnaire2
     * When:
     * Remove questionnaire1 from PlanDefinition
     * Then:
     * PlanDefinition only has questionnaire2
     * CarePlan still have questionnaire2
     */
    @Test
    public void patchPlanDefinition_questionnaire_remove_activeCarePlanExists_with_questionnaire() throws ServiceException, AccessValidationException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);
        QuestionnaireWrapperModel questionnaireWrapperModel1 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel2 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2);
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().questionnaires(List.of(questionnaireWrapperModel1, questionnaireWrapperModel2)).build();

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_2))).thenReturn(List.of(questionnaire2));
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));


        CarePlanModel existingCarePlan = CarePlanModel.builder()
                .id(new QualifiedId("CarePlan/careplan-1"))
                .build();

        CarePlanModel existingCarePlanModel = CarePlanModel
                .builder()
                .questionnaires(List.of(questionnaireWrapperModel1, questionnaireWrapperModel2))
                .build();


        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(List.of(existingCarePlan));
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), existingCarePlan.id().toString())).thenReturn(List.of());

        subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_2), List.of());


        assertEquals(1, planDefinitionModel.questionnaires().size());
        assertEquals(QUESTIONNAIRE_ID_2, planDefinitionModel.questionnaires().getFirst().questionnaire().id().toString());

        assertEquals(1, existingCarePlanModel.questionnaires().size());
        assertEquals(QUESTIONNAIRE_ID_2, existingCarePlanModel.questionnaires().getFirst().questionnaire().id().toString());
    }

    /**
     * Given:
     * PlanDefinition with questionnaires1 and questionnaire2
     * CarePlan based on PlanDefinition with questionnaire1 and questionnaire2
     * - careplan has missing scheduled responses on questionnaire1 (blå alarm)
     * When:
     * Remove questionnaire1 from PlanDefinition
     * Then:
     * Error is thrown
     */
    @Test
    public void patchPlanDefinition_questionnaire_remove_activeCarePlanExists_with_questionnaire_thatHas_missingScheduledResponses() throws AccessValidationException, ServiceException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);
        QuestionnaireWrapperModel questionnaireWrapperModel1 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel2 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2);
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().questionnaires((List.of(questionnaireWrapperModel1, questionnaireWrapperModel2))).build();

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_2))).thenReturn(List.of(questionnaire2));

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));

        // setup and mock careplen
        CarePlanModel existingCarePlan = CarePlanModel.builder()
                .id(new QualifiedId("CarePlan/careplan-1"))
                .build();

//        CarePlan.CarePlanActivityComponent activityComponent1 = new CarePlan.CarePlanActivityComponent();
//        activityComponent1.getDetail().addInstantiatesCanonical(QUESTIONNAIRE_ID_1).addExtension(ExtensionMapper.mapActivitySatisfiedUntil(Instant.now()));
//        existingCarePlan.addActivity(activityComponent1);
//        CarePlan.CarePlanActivityComponent activityComponent2 = new CarePlan.CarePlanActivityComponent();
//        activityComponent2.getDetail().addInstantiatesCanonical(QUESTIONNAIRE_ID_2).addExtension(ExtensionMapper.mapActivitySatisfiedUntil(Instant.now()));
//        existingCarePlan.addActivity(activityComponent2);
//        existingCarePlan.addExtension(ExtensionMapper.mapCarePlanSatisfiedUntil(Instant.now()));

        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(List.of(existingCarePlan));

        Mockito.when(dateProvider.now()).thenReturn(Instant.now());

        try {
            subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_2), List.of());
            fail();
        } catch (ServiceException se) {

            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES, se.getErrorDetails());
        }
    }


    /**
     * Given:
     * PlanDefinition with questionnaires1 and questionnaire2
     * CarePlan based on PlanDefinition with questionnaire1 and questionnaire2
     * - careplan has unhandled responses on questionnaire1
     * When:
     * Remove questionnaire1 from PlanDefinition
     * Then:
     * Error is thrown
     */
    @Test
    public void patchPlanDefinition_questionnaire_remove_activeCarePlanExists_with_questionnaire_thatHas_unhandledResponses() throws AccessValidationException, ServiceException {
        Instant before = Instant.now();
        Instant after = Instant.now();

        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaireModel1 = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);
        QuestionnaireModel questionnaireModel2 = buildQuestionnaireModel(QUESTIONNAIRE_ID_2);
        QuestionnaireWrapperModel questionnaireWrapperModel1 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel2 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2);

        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().questionnaires(List.of(questionnaireWrapperModel1, questionnaireWrapperModel2)).build();

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_2))).thenReturn(List.of(questionnaire2));

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));

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

        CarePlanModel existingCarePlanModel = CarePlanModel.builder().questionnaires(List.of(questionnaireWrapperModel1, questionnaireWrapperModel2)).build();

        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(List.of(existingCarePlanModel));

        Mockito.when(dateProvider.now()).thenReturn(before);

        QuestionnaireResponseModel questionnaireResponse = QuestionnaireResponseModel
                .builder()
                .questionnaireId(new QualifiedId(QUESTIONNAIRE_ID_1))
                .build();

        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), existingCarePlan.getId())).thenReturn(List.of(questionnaireResponse));

        try {
            subject.updatePlanDefinition(id, null, null, List.of(QUESTIONNAIRE_ID_2), List.of());
            fail();
        } catch (ServiceException se) {
            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES, se.getErrorDetails());
        }
    }

    @Test
    public void patchPlanDefinition_questionnaire_existingIsUntouched() throws ServiceException, AccessValidationException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireWrapperModel questionnaireWrapperModel = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().questionnaires(List.of(questionnaireWrapperModel)).build();

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of())).thenReturn(List.of());
        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(List.of());

        subject.updatePlanDefinition(id, null, null, List.of(), List.of());

        assertEquals(1, planDefinitionModel.questionnaires().size());
    }

    @Test
    public void retirePlanDefinition_noActiveCarePlanReferences_isRetired() throws ServiceException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();


        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(List.of());

        subject.retirePlanDefinition(id);

        assertEquals(Enumerations.PublicationStatus.RETIRED, planDefinition.status());
    }

    @Test
    public void retirePlanDefinition_activeCarePlanReferences_throwsError() throws ServiceException {
        String id = "plandefinition-1";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        CarePlanModel activeCarePlan = CarePlanModel.builder().build();

        Mockito.when(fhirClient.lookupPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(fhirClient.lookupActiveCarePlansWithPlanDefinition(PLANDEFINITION_ID_1)).thenReturn(List.of(activeCarePlan));

        try {
            subject.retirePlanDefinition(id);
            fail();
        } catch (ServiceException se) {
            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.PLANDEFINITION_IS_IN_ACTIVE_USE_BY_CAREPLAN, se.getErrorDetails());
        }
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel(String questionnaireId) {

        QuestionnaireModel questionnaireModel = buildQuestionnaireModel(questionnaireId);
        return QuestionnaireWrapperModel.builder()
                .questionnaire(questionnaireModel)
                .thresholds(questionnaireModel.questions().stream()
                        .flatMap(q -> q.thresholds().stream())
                        .toList())
                .build();
    }

    private QuestionnaireModel buildQuestionnaireModel(String questionnaireId) {
        var builder = QuestionnaireModel.builder();
        builder.questions(new ArrayList<>());
        builder.id(new QualifiedId(questionnaireId));
        var questionBuilder = QuestionModel.builder();
        questionBuilder.linkId("question-1");
        var question = questionBuilder.build();
        builder.questions(List.of(question));
        ThresholdModel thresholdModel = buildThresholdModel(question.linkId());
        questionBuilder.thresholds(List.of(thresholdModel));
        return builder.build();
    }

    private ThresholdModel buildThresholdModel(String questionnaireLinkId, Double valueQuantityLow) {
        return new ThresholdModel(
                questionnaireLinkId,
                ThresholdType.NORMAL,
                valueQuantityLow,
                null,
                Boolean.TRUE,
                null
        );
    }

    private ThresholdModel buildThresholdModel(String questionnaireLinkId) {
        return new ThresholdModel(
                questionnaireLinkId,
                ThresholdType.NORMAL,
                null,
                null,
                Boolean.TRUE,
                null
        );
    }

    private QuestionModel buildMeasurementQuestionModel() {
        var builder = QuestionModel.builder();
        builder.linkId(IdType.newRandomUuid().getValueAsString());
        builder.text("Hvad er din temperatur?");
        builder.questionType(QuestionType.QUANTITY);
        return builder.build();
    }

    private ThresholdModel buildMeasurementThresholdModel(String questionnaireLinkId) {
        return new ThresholdModel(
                questionnaireLinkId,
                ThresholdType.NORMAL,
                Double.valueOf("36.5"),
                Double.valueOf("37.5"),
                null,
                null
        );
    }

    private PlanDefinitionModel buildPlanDefinition() {
        return PlanDefinitionModel.builder()
                .id(new QualifiedId(PlanDefinitionServiceTest.PLANDEFINITION_ID_1))
                .status(PlanDefinitionStatus.ACTIVE)
                .build();
    }

    private PlanDefinitionModel buildPlanDefinitionModel(String questionnaireId, ThresholdModel questionnaireThreshold) {
        QuestionnaireWrapperModel questionnaireWrapperModel = QuestionnaireWrapperModel.builder()
                .questionnaire(QuestionnaireModel.builder()
                        .id(new QualifiedId(questionnaireId))
                        .build()
                )
                .thresholds(List.of(questionnaireThreshold))
                .build();

        return PlanDefinitionModel.builder()
                .questionnaires(List.of(questionnaireWrapperModel))
                .build();
    }

    private QuestionnaireModel buildQuestionnaire(String questionnaireId) {
        return QuestionnaireModel.builder()
                .id(new QualifiedId(questionnaireId))
                .build();
    }

    private Questionnaire.QuestionnaireItemComponent buildMeasurementQuestion() {
        Questionnaire.QuestionnaireItemComponent question = new Questionnaire.QuestionnaireItemComponent();
        question.setLinkId("temperature").setText("Hvad er din temperatur?").setType(Questionnaire.QuestionnaireItemType.QUANTITY);
        return question;
    }


}