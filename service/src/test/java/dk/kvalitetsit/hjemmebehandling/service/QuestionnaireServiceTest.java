package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.ClientAdaptor;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Enumerations;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireServiceTest {
    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";
    @InjectMocks
    private QuestionnaireService subject;
    @Mock
    private ClientAdaptor fhirClient;
    @Mock
    private AccessValidator accessValidator;

    private static Stream<Arguments> updateQuestionnaire_illegalStatusChange_throwsException() {
        Map<QuestionnaireStatus, List<QuestionnaireStatus>> valid = new HashMap<>();
        valid.put(QuestionnaireStatus.ACTIVE, List.of(QuestionnaireStatus.ACTIVE, QuestionnaireStatus.RETIRED));
        valid.put(QuestionnaireStatus.DRAFT, List.of(QuestionnaireStatus.DRAFT, QuestionnaireStatus.ACTIVE));

        // compute and add illegal status changes
        Stream.Builder<Arguments> argumentBuilder = Stream.builder();
        for (Enumerations.PublicationStatus currentStatus : Enumerations.PublicationStatus.values()) {
            for (Enumerations.PublicationStatus newStatus : Enumerations.PublicationStatus.values()) {
                if (valid.containsKey(currentStatus) && valid.get(currentStatus).contains(newStatus)) {
                    continue;
                } else {
                    argumentBuilder.add(Arguments.of(currentStatus, newStatus));
                }
            }
        }
        return argumentBuilder.build();
    }

    @Test
    public void getQuestionnairesById_sucecss() throws Exception {
        QuestionnaireModel questionnaire = QuestionnaireModel.builder()
                .id(new QualifiedId(QuestionnaireServiceTest.QUESTIONNAIRE_ID_1))
                .status(QuestionnaireStatus.ACTIVE)
                .build();

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));

        QuestionnaireModel questionnaireModel = QuestionnaireModel.builder().build();
        Optional<QuestionnaireModel> result = subject.getQuestionnaireById(QUESTIONNAIRE_ID_1);

        assertTrue(result.isPresent());
        assertEquals(questionnaireModel, result.get());
    }

    @Test
    public void getQuestionnairesById_notFound() throws Exception {
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of());
        Optional<QuestionnaireModel> result = subject.getQuestionnaireById(QUESTIONNAIRE_ID_1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getQuestionnaires_success() throws Exception {
        QuestionnaireModel questionnaireModel = QuestionnaireModel.builder().build();

        Mockito.when(fhirClient.lookupQuestionnairesByStatus(Collections.emptyList())).thenReturn(List.of(questionnaireModel));

        List<QuestionnaireModel> result = subject.getQuestionnaires(Collections.emptyList());

        assertEquals(1, result.size());
        assertEquals(questionnaireModel, result.getFirst());
    }

    @Test
    public void createQuestionnaire_success() throws Exception {
        QuestionnaireModel questionnaireModel = QuestionnaireModel.builder().build();

        Mockito.when(fhirClient.saveQuestionnaire(Mockito.any(QuestionnaireModel.class))).thenReturn("1");

        String result = subject.createQuestionnaire(questionnaireModel);
        assertEquals("1", result);
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
        String newStatus = "ACTIVE";
        List<QuestionModel> newQuestions = List.of(buildQuestionModel());
        QuestionModel newCallToAction = buildQuestionModel();

        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));

        QuestionnaireModel questionnaireModel = QuestionnaireModel.builder().build();

        subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, newTitle, newDescription, newStatus, newQuestions, newCallToAction);

        assertEquals(newTitle, questionnaireModel.title());
        assertEquals(newDescription, questionnaireModel.description());
        assertEquals(newStatus, questionnaireModel.status().toString());
        assertEquals(newQuestions.size(), questionnaireModel.questions().size());
        assertEquals(newQuestions.getFirst(), questionnaireModel.questions().getFirst());
        assertEquals(newCallToAction, questionnaireModel.callToAction());
    }

    @Test
    public void updateQuestionnaire_questionLinkId_isSetToRandomUuid() throws Exception {
        String linkId = null; // The system will generate a new random uuid with prefix 'urn:uuid' as linkId";
        String newStatus = "ACTIVE";
        List<QuestionModel> newQuestions = List.of(buildQuestionModel(linkId));
        QuestionModel newCallToAction = buildQuestionModel(linkId);
        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        QuestionnaireModel questionnaireModel = QuestionnaireModel.builder().build();

        subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, newStatus, newQuestions, newCallToAction);

        assertEquals(1, questionnaireModel.questions().size());
        assertNotNull(questionnaireModel.questions().getFirst().linkId());
        assertTrue(questionnaireModel.questions().getFirst().linkId().startsWith("urn:uuid"));
        assertNotNull(questionnaireModel.callToAction());
        assertEquals(Systems.CALL_TO_ACTION_LINK_ID, questionnaireModel.callToAction().linkId());
    }

    @Test
    public void updateQuestionnaire_accessViolation_throwsException() throws Exception {
        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(questionnaire);
        assertThrows(AccessValidationException.class, () -> subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, null, null, null));
    }

    @Test
    public void updateQuestionnaire_questionnaireNotFound_throwsException() throws Exception {

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of());
        assertThrows(ServiceException.class, () -> subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, null, null, null));
    }

    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void updateQuestionnaire_illegalStatusChange_throwsException(QuestionnaireStatus currentStatus, Enumerations.PublicationStatus newStatus) throws Exception {
        String newStatusParam = newStatus.toString();
        QuestionnaireModel questionnaire = QuestionnaireModel
                .builder()
                .status(currentStatus)
                .build();

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        assertThrows(ServiceException.class, () -> subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, newStatusParam, null, null));
    }

    @Test
    public void retireQuestionnaire_noActiveCarePlanReferences_isRetired() throws ServiceException {
        String id = "questionnaire-1";
        QuestionnaireModel questionnaire = QuestionnaireModel.builder()
                .id(new QualifiedId(QuestionnaireServiceTest.QUESTIONNAIRE_ID_1))
                .status(QuestionnaireStatus.ACTIVE)
                .build();
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        Mockito.when(fhirClient.lookupActiveCarePlansWithQuestionnaire(QUESTIONNAIRE_ID_1)).thenReturn(List.of());
        subject.retireQuestionnaire(id);
        assertEquals(QuestionnaireStatus.RETIRED, questionnaire.status());
    }

    @Test
    public void retirePlanDefinition_activeCarePlanReferences_throwsError() throws ServiceException {
        String id = "questionnaire-1";
        QuestionnaireModel questionnaire = QuestionnaireModel.builder()
                .id(new QualifiedId(QuestionnaireServiceTest.QUESTIONNAIRE_ID_1))
                .status(QuestionnaireStatus.ACTIVE)
                .build();

        CarePlanModel activeCarePlan = CarePlanModel.builder().build();

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        Mockito.when(fhirClient.lookupActiveCarePlansWithQuestionnaire(QUESTIONNAIRE_ID_1)).thenReturn(List.of(activeCarePlan));
        try {
            subject.retireQuestionnaire(id);
            fail();
        } catch (ServiceException se) {
            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN, se.getErrorDetails());
        }
    }





    private QuestionModel buildQuestionModel(String linkId) {
        var builder = QuestionModel.builder();
        builder.linkId(linkId);
        return builder.build();
    }

    private QuestionModel buildQuestionModel() {
        var builder = QuestionModel.builder();
        return builder.build();
    }

}