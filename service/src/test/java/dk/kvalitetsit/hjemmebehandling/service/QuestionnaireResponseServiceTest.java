package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.fhir.*;
import dk.kvalitetsit.hjemmebehandling.fhir.comparator.QuestionnaireResponsePriorityComparator;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
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

import java.util.Calendar;
import java.util.Date;
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
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(questionnaireIds)).thenReturn(null);

        QuestionnaireResponseModel responseModel = new QuestionnaireResponseModel();
        Mockito.when(fhirMapper.mapQuestionnaireResponse(response, lookupResult, null)).thenReturn(responseModel);

        // Act
        PageDetails pageDetails = new PageDetails(1,5);
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(carePlanId, questionnaireIds,pageDetails);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(responseModel));
    }

    @Test
    public void getQuestionnaireResponses_ReturnOneItem_WhenPagesizeIsOne() throws Exception {
        //Arrange
        QuestionnaireResponseService qrservice = new QuestionnaireResponseService(fhirClient, fhirMapper, null, accessValidator);


        QuestionnaireResponse response = new QuestionnaireResponse();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(response);
        Mockito.when(fhirClient.lookupQuestionnaireResponses(null, null)).thenReturn(lookupResult);



        PageDetails pageDetails = new PageDetails(1,1);
        List<QuestionnaireResponseModel> result = qrservice.getQuestionnaireResponses(null, null,pageDetails);


        assertEquals(1, result.size());
    }
