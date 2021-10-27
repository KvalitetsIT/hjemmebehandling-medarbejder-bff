package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireResponseServiceTest {
    @InjectMocks
    private QuestionnaireResponseService subject;

    @Mock
    private FhirClient fhirClient;

    @Mock
    private FhirMapper fhirMapper;

    @Test
    public void getQuestionnaireResponses_responsesPresent_returnsResponses() throws Exception {
        // Arrange
        String cpr = "0101010101";
        List<String> questionnaireIds = List.of("questionnaire-1");

        QuestionnaireResponse response1 = new QuestionnaireResponse();
        response1.setQuestionnaire("questionnaire-1");
        QuestionnaireResponseModel responseModel1 = new QuestionnaireResponseModel();

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId("questionnaire-1");
        Patient patient = new Patient();

        Mockito.when(fhirClient.lookupQuestionnaireResponses(cpr, questionnaireIds)).thenReturn(List.of(response1));
        Mockito.when(fhirClient.lookupQuestionnaires(questionnaireIds)).thenReturn(List.of(questionnaire));
        Mockito.when(fhirClient.lookupPatientByCpr(cpr)).thenReturn(Optional.of(patient));

        Mockito.when(fhirMapper.mapQuestionnaireResponse(response1, questionnaire, patient)).thenReturn(responseModel1);

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(cpr, questionnaireIds);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(responseModel1));
    }

    @Test
    public void getQuestionnaireResponses_responsesMissing_returnsEmptyList() throws Exception {
        // Arrange
        String cpr = "0101010101";
        List<String> questionnaireIds = List.of("questionnaire-1");

        Mockito.when(fhirClient.lookupQuestionnaireResponses(cpr, questionnaireIds)).thenReturn(List.of());

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(cpr, questionnaireIds);

        // Assert
        assertEquals(0, result.size());
    }
}