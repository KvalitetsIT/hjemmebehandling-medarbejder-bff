package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.comparator.QuestionnaireResponsePriorityComparator;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
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

    @Mock
    private QuestionnaireResponsePriorityComparator priorityComparator;

    private static final String PATIENT_ID = "patient-1";
    private static final String QUESTIONNAIRE_ID_1 = "questionnaire-1";
    private static final String QUESTIONNAIRE_ID_2 = "questionnaire-2";
    private static final String QUESTIONNAIRE_ID_3 = "questionnaire-3";

    @Test
    public void getQuestionnaireResponses_responsesPresent_returnsResponses() throws Exception {
        // Arrange
        String cpr = "0101010101";
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        QuestionnaireResponse response1 = new QuestionnaireResponse();
        response1.setQuestionnaire(QUESTIONNAIRE_ID_1);
        response1.setSubject(new Reference(PATIENT_ID));
        QuestionnaireResponseModel responseModel1 = new QuestionnaireResponseModel();

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(QUESTIONNAIRE_ID_1);
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);

        Mockito.when(fhirClient.lookupQuestionnaireResponses(cpr, questionnaireIds)).thenReturn(List.of(response1));
        Mockito.when(fhirClient.lookupQuestionnaires(Set.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
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
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

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
        response.setQuestionnaire(QUESTIONNAIRE_ID_1);
        response.setSubject(new Reference(PATIENT_ID));
        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(response));

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(QUESTIONNAIRE_ID_1);
        Mockito.when(fhirClient.lookupQuestionnaires(Set.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));

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
        response.setQuestionnaire(QUESTIONNAIRE_ID_1);
        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(response));

        Mockito.when(fhirClient.lookupQuestionnaires(Set.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of());

        // Act

        // Assert
        assertThrows(IllegalStateException.class, () -> subject.getQuestionnaireResponsesByStatus(statuses));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_patientMissing_throwsException() {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setQuestionnaire(QUESTIONNAIRE_ID_1);
        response.setSubject(new Reference(PATIENT_ID));
        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(response));

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(QUESTIONNAIRE_ID_1);
        Mockito.when(fhirClient.lookupQuestionnaires(Set.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));

        Mockito.when(fhirClient.lookupPatientsById(Set.of(PATIENT_ID))).thenReturn(List.of());

        // Act

        // Assert
        assertThrows(IllegalStateException.class, () -> subject.getQuestionnaireResponsesByStatus(statuses));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesMissing_returnsEmptyList() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of());

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_multipleEntriesForPatient_sameQuestionnaire() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponse firstResponse = new QuestionnaireResponse();
        firstResponse.setQuestionnaire(QUESTIONNAIRE_ID_1);
        firstResponse.setSubject(new Reference(PATIENT_ID));

        QuestionnaireResponse secondResponse = new QuestionnaireResponse();
        secondResponse.setQuestionnaire(QUESTIONNAIRE_ID_1);
        secondResponse.setSubject(new Reference(PATIENT_ID));
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(firstResponse, secondResponse));

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(QUESTIONNAIRE_ID_1);
        Mockito.when(fhirClient.lookupQuestionnaires(Set.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));

        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Mockito.when(fhirClient.lookupPatientsById(Set.of(PATIENT_ID))).thenReturn(List.of(patient));

        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirMapper.mapQuestionnaireResponse(firstResponse, questionnaire, patient)).thenReturn(model);

        Mockito.when(priorityComparator.compare(Mockito.any(), Mockito.any())).thenReturn(1);

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(1, result.size());
        assertEquals(model, result.get(0));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_multipleEntriesForPatient_differentQuestionnaires() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponse firstResponse = new QuestionnaireResponse();
        firstResponse.setQuestionnaire(QUESTIONNAIRE_ID_1);
        firstResponse.setSubject(new Reference(PATIENT_ID));

        QuestionnaireResponse secondResponse = new QuestionnaireResponse();
        secondResponse.setQuestionnaire(QUESTIONNAIRE_ID_2);
        secondResponse.setSubject(new Reference(PATIENT_ID));
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(firstResponse, secondResponse));

        Questionnaire questionnaire1 = new Questionnaire();
        questionnaire1.setId(QUESTIONNAIRE_ID_1);
        Questionnaire questionnaire2 = new Questionnaire();
        questionnaire2.setId(QUESTIONNAIRE_ID_2);
        Mockito.when(fhirClient.lookupQuestionnaires(Set.of(QUESTIONNAIRE_ID_1, QUESTIONNAIRE_ID_2))).thenReturn(List.of(questionnaire1, questionnaire2));

        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Mockito.when(fhirClient.lookupPatientsById(Set.of(PATIENT_ID))).thenReturn(List.of(patient));

        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirMapper.mapQuestionnaireResponse(firstResponse, questionnaire1, patient)).thenReturn(new QuestionnaireResponseModel());
        Mockito.when(fhirMapper.mapQuestionnaireResponse(secondResponse, questionnaire2, patient)).thenReturn(new QuestionnaireResponseModel());

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_handlesPagingParameters_page1() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        PageDetails pageDetails = new PageDetails(1, 2);

        QuestionnaireResponse response1 = buildQuestionnaireResponse(QUESTIONNAIRE_ID_1, PATIENT_ID);
        QuestionnaireResponse response2 = buildQuestionnaireResponse(QUESTIONNAIRE_ID_2, PATIENT_ID);
        QuestionnaireResponse response3 = buildQuestionnaireResponse(QUESTIONNAIRE_ID_3, PATIENT_ID);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(response1, response2, response3));

        setupQuestionnaires(QUESTIONNAIRE_ID_2, QUESTIONNAIRE_ID_3);
        setupPatient(PATIENT_ID);

        Mockito.when(fhirMapper.mapQuestionnaireResponse(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new QuestionnaireResponseModel());

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses, pageDetails);

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_handlesPagingParameters_page2() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        PageDetails pageDetails = new PageDetails(2, 2);

        QuestionnaireResponse response1 = buildQuestionnaireResponse(QUESTIONNAIRE_ID_1, PATIENT_ID);
        QuestionnaireResponse response2 = buildQuestionnaireResponse(QUESTIONNAIRE_ID_2, PATIENT_ID);
        QuestionnaireResponse response3 = buildQuestionnaireResponse(QUESTIONNAIRE_ID_3, PATIENT_ID);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(response1, response2, response3));

        setupQuestionnaires(QUESTIONNAIRE_ID_1);
        setupPatient(PATIENT_ID);

        Mockito.when(fhirMapper.mapQuestionnaireResponse(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new QuestionnaireResponseModel());

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses, pageDetails);

        // Assert
        assertEquals(1, result.size());
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

    private QuestionnaireResponse buildQuestionnaireResponse(String questionnaireId, String patientId) {
        QuestionnaireResponse response = new QuestionnaireResponse();

        response.setQuestionnaire(questionnaireId);
        response.setSubject(new Reference(patientId));

        return response;
    }

    private void setupQuestionnaires(String... questionnaireIds) {
        List<Questionnaire> questionnaires = new ArrayList<>();
        for(String questionnaireId : questionnaireIds) {
            Questionnaire questionnaire = new Questionnaire();
            questionnaire.setId(questionnaireId);
            questionnaires.add(questionnaire);
        }
        Mockito.when(fhirClient.lookupQuestionnaires(Set.of(questionnaireIds))).thenReturn(questionnaires);
    }

    private void setupPatient(String patientId) {
        Patient patient = new Patient();
        patient.setId(patientId);
        Mockito.when(fhirClient.lookupPatientsById(Set.of(patientId))).thenReturn(List.of(patient));
    }
}