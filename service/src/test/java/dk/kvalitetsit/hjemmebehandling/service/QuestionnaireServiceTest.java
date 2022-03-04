package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Bundle;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireServiceTest {
    @InjectMocks
    private QuestionnaireService subject;

    @Mock
    private FhirClient fhirClient;

    @Mock
    private FhirMapper fhirMapper;

    @Mock
    private AccessValidator accessValidator;

    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";

    @Test
    public void getQuestionnairesById_sucecss() throws Exception {
        // Arrange
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);

        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel);

        // Act
        Optional<QuestionnaireModel> result = subject.getQuestionnaireById(QUESTIONNAIRE_ID_1);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(questionnaireModel, result.get());
    }

    @Test
    public void getQuestionnairesById_notFound() throws Exception {
        // Arrange
        FhirLookupResult emptyLookupResult = FhirLookupResult.fromBundle(new Bundle());
        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(emptyLookupResult);

        // Act
        Optional<QuestionnaireModel> result = subject.getQuestionnaireById(QUESTIONNAIRE_ID_1);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    public void getQuestionnaires_sucecss() throws Exception {
        // Arrange
        Questionnaire questionnaire = new Questionnaire();
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();

        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);

        Mockito.when(fhirClient.lookupQuestionnaires()).thenReturn(lookupResult);
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel);

        // Act
        List<QuestionnaireModel> result = subject.getQuestionnaires();

        // Assert
        assertEquals(1, result.size());
        assertEquals(questionnaireModel, result.get(0));
    }

    @Test
    public void createQuestionnaire_success() throws Exception {
        // Arrange
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel(QUESTIONNAIRE_ID_1);

        Mockito.when(fhirClient.saveQuestionnaire(any())).thenReturn("1");

        // Act
        String result = subject.createQuestionnaire(questionnaireModel);

        // Assert
        assertEquals("1", result);
    }

    @Test
    public void updateQuestionnaire_success() throws Exception {
        // Arrange
        String newTitle = "new title";
        String newDescription = "new description";
        String newStatus = "ACTIVE";
        List<QuestionModel> newQuestions = List.of(new QuestionModel());
        List<QuestionModel> newCallToActions = List.of(new QuestionModel());

        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);

        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel);

        // Act
        subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, newTitle, newDescription, newStatus, newQuestions, newCallToActions);

        // Assert
        assertEquals(newTitle, questionnaireModel.getTitle());
        assertEquals(newDescription, questionnaireModel.getDescription());
        assertEquals(newStatus, questionnaireModel.getStatus().toString());
        assertEquals(newQuestions.size(), questionnaireModel.getQuestions().size());
        assertEquals(newQuestions.get(0), questionnaireModel.getQuestions().get(0));
        assertEquals(newCallToActions.size(), questionnaireModel.getCallToActions().size());
        assertEquals(newCallToActions.get(0), questionnaireModel.getCallToActions().get(0));
    }

    @Test
    public void updateQuestionnaire_accessViolation_throwsException() throws Exception {
        // Arrange
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(questionnaire);

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, null, null, null));
    }

    @Test
    public void updateQuestionnaire_questionnaireNotFound_throwsException() throws Exception {
        // Arrange
        FhirLookupResult emptyLookupResult = FhirLookupResult.fromBundle(new Bundle());
        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(emptyLookupResult);

        // Act

        // Assert
        assertThrows(ServiceException.class, () -> subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, null, null, null));
    }

    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void updateQuestionnaire_illegalStatusChange_throwsException(Enumerations.PublicationStatus currentStatus, Enumerations.PublicationStatus newStatus) throws Exception {
        // Arrange
        String newStatusParam = newStatus.toString();

        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, currentStatus);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);

        // Act

        // Assert
        assertThrows(ServiceException.class, () -> subject.updateQuestionnaire(QUESTIONNAIRE_ID_1, null, null, newStatusParam, null, null));
    }

    private static Stream<Arguments> updateQuestionnaire_illegalStatusChange_throwsException() {
        Map<Enumerations.PublicationStatus, List<Enumerations.PublicationStatus>> valid = new HashMap();
        valid.put(Enumerations.PublicationStatus.ACTIVE, List.of(Enumerations.PublicationStatus.ACTIVE, Enumerations.PublicationStatus.RETIRED));
        valid.put(Enumerations.PublicationStatus.DRAFT, List.of(Enumerations.PublicationStatus.DRAFT, Enumerations.PublicationStatus.ACTIVE));

        // compute and add illegal status changes
        Stream.Builder<Arguments> argumentBuilder = Stream.builder();
        for (Enumerations.PublicationStatus currentStatus : Enumerations.PublicationStatus.values()) {
            for (Enumerations.PublicationStatus newStatus : Enumerations.PublicationStatus.values()) {
                if (valid.containsKey(currentStatus) && valid.get(currentStatus).contains(newStatus)) {
                    continue;
                }
                else {
                    argumentBuilder.add(Arguments.of(currentStatus, newStatus));
                }
            }
        }
        return argumentBuilder.build();
    }

    private Questionnaire buildQuestionnaire(String questionnaireId) {
        return buildQuestionnaire(questionnaireId, Enumerations.PublicationStatus.ACTIVE);

    }

    private Questionnaire buildQuestionnaire(String questionnaireId, Enumerations.PublicationStatus status) {
        Questionnaire questionnaire = new Questionnaire();

        questionnaire.setId(questionnaireId);
        questionnaire.setStatus(status);

        return questionnaire;
    }

    private QuestionnaireModel buildQuestionnaireModel(String questionnaireId) {
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();

        return questionnaireModel;
    }
}