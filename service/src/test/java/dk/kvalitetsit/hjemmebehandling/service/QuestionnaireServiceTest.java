package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    private FhirClient fhirClient;
    @Mock
    private FhirMapper fhirMapper;
    @Mock
    private AccessValidator accessValidator;

    private static Stream<Arguments> updateQuestionnaire_illegalStatusChange_throwsException() {
        Map<Enumerations.PublicationStatus, List<Enumerations.PublicationStatus>> valid = new HashMap<>();
        valid.put(Enumerations.PublicationStatus.ACTIVE, List.of(Enumerations.PublicationStatus.ACTIVE, Enumerations.PublicationStatus.RETIRED));
        valid.put(Enumerations.PublicationStatus.DRAFT, List.of(Enumerations.PublicationStatus.DRAFT, Enumerations.PublicationStatus.ACTIVE));

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
        Questionnaire questionnaire = buildQuestionnaire();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);

        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel);

        Optional<QuestionnaireModel> result = subject.getQuestionnaireById(QUESTIONNAIRE_ID_1);

        assertTrue(result.isPresent());
        assertEquals(questionnaireModel, result.get());
    }

    @Test
    public void getQuestionnairesById_notFound() throws Exception {
        FhirLookupResult emptyLookupResult = FhirLookupResult.fromBundle(new Bundle());
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(emptyLookupResult);

        Optional<QuestionnaireModel> result = subject.getQuestionnaireById(QUESTIONNAIRE_ID_1);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getQuestionnaires_success() throws Exception {
        Questionnaire questionnaire = new Questionnaire();
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnairesByStatus(Collections.emptyList())).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel);

        List<QuestionnaireModel> result = subject.getQuestionnaires(Collections.emptyList());

        assertEquals(1, result.size());
        assertEquals(questionnaireModel, result.getFirst());
    }

    @Test
    public void createQuestionnaire_success() throws Exception {
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel();

        Mockito.when(fhirClient.saveQuestionnaire(any())).thenReturn("1");

        String result = subject.createQuestionnaire(questionnaireModel);

        assertEquals("1", result);
    }

    @Test
    public void createQuestionnaire_questionLinkId_isSetToRandomUuid_ifNull() throws Exception {
        String linkId = "This should be ignored by the system as it contains a value";
        String nullLinkId = null; // this should trigger the system to set new random uuid with prefix 'urn:uuid'";
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel();
        questionnaireModel.setQuestions(List.of(buildQuestionModel(linkId), buildQuestionModel(nullLinkId)));
        questionnaireModel.setCallToAction(buildQuestionModel(nullLinkId));
        Mockito.when(fhirClient.saveQuestionnaire(any())).thenReturn("1");

        String result = subject.createQuestionnaire(questionnaireModel);

        assertEquals("1", result);
        assertEquals(2, questionnaireModel.getQuestions().size());
        assertTrue(questionnaireModel.getQuestions().stream().anyMatch(q -> q.getLinkId().equals(linkId)));
        assertTrue(questionnaireModel.getQuestions().stream().anyMatch(q -> q.getLinkId().startsWith("urn:uuid")));
        assertTrue(questionnaireModel.getQuestions().stream().noneMatch(q -> q.getLinkId().equals(nullLinkId)));
        assertNotNull(questionnaireModel.getCallToAction());
        assertEquals(Systems.CALL_TO_ACTION_LINK_ID, questionnaireModel.getCallToAction().getLinkId());
    }

    @Test
    public void updateQuestionnaire_success() throws Exception {
        String newTitle = "new title";
        String newDescription = "new description";
        String newStatus = "ACTIVE";
        List<QuestionModel> newQuestions = List.of(new QuestionModel());
        QuestionModel newCallToAction = new QuestionModel();

        Questionnaire questionnaire = buildQuestionnaire();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);

        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel);

        subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, newTitle, newDescription, newStatus, newQuestions, newCallToAction);

        assertEquals(newTitle, questionnaireModel.getTitle());
        assertEquals(newDescription, questionnaireModel.getDescription());
        assertEquals(newStatus, questionnaireModel.getStatus().toString());
        assertEquals(newQuestions.size(), questionnaireModel.getQuestions().size());
        assertEquals(newQuestions.getFirst(), questionnaireModel.getQuestions().getFirst());
        assertEquals(newCallToAction, questionnaireModel.getCallToAction());
    }

    @Test
    public void updateQuestionnaire_questionLinkId_isSetToRandomUuid() throws Exception {
        String linkId = null; // The system will generate a new random uuid with prefix 'urn:uuid' as linkId";
        String newStatus = "ACTIVE";
        List<QuestionModel> newQuestions = List.of(buildQuestionModel(linkId));
        QuestionModel newCallToAction = buildQuestionModel(linkId);
        Questionnaire questionnaire = buildQuestionnaire();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel);

        subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, newStatus, newQuestions, newCallToAction);

        assertEquals(1, questionnaireModel.getQuestions().size());
        assertNotNull(questionnaireModel.getQuestions().getFirst().getLinkId());
        assertTrue(questionnaireModel.getQuestions().getFirst().getLinkId().startsWith("urn:uuid"));
        assertNotNull(questionnaireModel.getCallToAction());
        assertEquals(Systems.CALL_TO_ACTION_LINK_ID, questionnaireModel.getCallToAction().getLinkId());
    }

    @Test
    public void updateQuestionnaire_accessViolation_throwsException() throws Exception {
        Questionnaire questionnaire = buildQuestionnaire();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(questionnaire);
        assertThrows(AccessValidationException.class, () -> subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, null, null, null));
    }

    @Test
    public void updateQuestionnaire_questionnaireNotFound_throwsException() throws Exception {
        FhirLookupResult emptyLookupResult = FhirLookupResult.fromBundle(new Bundle());
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(emptyLookupResult);
        assertThrows(ServiceException.class, () -> subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, null, null, null));
    }

    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void updateQuestionnaire_illegalStatusChange_throwsException(Enumerations.PublicationStatus currentStatus, Enumerations.PublicationStatus newStatus) throws Exception {
        String newStatusParam = newStatus.toString();
        Questionnaire questionnaire = buildQuestionnaire(currentStatus);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);
        assertThrows(ServiceException.class, () -> subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, newStatusParam, null, null));
    }

    @Test
    public void retireQuestionnaire_noActiveCarePlanReferences_isRetired() throws ServiceException {
        String id = "questionnaire-1";
        Questionnaire questionnaire = buildQuestionnaire(Enumerations.PublicationStatus.ACTIVE);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);
        Mockito.when(fhirClient.lookupActiveCarePlansWithQuestionnaire(QUESTIONNAIRE_ID_1)).thenReturn(lookupResult);
        subject.retireQuestionnaire(id);
        assertEquals(Enumerations.PublicationStatus.RETIRED, questionnaire.getStatus());
    }

    @Test
    public void retirePlanDefinition_activeCarePlanReferences_throwsError() throws ServiceException {
        String id = "questionnaire-1";
        Questionnaire questionnaire = buildQuestionnaire(Enumerations.PublicationStatus.ACTIVE);
        CarePlan activeCarePlan = new CarePlan();
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(questionnaire, activeCarePlan);
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);
        Mockito.when(fhirClient.lookupActiveCarePlansWithQuestionnaire(QUESTIONNAIRE_ID_1)).thenReturn(lookupResult);
        try {
            subject.retireQuestionnaire(id);
            fail();
        } catch (ServiceException se) {
            assertEquals(ErrorKind.BAD_REQUEST, se.getErrorKind());
            assertEquals(ErrorDetails.QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN, se.getErrorDetails());
        }
    }

    private Questionnaire buildQuestionnaire() {
        return buildQuestionnaire(Enumerations.PublicationStatus.ACTIVE);
    }

    private Questionnaire buildQuestionnaire(Enumerations.PublicationStatus status) {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(QuestionnaireServiceTest.QUESTIONNAIRE_ID_1);
        questionnaire.setStatus(status);
        return questionnaire;
    }

    private QuestionnaireModel buildQuestionnaireModel() {
        return new QuestionnaireModel();
    }

    private QuestionModel buildQuestionModel(String linkId) {
        QuestionModel questionModel = new QuestionModel();
        questionModel.setLinkId(linkId);
        return questionModel;
    }
}