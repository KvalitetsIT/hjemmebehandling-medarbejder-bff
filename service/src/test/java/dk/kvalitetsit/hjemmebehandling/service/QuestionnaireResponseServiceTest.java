package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.fhir.*;
import dk.kvalitetsit.hjemmebehandling.fhir.comparator.QuestionnaireResponsePriorityComparator;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
    private QuestionnaireResponsePriorityComparator priorityComparator;

    @Mock
    private AccessValidator accessValidator;

    private static final String CAREPLAN_ID_1 = "CarePlan/careplan-1";
    private static final String ORGANIZATION_ID_1 = "Organization/organization-1";
    private static final String ORGANIZATION_ID_2 = "Organization/organization-2";
    private static final String PATIENT_ID = "Patiient/patient-1";
    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";
    private static final String QUESTIONNAIRE_ID_2 = "Questionnaire/questionnaire-2";
    private static final String QUESTIONNAIRE_ID_3 = "Questionnaire/questionnaire-3";
    private static final String QUESTIONNAIRE_RESPONSE_ID_1 = "QuestionnaireResponse/questionnaireresponse-1";
    private static final String QUESTIONNAIRE_RESPONSE_ID_2 = "QuestionnaireResponse/questionnaireresponse-2";
    private static final String QUESTIONNAIRE_RESPONSE_ID_3 = "QuestionnaireResponse/questionnaireresponse-3";

    @Test
    public void getQuestionnaireResponses_responsesPresent_returnsResponses() throws Exception {
        // Arrange
        String carePlanId = CAREPLAN_ID_1;
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        QuestionnaireResponse response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(response);
        Mockito.when(fhirClient.lookupQuestionnaireResponses(carePlanId, questionnaireIds)).thenReturn(lookupResult);

        QuestionnaireResponseModel responseModel = new QuestionnaireResponseModel();
        Mockito.when(fhirMapper.mapQuestionnaireResponse(response, lookupResult)).thenReturn(responseModel);

        // Act
        PageDetails pageDetails = new PageDetails(1,5);
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(carePlanId, questionnaireIds,pageDetails);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(responseModel));
    }

    @Test
    public void getQuestionnaireResponses_responsesMissing_returnsEmptyList() throws Exception {
        // Arrange
        String carePlanId = CAREPLAN_ID_1;
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        Mockito.when(fhirClient.lookupQuestionnaireResponses(carePlanId, questionnaireIds)).thenReturn(FhirLookupResult.fromResources());

        // Act
        PageDetails pageDetails = new PageDetails(1,0);
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(carePlanId, questionnaireIds,pageDetails);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void getQuestionnaireResponses_accessViolation_throwsException() throws Exception {
        // Arrange
        String carePlanId = CAREPLAN_ID_1;
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        QuestionnaireResponse response = new QuestionnaireResponse();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(response);
        Mockito.when(fhirClient.lookupQuestionnaireResponses(carePlanId, questionnaireIds)).thenReturn(lookupResult);

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(response));

        // Act

        // Assert
        PageDetails pageDetails = new PageDetails(1,0);
        assertThrows(AccessValidationException.class, () -> subject.getQuestionnaireResponses(carePlanId, questionnaireIds,pageDetails));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesPresent_returnsResponses() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponse response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(response);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(lookupResult);

        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirMapper.mapQuestionnaireResponse(response, lookupResult)).thenReturn(model);

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(1, result.size());
        assertEquals(model, result.get(0));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesMissing_returnsEmptyList() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(FhirLookupResult.fromResources());

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_multipleEntriesForPatient_sameQuestionnaire() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponse firstResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID);
        QuestionnaireResponse secondResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_1, PATIENT_ID);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(firstResponse, secondResponse);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(lookupResult);

        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirMapper.mapQuestionnaireResponse(secondResponse, lookupResult)).thenReturn(model);

        // Impose that the second response is greater than the first.
        Mockito.when(priorityComparator.compare(firstResponse, secondResponse)).thenReturn(-1);

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

        QuestionnaireResponse firstResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID);
        QuestionnaireResponse secondResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_2, PATIENT_ID);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(firstResponse, secondResponse);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(lookupResult);

        Mockito.when(fhirMapper.mapQuestionnaireResponse(firstResponse, lookupResult)).thenReturn(new QuestionnaireResponseModel());
        Mockito.when(fhirMapper.mapQuestionnaireResponse(secondResponse, lookupResult)).thenReturn(new QuestionnaireResponseModel());

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

        QuestionnaireResponse response1 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID);
        QuestionnaireResponse response2 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_2, PATIENT_ID);
        QuestionnaireResponse response3 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_3, QUESTIONNAIRE_ID_3, PATIENT_ID);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(response1, response2, response3);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(lookupResult);

        Mockito.when(fhirMapper.mapQuestionnaireResponse(Mockito.any(), Mockito.any())).thenReturn(new QuestionnaireResponseModel());

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

        QuestionnaireResponse response1 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID);
        QuestionnaireResponse response2 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_2, PATIENT_ID);
        QuestionnaireResponse response3 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_3, QUESTIONNAIRE_ID_3, PATIENT_ID);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(response1, response2, response3);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(lookupResult);

        Mockito.when(fhirMapper.mapQuestionnaireResponse(Mockito.any(), Mockito.any())).thenReturn(new QuestionnaireResponseModel());

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

        Mockito.when(fhirClient.lookupQuestionnaireResponseById(id)).thenReturn(FhirLookupResult.fromResources());

        // Act

        // Assert
        assertThrows(ServiceException.class, () -> subject.updateExaminationStatus(id, status));
    }

    @Test
    public void updateExaminationStatus_accessViolation_throwsException() throws Exception {
        // Arrange
        String id = "questionnaireresponse-1";
        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;

        QuestionnaireResponse response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID, ORGANIZATION_ID_2);
        Mockito.when(fhirClient.lookupQuestionnaireResponseById(id)).thenReturn(FhirLookupResult.fromResource(response));

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(response);

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.updateExaminationStatus(id, status));
    }

    @Test
    public void updateExaminationStatus_successfulUpdate() throws Exception {
        // Arrange
        String id = "questionnaireresponse-1";
        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;

        QuestionnaireResponse response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID, ORGANIZATION_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(response);
        Mockito.when(fhirClient.lookupQuestionnaireResponseById(id)).thenReturn(lookupResult);

        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirMapper.mapQuestionnaireResponse(response, lookupResult)).thenReturn(model);
        Mockito.when(fhirMapper.mapQuestionnaireResponseModel(model)).thenReturn(response);

        Mockito.doNothing().when(fhirClient).updateQuestionnaireResponse(response);

        // Act
        subject.updateExaminationStatus(id, status);

        // Assert
        assertEquals(status, model.getExaminationStatus());
    }

    private QuestionnaireResponse buildQuestionnaireResponse(String questionnaireResponseId, String questionnaireId, String patientId) {
        return buildQuestionnaireResponse(questionnaireResponseId, questionnaireId, patientId, ORGANIZATION_ID_1);
    }

    private QuestionnaireResponse buildQuestionnaireResponse(String questionnaireResponseId, String questionnaireId, String patientId, String organizationId) {
        QuestionnaireResponse response = new QuestionnaireResponse();

        response.setId(questionnaireResponseId);
        response.setQuestionnaire(questionnaireId);
        response.setSubject(new Reference(patientId));
        response.addExtension(Systems.ORGANIZATION, new Reference(organizationId));

        return response;
    }
}