/*
    @Test
    public void getQuestionnaireResponses_ReturnSortedPages_WhenTwoPages() throws Exception {
        //Arrange
        QuestionnaireResponseService qrservice = new QuestionnaireResponseService(fhirClient, fhirMapper, null, accessValidator);

        QuestionnaireResponse response1 = new QuestionnaireResponse();
        response1.setAuthored(new Date(1,Calendar.JANUARY,1));
        response1.setId("1");

        QuestionnaireResponse response2 = new QuestionnaireResponse();
        response2.setAuthored(new Date(2, Calendar.FEBRUARY,2));
        response2.setId("2");

        QuestionnaireResponse response3 = new QuestionnaireResponse();
        response3.setAuthored(new Date(3,Calendar.MARCH,3));
        response3.setId("3");

        QuestionnaireResponse response4 = new QuestionnaireResponse();
        response4.setAuthored(new Date(4,Calendar.APRIL,4));
        response4.setId("4");

        FhirLookupResult lookupResult1 = FhirLookupResult.fromResources(response1,response3, response4, response2);

        Mockito.when(fhirClient.lookupQuestionnaireResponses(null, List.of())).thenReturn(lookupResult1);
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById( List.of())).thenReturn(List.of());

        Mockito.when(fhirMapper.mapQuestionnaireResponse(response1,lookupResult1, List.of())).thenReturn(questionnaireResponseToQuestionnaireResponseModel(response1));
        Mockito.when(fhirMapper.mapQuestionnaireResponse(response2,lookupResult1, List.of())).thenReturn(questionnaireResponseToQuestionnaireResponseModel(response2));
        Mockito.when(fhirMapper.mapQuestionnaireResponse(response3,lookupResult1, List.of())).thenReturn(questionnaireResponseToQuestionnaireResponseModel(response3));
        Mockito.when(fhirMapper.mapQuestionnaireResponse(response4,lookupResult1, List.of())).thenReturn(questionnaireResponseToQuestionnaireResponseModel(response4));

        //ACTION
        PageDetails pageDetails1 = new PageDetails(1,2);
        List<QuestionnaireResponseModel> result1 = qrservice.getQuestionnaireResponses(null, null,pageDetails1);

        PageDetails pageDetails2 = new PageDetails(2,2);
        List<QuestionnaireResponseModel> result2 = qrservice.getQuestionnaireResponses(null, null,pageDetails2);

        //ASSERT
        assertEquals(2, result1.size());
        assertEquals(2, result2.size());

        assertEquals("4",result1.get(0).getId().getId());
        assertEquals("3",result1.get(1).getId().getId());
        assertEquals("2",result2.get(0).getId().getId());
        assertEquals("1",result2.get(1).getId().getId());
    }
    QuestionnaireResponseModel questionnaireResponseToQuestionnaireResponseModel(QuestionnaireResponse questionnaireResponse){
        QuestionnaireResponseModel responseModel1 = new QuestionnaireResponseModel();
        responseModel1.setId(new QualifiedId("QuestionnaireResponse/"+questionnaireResponse.getId()));
        return responseModel1;
    }
*/
    @Test
    public void getQuestionnaireResponses_ReturnZeroItem_WhenPagesizeIsZero() throws Exception {
        //Arrange
        QuestionnaireResponseService qrservice = new QuestionnaireResponseService(fhirClient, fhirMapper, null, accessValidator);


        QuestionnaireResponse response = new QuestionnaireResponse();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(response);
        Mockito.when(fhirClient.lookupQuestionnaireResponses(null, null)).thenReturn(lookupResult);



        PageDetails pageDetails = new PageDetails(1,0);
        List<QuestionnaireResponseModel> result = qrservice.getQuestionnaireResponses(null, null,pageDetails);


        assertEquals(0, result.size());
    }

    @Test
    public void getQuestionnaireResponses_ReturnZeroItem_WhenOnSecondPage() throws Exception {
        //Arrange
        QuestionnaireResponseService qrservice = new QuestionnaireResponseService(fhirClient, fhirMapper, null, accessValidator);


        QuestionnaireResponse response = new QuestionnaireResponse();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(response);
        Mockito.when(fhirClient.lookupQuestionnaireResponses(null, null)).thenReturn(lookupResult);

        PageDetails pageDetails = new PageDetails(2,1);
        List<QuestionnaireResponseModel> result = qrservice.getQuestionnaireResponses(null, null,pageDetails);

        assertEquals(0, result.size());
    }

    @Test
    public void getQuestionnaireResponses_responsesMissing_returnsEmptyList() throws Exception {
        // Arrange
        String carePlanId = CAREPLAN_ID_1;
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        Mockito.when(fhirClient.lookupQuestionnaireResponses(carePlanId, questionnaireIds)).thenReturn(FhirLookupResult.fromResources());

        // Act
        PageDetails pageDetails = new PageDetails(1,1);
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
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(List.of())).thenReturn(null);

        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirMapper.mapQuestionnaireResponse(response, lookupResult, null)).thenReturn(model);

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
    /*

    @Test
    public void getQuestionnaireResponsesByStatus_multipleEntriesForPatient_sameQuestionnaire() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponse firstResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID);
        QuestionnaireResponse secondResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_1, PATIENT_ID);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(firstResponse, secondResponse);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(lookupResult);
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(null);

        QuestionnaireResponseModel model = new QuestionnaireResponseModel();
        Mockito.when(fhirMapper.mapQuestionnaireResponse(secondResponse, lookupResult)).thenReturn(model);

        // Impose that the second response is greater than the first.
        Mockito.when(priorityComparator.compare(firstResponse, secondResponse)).thenReturn(1);

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(1, result.size());
        assertEquals(model, result.get(0));
    }
    */

    @Test
    public void getQuestionnaireResponsesByStatus_multipleEntriesForPatient_differentQuestionnaires() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponse firstResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID);
        QuestionnaireResponse secondResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_2, PATIENT_ID);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(firstResponse, secondResponse);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(lookupResult);
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(List.of())).thenReturn(null);

        Mockito.when(fhirMapper.mapQuestionnaireResponse(firstResponse, lookupResult, null)).thenReturn(new QuestionnaireResponseModel());
        Mockito.when(fhirMapper.mapQuestionnaireResponse(secondResponse, lookupResult, null)).thenReturn(new QuestionnaireResponseModel());

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(2, result.size());
    }
/*
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
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(List.of())).thenReturn(List.of());

        Mockito.when(fhirMapper.mapQuestionnaireResponse(Mockito.any(), Mockito.any(), List.of())).thenReturn(new QuestionnaireResponseModel());

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
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(List.of())).thenReturn(null);

        Mockito.when(fhirMapper.mapQuestionnaireResponse(Mockito.any(), Mockito.any(), null)).thenReturn(new QuestionnaireResponseModel());

        // Act
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses, pageDetails);

        // Assert
        assertEquals(1, result.size());
    }
*/
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