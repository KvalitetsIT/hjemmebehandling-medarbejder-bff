package dk.kvalitetsit.hjemmebehandling.service.concrete;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.repository.CarePlanRepository;
import dk.kvalitetsit.hjemmebehandling.repository.PlanDefinitionRepository;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireResponseRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcretePlanDefinitionService;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.PlanDefinition;
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
import java.util.stream.Stream;

import static dk.kvalitetsit.hjemmebehandling.MockFactory.*;
import static dk.kvalitetsit.hjemmebehandling.service.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PlanDefinitionServiceTest {


    @InjectMocks
    private ConcretePlanDefinitionService subject;

    @Mock
    private PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository;

    @Mock
    private QuestionnaireRepository<QuestionnaireModel> questionnaireRepository;

    @Mock
    private CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository;

    @Mock
    private QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository;


    @Mock
    private DateProvider dateProvider;

    @Test
    public void getPlanDefinitions_success() throws Exception {
        PlanDefinitionModel planDefinition = PlanDefinitionModel.builder().build();
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().build();

        Mockito.when(planDefinitionRepository.lookupPlanDefinitionsByStatus(List.of())).thenReturn(List.of(planDefinition));

        List<PlanDefinitionModel> result = subject.getPlanDefinitions(List.of());

        assertEquals(1, result.size());
        assertEquals(planDefinitionModel, result.getFirst());
    }


    @Test
    public void patchPlanDefinition_name() throws ServiceException, AccessValidationException {
        PlanDefinitionModel planDefinition = PlanDefinitionModel.builder().build();
        List<QualifiedId.QuestionnaireId> ids = List.of();

        Mockito.when(questionnaireRepository.fetch(ids)).thenReturn(List.of());
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));

        var expectedName = "a new name";

        subject.updatePlanDefinition(PLANDEFINITION_ID_1, expectedName, null, List.of(), List.of());

        ArgumentCaptor<PlanDefinitionModel> captor = ArgumentCaptor.forClass(PlanDefinitionModel.class);

        verify(planDefinitionRepository, times(1)).update(captor.capture());

        assertEquals(expectedName, captor.getValue().name());
    }

    @Test
    public void patchPlanDefinition_status() throws ServiceException, AccessValidationException {

        PlanDefinitionModel planDefinition = buildPlanDefinition();

        List<QualifiedId.QuestionnaireId> ids = List.of();
        Mockito.when(questionnaireRepository.fetch(ids)).thenReturn(List.of());
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));

        subject.updatePlanDefinition(PLANDEFINITION_ID_1, "", Status.DRAFT, List.of(), List.of());

        ArgumentCaptor<PlanDefinitionModel> captor1 = ArgumentCaptor.forClass(PlanDefinitionModel.class);
        verify(planDefinitionRepository, times(1)).update(captor1.capture());
        assertEquals(Status.DRAFT, captor1.getValue().status());


        subject.updatePlanDefinition(PLANDEFINITION_ID_1, "", Status.ACTIVE, List.of(), List.of());

        ArgumentCaptor<PlanDefinitionModel> captor2 = ArgumentCaptor.forClass(PlanDefinitionModel.class);
        verify(planDefinitionRepository, times(2)).update(captor2.capture());
        assertEquals(Status.ACTIVE, captor2.getValue().status());
    }


    @Test
    public void patchPlanDefinition_name_existingIsUntouched() throws ServiceException, AccessValidationException {

        String name = "existing name";
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel
                .builder()
                .name(name)
                .build();

        List<QualifiedId.QuestionnaireId> ids = List.of();
        Mockito.when(questionnaireRepository.fetch(ids)).thenReturn(List.of());
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));

        subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, List.of(), List.of());

        fail("Falsk positiv - der testes på et forkert grundlag");
        assertEquals(name, planDefinitionModel.name());
    }

    @Test
    public void patchPlanDefinition_threshold() throws ServiceException, AccessValidationException {

        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
//        planDefinition.getAction().add(buildPlanDefinitionAction(QUESTIONNAIRE_ID_1));

        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().build();
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);
        QuestionModel temperatureQuestion = buildMeasurementQuestionModel();

        List<QuestionModel> questions = questionnaireModel.questions();
        questions.add(temperatureQuestion);

        var questionnaires = Stream.concat(planDefinitionModel.questionnaires().stream(), Stream.of(buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1))).toList();

        planDefinitionModel = PlanDefinitionModel.Builder.from(planDefinitionModel)
                .questionnaires(questionnaires)
                .build();


        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinitionModel));

        ThresholdModel thresholdModel = buildThresholdModel(temperatureQuestion.linkId());

        subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, List.of(QUESTIONNAIRE_ID_1), List.of(thresholdModel));

        ArgumentCaptor<PlanDefinitionModel> captor = ArgumentCaptor.forClass(PlanDefinitionModel.class);
        verify(planDefinitionRepository, times(1)).update(captor.capture());

        assertEquals(1, captor.getValue().questionnaires().size());
        assertEquals(2, captor.getValue().questionnaires().getFirst().thresholds().size());
        assertTrue(captor.getValue().questionnaires().getFirst().thresholds().stream().anyMatch(t -> t.questionnaireItemLinkId().equals(thresholdModel.questionnaireItemLinkId())));

    }

    private PlanDefinition.PlanDefinitionActionComponent buildPlanDefinitionAction(String questionnaireId) {
        PlanDefinition.PlanDefinitionActionComponent action = new PlanDefinition.PlanDefinitionActionComponent();
        CanonicalType definitionCanonical = new CanonicalType(questionnaireId);
        action.setDefinition(definitionCanonical);
        return action;
    }

    @Test
    public void patchPlanDefinition_threshold_onMultipleQuestionnaires() throws ServiceException, AccessValidationException {

        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);

        QuestionModel temperatureQuestion1 = buildMeasurementQuestionModel();
        QuestionModel temperatureQuestion2 = buildMeasurementQuestionModel();

        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().questionnaires(List.of(
                buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1),
                buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2)
        )).build();

        QuestionnaireModel questionnaireModel1 = QuestionnaireModel.Builder.from(buildQuestionnaireModel(QUESTIONNAIRE_ID_1))
                .questions(List.of(temperatureQuestion1))
                .build();

        QuestionnaireModel questionnaireModel2 = QuestionnaireModel.Builder.from(buildQuestionnaireModel(QUESTIONNAIRE_ID_2))
                .questions(List.of(temperatureQuestion2))
                .build();

        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_1, QUESTIONNAIRE_ID_2))).thenReturn(List.of(questionnaire1, questionnaire2));
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));

        ThresholdModel thresholdModel1 = buildThresholdModel(temperatureQuestion1.linkId());
        ThresholdModel thresholdModel2 = buildThresholdModel(temperatureQuestion2.linkId());

        subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, List.of(QUESTIONNAIRE_ID_1, QUESTIONNAIRE_ID_2), List.of(thresholdModel1, thresholdModel2));

        assertEquals(2, planDefinitionModel.questionnaires().size());
        assertEquals(2, planDefinitionModel.questionnaires().get(0).thresholds().size());
        assertEquals(2, planDefinitionModel.questionnaires().get(1).thresholds().size());

    }


    @Test
    public void patchPlanDefinition_updateThreshold_onExistingQuestionnaire() throws ServiceException, AccessValidationException {

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
                .flatMap(q -> Stream.concat(q.thresholds().stream(), Stream.of(thresholdModel)))
                .toList();


        var questionnaireWrapperModel = QuestionnaireWrapperModel.builder()
                .questionnaire(questionnaireModel)
                .thresholds(thresholds)
                .build();

        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of();

        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(List.of());
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(PLANDEFINITION_ID_1)).thenReturn(List.of());

        ThresholdModel newThresholdModel = buildThresholdModel(temperatureQuestion.linkId(), thresholdModel.valueQuantityLow() - 1);

        subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, questionnaireIds, List.of(newThresholdModel));

        ArgumentCaptor<PlanDefinitionModel> captor = ArgumentCaptor.forClass(PlanDefinitionModel.class);
        verify(planDefinitionRepository, times(1)).update(captor.capture());


        assertEquals(1, captor.getValue().questionnaires().size());
        assertEquals(2, captor.getValue().questionnaires().getFirst().thresholds().size());

        Optional<ThresholdModel> updatedThreshold = captor.getValue().questionnaires().getFirst().thresholds().stream().filter(t -> t.questionnaireItemLinkId().equals(temperatureQuestion.linkId())).findFirst();
        assertTrue(updatedThreshold.isPresent());

        assertEquals(1, captor.getValue().questionnaires().size());


        assertNotEquals(thresholdModel.valueQuantityLow(), newThresholdModel.valueQuantityLow());
        assertEquals(newThresholdModel.valueQuantityLow(), updatedThreshold.get().valueQuantityLow());
    }


    @Test
    public void patchPlanDefinition_questionnaire_addNew() throws ServiceException, AccessValidationException {

        PlanDefinitionModel planDefinition = buildPlanDefinition();
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().build();
        QuestionnaireModel questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireModel questionnaireModel1 = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);

        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(PLANDEFINITION_ID_1)).thenReturn(List.of());

        subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());

        ArgumentCaptor<PlanDefinitionModel> captor = ArgumentCaptor.forClass(PlanDefinitionModel.class);
        verify(planDefinitionRepository, times(1)).update(captor.capture());
        assertEquals(1, captor.getValue().questionnaires().size());
    }

    @Test
    public void patchPlanDefinition_questionnaire_addNew_activeCarePlanExists_without_same_questionnaire() throws ServiceException, AccessValidationException {
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        CarePlanModel existingCarePlanModel = CarePlanModel.builder().build();

        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(PLANDEFINITION_ID_1)).thenReturn(List.of(existingCarePlanModel));

        subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());

        ArgumentCaptor<PlanDefinitionModel> captor = ArgumentCaptor.forClass(PlanDefinitionModel.class);
        verify(planDefinitionRepository, times(1)).update(captor.capture());

        assertEquals(1, captor.getValue().questionnaires().size());
        assertEquals(1, existingCarePlanModel.questionnaires().size());
        assertNotNull(existingCarePlanModel.questionnaires().getFirst().frequency());
        assertTrue(existingCarePlanModel.questionnaires().getFirst().frequency().weekdays().isEmpty());
        assertNotNull(existingCarePlanModel.questionnaires().getFirst().satisfiedUntil());
    }

    @Test
    public void patchPlanDefinition_questionnaire_addNew_activeCarePlanExists_with_same_questionnaire() throws AccessValidationException, ServiceException {

        PlanDefinitionModel planDefinition = buildPlanDefinition();
        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().build();
        QuestionnaireModel questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        CarePlanModel existingCarePlanModel = CarePlanModel.builder().questionnaires(List.of(questionnaireWrapperModel)).build();

        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(PLANDEFINITION_ID_1)).thenReturn(List.of(existingCarePlanModel));

        try {
            subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());
        } catch (ServiceException se) {
            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN, se.getErrorDetails());
        }
    }

    @Test
    public void patchPlanDefinition_questionnaire_remove() throws ServiceException, AccessValidationException {

        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        var questionnaires = List.of(buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1), buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2));

        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder()
                .questionnaires(questionnaires)
                .build();

        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire1));
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(PLANDEFINITION_ID_1)).thenReturn(List.of());

        subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());

        ArgumentCaptor<PlanDefinitionModel> captor = ArgumentCaptor.forClass(PlanDefinitionModel.class);
        verify(planDefinitionRepository, times(1)).update(captor.capture());
        assertEquals(1, captor.getValue().questionnaires().size());
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

        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel1 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel2 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2);

        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel
                .builder()
                .questionnaires(List.of(questionnaireWrapperModel1, questionnaireWrapperModel2))
                .build();

        CarePlanModel existingCarePlan = CarePlanModel.builder()
                .id(CAREPLAN_ID_1)
                .build();

        CarePlanModel existingCarePlanModel = CarePlanModel.builder()
                .questionnaires(List.of(questionnaireWrapperModel1))
                .build();

        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire1));
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(PLANDEFINITION_ID_1)).thenReturn(List.of(existingCarePlan));
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), existingCarePlan.id())).thenReturn(List.of());

        subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, List.of(QUESTIONNAIRE_ID_1), List.of());

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

        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);
        QuestionnaireWrapperModel questionnaireWrapperModel1 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel2 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2);

        CarePlanModel existingCarePlan = CarePlanModel.builder()
                .id(CAREPLAN_ID_1)
                .build();

        CarePlanModel existingCarePlanModel = CarePlanModel
                .builder()
                .questionnaires(List.of(questionnaireWrapperModel1, questionnaireWrapperModel2))
                .build();

        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_2))).thenReturn(List.of(questionnaire2));
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(PLANDEFINITION_ID_1)).thenReturn(List.of(existingCarePlan));
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), existingCarePlan.id())).thenReturn(List.of());

        subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, List.of(QUESTIONNAIRE_ID_2), List.of());

        ArgumentCaptor<PlanDefinitionModel> captor = ArgumentCaptor.forClass(PlanDefinitionModel.class);
        verify(planDefinitionRepository, times(1)).update(captor.capture());
        assertEquals(1, captor.getValue().questionnaires().size());

        assertEquals(1, captor.getValue().questionnaires().size());
        assertEquals(QUESTIONNAIRE_ID_2, captor.getValue().questionnaires().getFirst().questionnaire().id());
        assertEquals(1, existingCarePlanModel.questionnaires().size());
        assertEquals(QUESTIONNAIRE_ID_2, existingCarePlanModel.questionnaires().getFirst().questionnaire().id());
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

        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);
        CarePlanModel existingCarePlan = CarePlanModel.builder()
                .id(CAREPLAN_ID_1)
                .build();

        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_2))).thenReturn(List.of(questionnaire2));
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(PLANDEFINITION_ID_1)).thenReturn(List.of(existingCarePlan));
        Mockito.when(dateProvider.now()).thenReturn(Instant.now());

        ServiceException se = assertThrows(ServiceException.class, () -> subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, List.of(QUESTIONNAIRE_ID_2), List.of()));
        assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
        assertEquals(ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES, se.getErrorDetails());
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

        PlanDefinitionModel planDefinition = buildPlanDefinition();
        QuestionnaireModel questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);
        QuestionnaireWrapperModel questionnaireWrapperModel1 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1);
        QuestionnaireWrapperModel questionnaireWrapperModel2 = buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2);

        CarePlanModel existingCarePlanModel = CarePlanModel.builder().questionnaires(List.of(questionnaireWrapperModel1, questionnaireWrapperModel2)).build();

        QuestionnaireResponseModel questionnaireResponse = QuestionnaireResponseModel
                .builder()
                .questionnaireId(QUESTIONNAIRE_ID_1)
                .build();

        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_2))).thenReturn(List.of(questionnaire2));
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(PLANDEFINITION_ID_1)).thenReturn(List.of(existingCarePlanModel));
        Mockito.when(dateProvider.now()).thenReturn(before);
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.NOT_EXAMINED), existingCarePlanModel.id())).thenReturn(List.of(questionnaireResponse));

        try {
            subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, List.of(QUESTIONNAIRE_ID_2), List.of());
        } catch (ServiceException se) {
            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.REMOVED_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES, se.getErrorDetails());
        }
    }

    @Test
    public void patchPlanDefinition_questionnaire_existingIsUntouched() throws ServiceException, AccessValidationException {

        PlanDefinitionModel planDefinition = buildPlanDefinition();
        List<QualifiedId.QuestionnaireId> ids = List.of();

        Mockito.when(questionnaireRepository.fetch(ids)).thenReturn(List.of());
        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(PLANDEFINITION_ID_1)).thenReturn(List.of());

        subject.updatePlanDefinition(PLANDEFINITION_ID_1, null, null, List.of(), List.of());

        ArgumentCaptor<PlanDefinitionModel> captor = ArgumentCaptor.forClass(PlanDefinitionModel.class);
        verify(planDefinitionRepository, times(1)).update(captor.capture());
        assertEquals(1, captor.getValue().questionnaires().size());
    }

    @Test
    public void retirePlanDefinition_noActiveCarePlanReferences_isRetired() throws ServiceException, AccessValidationException {

        PlanDefinitionModel planDefinition = buildPlanDefinition();

        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(PLANDEFINITION_ID_1)).thenReturn(List.of());

        subject.retirePlanDefinition(PLANDEFINITION_ID_1);

        ArgumentCaptor<PlanDefinitionModel> captor = ArgumentCaptor.forClass(PlanDefinitionModel.class);
        verify(planDefinitionRepository, times(1)).update(captor.capture());

        assertEquals(Status.RETIRED, captor.getValue().status());
    }

    @Test
    public void retirePlanDefinition_activeCarePlanReferences_throwsError() throws ServiceException, AccessValidationException {

        PlanDefinitionModel planDefinition = buildPlanDefinition();
        CarePlanModel activeCarePlan = CarePlanModel.builder().build();

        Mockito.when(planDefinitionRepository.fetch(PLANDEFINITION_ID_1)).thenReturn(Optional.of(planDefinition));
        Mockito.when(carePlanRepository.fetchActiveCarePlansByPlanDefinitionId(PLANDEFINITION_ID_1)).thenReturn(List.of(activeCarePlan));

        assertThrows(ServiceException.class, () -> subject.retirePlanDefinition(PLANDEFINITION_ID_1));
    }


}