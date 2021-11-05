package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireResponseServiceTest {
    @InjectMocks
    private QuestionnaireResponseService subject;

    @Mock
    private FhirClient fhirClient;

    @Mock
    private FhirMapper fhirMapper;

    @Mock
    private FhirObjectBuilder fhirObjectBuilder;

    private static final String PATIENT_ID = "patient-1";
    private static final String QUESTIONNAIRE_ID = "questionnaire-1";

    @Test
    public void getQuestionnaireResponses_responsesPresent_returnsResponses() throws Exception {
        // Arrange
        String cpr = "0101010101";
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID);

        QuestionnaireResponse response1 = new QuestionnaireResponse();
        response1.setQuestionnaire(QUESTIONNAIRE_ID);
        response1.setSubject(new Reference(PATIENT_ID));
        QuestionnaireResponseModel responseModel1 = new QuestionnaireResponseModel();

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(QUESTIONNAIRE_ID);
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);

        Mockito.when(fhirClient.lookupQuestionnaireResponses(cpr, questionnaireIds)).thenReturn(List.of(response1));
        Mockito.when(fhirClient.lookupQuestionnaires(Set.of(QUESTIONNAIRE_ID))).thenReturn(List.of(questionnaire));
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
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID);

        Mockito.when(fhirClient.lookupQuestionnaireResponses(cpr, questionnaireIds)).thenReturn(List.of());

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(cpr, questionnaireIds);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesPresent_returnsResponses() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setQuestionnaire(QUESTIONNAIRE_ID);
        response.setSubject(new Reference(PATIENT_ID));
        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByExaminationStatus(statuses)).thenReturn(List.of(response));

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(QUESTIONNAIRE_ID);
        Mockito.when(fhirClient.lookupQuestionnaires(Set.of(QUESTIONNAIRE_ID))).thenReturn(List.of(questionnaire));

        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Mockito.when(fhirClient.lookupPatientsById(Set.of(PATIENT_ID))).thenReturn(List.of(patient));

        Mockito.when(fhirMapper.mapQuestionnaireResponse(response, questionnaire, patient)).thenReturn(model);

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(1, result.size());
        assertEquals(model, result.get(0));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_questionnaireMissing_throwsException() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setQuestionnaire(QUESTIONNAIRE_ID);
        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByExaminationStatus(statuses)).thenReturn(List.of(response));

        Mockito.when(fhirClient.lookupQuestionnaires(Set.of(QUESTIONNAIRE_ID))).thenReturn(List.of());

        // Act

        // Assert
        assertThrows(IllegalStateException.class, () -> subject.getQuestionnaireResponsesByStatus(statuses));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_patientMissing_throwsException() {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setQuestionnaire(QUESTIONNAIRE_ID);
        response.setSubject(new Reference(PATIENT_ID));
        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByExaminationStatus(statuses)).thenReturn(List.of(response));

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(QUESTIONNAIRE_ID);
        Mockito.when(fhirClient.lookupQuestionnaires(Set.of(QUESTIONNAIRE_ID))).thenReturn(List.of(questionnaire));

        Mockito.when(fhirClient.lookupPatientsById(Set.of(PATIENT_ID))).thenReturn(List.of());

        // Act

        // Assert
        assertThrows(IllegalStateException.class, () -> subject.getQuestionnaireResponsesByStatus(statuses));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesMissing_returnsEmptyList() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        Mockito.when(fhirClient.lookupQuestionnaireResponsesByExaminationStatus(statuses)).thenReturn(List.of());

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void updateExaminationStatus_resourceNotFound_throwsException() throws Exception {
        // Arrange
        String id = "questionnaireresponse-1";
        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;

        Mockito.when(fhirClient.lookupQuestionnaireResponseById(id)).thenReturn(Optional.empty());

        // Act

        // Assert
        assertThrows(ServiceException.class, () -> subject.updateExaminationStatus(id, status));
    }

    @Test
    public void updateExaminationStatus_successfulUpdate() throws Exception {
        // Arrange
        String id = "questionnaireresponse-1";
        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;

        QuestionnaireResponse response = new QuestionnaireResponse();
        Mockito.when(fhirClient.lookupQuestionnaireResponseById(id)).thenReturn(Optional.of(response));

        Mockito.doNothing().when(fhirClient).updateQuestionnaireResponse(response);

        // Act
        subject.updateExaminationStatus(id, status);

        // Assert
        Mockito.verify(fhirObjectBuilder).updateExaminationStatusForQuestionnaireResponse(response, status);
    }
}