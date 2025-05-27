package dk.kvalitetsit.hjemmebehandling.service.concrete;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.model.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.CarePlanRepository;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteQuestionnaireService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Stream;

import static dk.kvalitetsit.hjemmebehandling.MockFactory.buildQuestionModel;
import static dk.kvalitetsit.hjemmebehandling.service.Constants.QUESTIONNAIRE_ID_1;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireServiceTest {

    @InjectMocks
    private ConcreteQuestionnaireService subject;

    @Mock
    private QuestionnaireRepository<QuestionnaireModel> questionnaireRepository;

    @Mock
    private CarePlanRepository<CarePlanModel, PatientModel> careplanRepository;


    private static Stream<Arguments> updateQuestionnaire_illegalStatusChange_throwsException() {
        Map<Status, List<Status>> valid = new HashMap<>();
        valid.put(Status.ACTIVE, List.of(Status.ACTIVE, Status.RETIRED));
        valid.put(Status.DRAFT, List.of(Status.DRAFT, Status.ACTIVE));

        // compute and add illegal status changes
        Stream.Builder<Arguments> argumentBuilder = Stream.builder();
        for (Status currentStatus : Status.values()) {
            for (Status newStatus : Status.values()) {
                if (!(valid.containsKey(currentStatus) && valid.get(currentStatus).contains(newStatus))) {
                    argumentBuilder.add(Arguments.of(currentStatus, newStatus));
                }
            }
        }
        return argumentBuilder.build();
    }

    @Test
    public void getQuestionnairesById_success() throws Exception {
        QuestionnaireModel questionnaire = QuestionnaireModel.builder()
                .id(QUESTIONNAIRE_ID_1)
                .status(Status.ACTIVE)
                .build();
        Mockito.when(questionnaireRepository.fetch(QUESTIONNAIRE_ID_1)).thenReturn(Optional.of(questionnaire));
        Optional<QuestionnaireModel> result = subject.getQuestionnaireById(QUESTIONNAIRE_ID_1);
        assertTrue(result.isPresent());
        assertEquals(questionnaire, result.get());
    }

    @Test
    public void getQuestionnairesById_notFound() throws Exception {
        Mockito.when(questionnaireRepository.fetch(QUESTIONNAIRE_ID_1)).thenReturn(Optional.empty());
        Optional<QuestionnaireModel> result = subject.getQuestionnaireById(QUESTIONNAIRE_ID_1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getQuestionnaires_success() throws Exception {
        QuestionnaireModel questionnaireModel = QuestionnaireModel.builder().build();
        List<String> statuses = List.of();
        Mockito.when(questionnaireRepository.fetch(statuses)).thenReturn(List.of(questionnaireModel));
        List<QuestionnaireModel> result = subject.getQuestionnaires(Collections.emptyList());

        assertEquals(1, result.size());
        assertEquals(questionnaireModel, result.getFirst());
    }

    @Test
    public void createQuestionnaire_success() throws Exception {
        QuestionnaireModel questionnaireModel = QuestionnaireModel.builder().build();
        var id = new QualifiedId.QuestionnaireId("1");
        Mockito.when(questionnaireRepository.save(Mockito.any(QuestionnaireModel.class))).thenReturn(id);

        QualifiedId.QuestionnaireId result = subject.createQuestionnaire(questionnaireModel);

        assertEquals(id, result);
    }

    @Test
    public void createQuestionnaire_questionLinkId_isSetToRandomUuid_ifNull() throws Exception {
        String linkId = "This should be ignored by the system as it contains a value";
        String nullLinkId = null; // this should trigger the system to set new random uuid with prefix 'urn:uuid'";
        QuestionnaireModel questionnaireModel = QuestionnaireModel.builder()
                .questions(List.of(buildQuestionModel(linkId), buildQuestionModel(nullLinkId)))
                .callToAction(buildQuestionModel(nullLinkId))
                .build();


        subject.createQuestionnaire(questionnaireModel);

        // Assert
        ArgumentCaptor<QuestionnaireModel> captor = ArgumentCaptor.forClass(QuestionnaireModel.class);
        verify(questionnaireRepository, times(1)).save(captor.capture());
        questionnaireModel = captor.getValue();

        assertEquals(2, questionnaireModel.questions().size());
        assertTrue(questionnaireModel.questions().stream().anyMatch(q -> q.linkId().equals(linkId)));
        assertTrue(questionnaireModel.questions().stream().anyMatch(q -> q.linkId().startsWith("urn:uuid")));
        assertTrue(questionnaireModel.questions().stream().noneMatch(q -> q.linkId().equals(nullLinkId)));

        assertNotNull(questionnaireModel.callToAction());
        assertEquals(Systems.CALL_TO_ACTION_LINK_ID, questionnaireModel.callToAction().linkId());

    }

    @Test
    public void updateQuestionnaire_success() throws Exception {
        String newTitle = "new title";
        String newDescription = "new description";
        Status newStatus = Status.ACTIVE;
        List<QuestionModel> newQuestions = List.of(QuestionModel.builder().build());
        QuestionModel newCallToAction = QuestionModel.builder().build();;

        QuestionnaireModel questionnaire = QuestionnaireModel
                .builder()
                .status(Status.ACTIVE)
                .build();

        Mockito.when(questionnaireRepository.fetch(QUESTIONNAIRE_ID_1)).thenReturn(Optional.of(questionnaire));

        subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, newTitle, newDescription, newStatus, newQuestions, newCallToAction);

        ArgumentCaptor<QuestionnaireModel> captor = ArgumentCaptor.forClass(QuestionnaireModel.class);
        verify(questionnaireRepository, times(1)).update(captor.capture());

        assertEquals(newTitle, captor.getValue().title());
        assertEquals(newDescription, captor.getValue().description());
        assertEquals(newStatus, captor.getValue().status());
        assertEquals(newQuestions.size(), captor.getValue().questions().size());
        assertNotNull(captor.getValue().questions().getFirst().linkId());
        assertEquals(Systems.CALL_TO_ACTION_LINK_ID, captor.getValue().callToAction().linkId());
    }

    @Test
    public void updateQuestionnaire_questionLinkId_isSetToRandomUuid() throws Exception {
        String linkId = null; // The system will generate a new random uuid with prefix 'urn:uuid' as linkId";
        Status newStatus = Status.ACTIVE;
        List<QuestionModel> newQuestions = List.of(buildQuestionModel(linkId));
        QuestionModel newCallToAction = buildQuestionModel(linkId);
        QuestionnaireModel questionnaire = QuestionnaireModel.builder().status(Status.ACTIVE).build();
        Mockito.when(questionnaireRepository.fetch(QUESTIONNAIRE_ID_1)).thenReturn(Optional.of(questionnaire));

        subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, newStatus, newQuestions, newCallToAction);

        ArgumentCaptor<QuestionnaireModel> captor = ArgumentCaptor.forClass(QuestionnaireModel.class);

        verify(questionnaireRepository, times(1)).update(captor.capture());

        questionnaire = captor.getValue();

        assertEquals(1, questionnaire.questions().size());
        assertNotNull(captor.getValue().questions().getFirst().linkId());
        assertTrue(captor.getValue().questions().getFirst().linkId().startsWith("urn:uuid"));
        assertNotNull(captor.getValue().callToAction());
        assertEquals(Systems.CALL_TO_ACTION_LINK_ID, captor.getValue().callToAction().linkId());
    }


    @Test
    public void updateQuestionnaire_accessViolation_throwsException() throws Exception {
        Mockito.when(questionnaireRepository.fetch(QUESTIONNAIRE_ID_1)).thenThrow(new AccessValidationException(""));
        assertThrows(AccessValidationException.class, () -> subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, null, null, null));
    }


    @Test
    public void updateQuestionnaire_questionnaireNotFound_throwsException() throws Exception {
        Mockito.when(questionnaireRepository.fetch(QUESTIONNAIRE_ID_1)).thenReturn(Optional.empty());
        assertThrows(ServiceException.class, () -> subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, null, null, null));
    }

    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void updateQuestionnaire_illegalStatusChange_throwsException(Status currentStatus, Status newStatus) throws Exception {
        QuestionnaireModel questionnaire = QuestionnaireModel
                .builder()
                .status(currentStatus)
                .build();
        Mockito.when(questionnaireRepository.fetch(QUESTIONNAIRE_ID_1)).thenReturn(Optional.of(questionnaire));
        assertThrows(ServiceException.class, () -> subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, newStatus, null, null));
    }

    @Test
    public void retireQuestionnaire_noActiveCarePlanReferences_isRetired() throws ServiceException, AccessValidationException {
        QuestionnaireModel questionnaire = QuestionnaireModel.builder()
                .id(QUESTIONNAIRE_ID_1)
                .status(Status.ACTIVE)
                .build();

        Mockito.when(questionnaireRepository.fetch(QUESTIONNAIRE_ID_1)).thenReturn(Optional.of(questionnaire));
        Mockito.when(careplanRepository.fetchActiveCarePlansByQuestionnaireId(QUESTIONNAIRE_ID_1)).thenReturn(List.of());

        subject.retireQuestionnaire(QUESTIONNAIRE_ID_1);

        ArgumentCaptor<QuestionnaireModel> captor = ArgumentCaptor.forClass(QuestionnaireModel.class);

        verify(questionnaireRepository, times(1)).update(captor.capture());

        var retiredQuestionnaire = captor.getValue();

        assertEquals(Status.RETIRED, retiredQuestionnaire.status());
    }

    @Test
    public void retirePlanDefinition_activeCarePlanReferences_throwsError() throws ServiceException, AccessValidationException {
        QuestionnaireModel questionnaire = QuestionnaireModel.builder()
                .id(QUESTIONNAIRE_ID_1)
                .status(Status.ACTIVE)
                .build();

        CarePlanModel activeCarePlan = CarePlanModel.builder().build();

        Mockito.when(questionnaireRepository.fetch(QUESTIONNAIRE_ID_1)).thenReturn(Optional.of(questionnaire));
        Mockito.when(careplanRepository.fetchActiveCarePlansByQuestionnaireId(QUESTIONNAIRE_ID_1)).thenReturn(List.of(activeCarePlan));
        try {
            subject.retireQuestionnaire(QUESTIONNAIRE_ID_1);
            fail();
        } catch (ServiceException se) {
            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN, se.getErrorDetails());
        }
    }

}