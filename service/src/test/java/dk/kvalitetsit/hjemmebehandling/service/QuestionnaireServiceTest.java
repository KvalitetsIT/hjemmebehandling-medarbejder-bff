package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireServiceTest {
    @InjectMocks
    private QuestionnaireService subject;

    @Mock
    private FhirClient fhirClient;

    @Mock
    private FhirMapper fhirMapper;

    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";

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

    private QuestionnaireModel buildQuestionnaireModel(String questionnaireId) {
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();

        return questionnaireModel;
    }
}