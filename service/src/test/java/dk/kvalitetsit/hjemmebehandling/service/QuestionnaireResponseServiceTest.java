package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.comparator.QuestionnaireResponsePriorityComparator;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteQuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireResponseServiceTest {
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

    @InjectMocks
    private ConcreteQuestionnaireResponseService subject;
    @Mock
    private ClientAdaptor fhirClient;
    @Mock
    private QuestionnaireResponsePriorityComparator priorityComparator;
    @Mock
    private AccessValidator accessValidator;

    @Test
    public void getQuestionnaireResponses_responsesPresent_returnsResponses() throws Exception {
        String carePlanId = CAREPLAN_ID_1;
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);

        Mockito.when(fhirClient.lookupQuestionnaireResponses(carePlanId, questionnaireIds)).thenReturn(List.of(response));
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(questionnaireIds)).thenReturn(null);

        Pagination pagination = new Pagination(1, 5);
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(carePlanId, questionnaireIds, pagination);

        ArgumentCaptor<QuestionnaireResponseModel> captor = ArgumentCaptor.forClass(QuestionnaireResponseModel.class);
        verify(fhirClient, times(1)).updateQuestionnaireResponse(captor.capture());


        assertEquals(1, result.size());

    }

    @Test
    public void getQuestionnaireResponses_ReturnOneItem_WhenPagesizeIsOne() throws Exception {
        ConcreteQuestionnaireResponseService qrservice = new ConcreteQuestionnaireResponseService(fhirClient, null, accessValidator);

        QuestionnaireResponseModel response = QuestionnaireResponseModel.builder().build();

        Mockito.when(fhirClient.lookupQuestionnaireResponses(null, null)).thenReturn(List.of(response));

        Pagination pagination = new Pagination(1, 1);
        List<QuestionnaireResponseModel> result = qrservice.getQuestionnaireResponses(null, null, pagination);

        assertEquals(1, result.size());
    }

    @Test
    public void getQuestionnaireResponses_ReturnSortedPages_WhenTwoPages() throws Exception {
        ConcreteQuestionnaireResponseService qrservice = new ConcreteQuestionnaireResponseService(fhirClient, null, accessValidator);

        QuestionnaireResponseModel response1 = QuestionnaireResponseModel.builder()
                .id(new QualifiedId("1", ResourceType.QuestionnaireResponse))
                .answered(new Date(1, Calendar.JANUARY, 1).toInstant())
                .build();

        QuestionnaireResponseModel response2 = QuestionnaireResponseModel.builder()
                .id(new QualifiedId("2", ResourceType.QuestionnaireResponse))
                .answered(new Date(2, Calendar.FEBRUARY, 2).toInstant())
                .build();


        QuestionnaireResponseModel response3 = QuestionnaireResponseModel.builder()
                .id(new QualifiedId("3", ResourceType.QuestionnaireResponse))
                .answered(new Date(3, Calendar.MARCH, 3).toInstant())
                .build();

        QuestionnaireResponseModel response4 = QuestionnaireResponseModel.builder()
                .id(new QualifiedId("4", ResourceType.QuestionnaireResponse))
                .answered(new Date(4, Calendar.APRIL, 4).toInstant())
                .build();

        Mockito.when(fhirClient.lookupQuestionnaireResponses(null, null)).thenReturn(List.of(response1, response3, response4, response2));
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(null)).thenReturn(List.of());

        Pagination pagination1 = new Pagination(1, 2);
        List<QuestionnaireResponseModel> result1 = qrservice.getQuestionnaireResponses(null, null, pagination1);

        Pagination pagination2 = new Pagination(2, 2);
        List<QuestionnaireResponseModel> result2 = qrservice.getQuestionnaireResponses(null, null, pagination2);

        assertEquals(2, result1.size());
        assertEquals(2, result2.size());

        assertEquals("4", result1.get(0).id().id());
        assertEquals("3", result1.get(1).id().id());
        assertEquals("2", result2.get(0).id().id());
        assertEquals("1", result2.get(1).id().id());
    }

    QuestionnaireResponseModel questionnaireResponseToQuestionnaireResponseModel(QuestionnaireResponse questionnaireResponse) {
        return QuestionnaireResponseModel
                .builder()
                .id(new QualifiedId("QuestionnaireResponse/" + questionnaireResponse.getId()))
                .build();
    }

    @Test
    public void getQuestionnaireResponses_ReturnZeroItem_WhenPagesizeIsZero() throws Exception {
        ConcreteQuestionnaireResponseService qrservice = new ConcreteQuestionnaireResponseService(fhirClient, null, accessValidator);

        QuestionnaireResponseModel response = QuestionnaireResponseModel.builder().build();

        Mockito.when(fhirClient.lookupQuestionnaireResponses(null, null)).thenReturn(List.of(response));

        Pagination pagination = new Pagination(1, 0);
        List<QuestionnaireResponseModel> result = qrservice.getQuestionnaireResponses(null, null, pagination);

        assertEquals(0, result.size());
    }

    @Test
    public void getQuestionnaireResponses_ReturnZeroItem_WhenOnSecondPage() throws Exception {
        ConcreteQuestionnaireResponseService qrservice = new ConcreteQuestionnaireResponseService(fhirClient, null, accessValidator);

        QuestionnaireResponseModel response = QuestionnaireResponseModel.builder().build();

        Mockito.when(fhirClient.lookupQuestionnaireResponses(null, null)).thenReturn(List.of(response));

        Pagination pagination = new Pagination(2, 1);
        List<QuestionnaireResponseModel> result = qrservice.getQuestionnaireResponses(null, null, pagination);

        assertEquals(0, result.size());
    }

    @Test
    public void getQuestionnaireResponses_responsesMissing_returnsEmptyList() throws Exception {
        String carePlanId = CAREPLAN_ID_1;
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        Mockito.when(fhirClient.lookupQuestionnaireResponses(carePlanId, questionnaireIds)).thenReturn(List.of());

        Pagination pagination = new Pagination(1, 1);
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(carePlanId, questionnaireIds, pagination);

        assertEquals(0, result.size());
    }

    @Test
    public void getQuestionnaireResponses_accessViolation_throwsException() throws Exception {
        String carePlanId = CAREPLAN_ID_1;
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        QuestionnaireResponseModel response = QuestionnaireResponseModel.builder().build();

        Mockito.when(fhirClient.lookupQuestionnaireResponses(carePlanId, questionnaireIds)).thenReturn(List.of(response));

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(response));

        Pagination pagination = new Pagination(1, 0);
        assertThrows(AccessValidationException.class, () -> subject.getQuestionnaireResponses(carePlanId, questionnaireIds, pagination));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesPresent_returnsResponses() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(response));
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(List.of())).thenReturn(null);

        QuestionnaireResponseModel model = QuestionnaireResponseModel.builder().build();

        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        assertEquals(1, result.size());
        assertEquals(model, result.getFirst());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesMissing_returnsEmptyList() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of());

        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        assertEquals(0, result.size());
    }


    @Test
    public void getQuestionnaireResponsesByStatus_multipleEntriesForPatient_sameQuestionnaire() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponseModel firstResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);
        QuestionnaireResponseModel secondResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_1);

        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(firstResponse, secondResponse));

        List<String> ids = List.of(firstResponse.questionnaireId().id(), secondResponse.questionnaireId().id());

        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(ids)).thenReturn(null);

        List<Questionnaire> historicalQuestionnaires = null;
        QuestionnaireResponseModel model = QuestionnaireResponseModel.builder().build();

       // Mockito.when(priorityComparator.compare(firstResponse, secondResponse)).thenReturn(1);

        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        assertEquals(1, result.size());
        assertEquals(model, result.getFirst());
    }


    @Test
    public void getQuestionnaireResponsesByStatus_multipleEntriesForPatient_differentQuestionnaires() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponseModel firstResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);
        QuestionnaireResponseModel secondResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_2);

        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(firstResponse, secondResponse));
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(List.of())).thenReturn(null);
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANIZATION_ID_1);

        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        assertEquals(2, result.size());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_handlesPagingParameters_page1() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        Pagination pagination = new Pagination(1, 2);

        QuestionnaireResponseModel response1 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);
        QuestionnaireResponseModel response2 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_2);
        QuestionnaireResponseModel response3 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_3, QUESTIONNAIRE_ID_3);

        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(response1, response2, response3));
        List<String> ids = Stream.of(response1, response2, response3).map(questionnaire -> questionnaire.id().toString()).toList();
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(ids)).thenReturn(List.of());

        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses, pagination);

        assertEquals(2, result.size());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_handlesPagingParameters_page2() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        Pagination pagination = new Pagination(2, 2);
        QuestionnaireResponseModel response1 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);
        QuestionnaireResponseModel response2 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_2);
        QuestionnaireResponseModel response3 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_3, QUESTIONNAIRE_ID_3);

        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(response1, response2, response3));
        Mockito.when(fhirClient.lookupVersionsOfQuestionnaireById(List.of())).thenReturn(List.of());

        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses, pagination);


        assertEquals(1, result.size());
    }

    @Test
    public void updateExaminationStatus_resourceNotFound_throwsException() throws Exception {
        String id = "questionnaireresponse-1";
        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;
        Mockito.when(fhirClient.lookupQuestionnaireResponseById(id)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> subject.updateExaminationStatus(id, status));
    }

    @Test
    public void updateExaminationStatus_accessViolation_throwsException() throws Exception {
        String id = "questionnaireresponse-1";
        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;

        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, ORGANIZATION_ID_2);
        Mockito.when(fhirClient.lookupQuestionnaireResponseById(id)).thenReturn(Optional.of(response));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(response);

        assertThrows(AccessValidationException.class, () -> subject.updateExaminationStatus(id, status));
    }

    @Test
    public void updateExaminationStatus_successfulUpdate() throws Exception {
        String id = "questionnaireresponse-1";
        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;

        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, ORGANIZATION_ID_1);

        Mockito.when(fhirClient.lookupQuestionnaireResponseById(id)).thenReturn(Optional.of(response));

        subject.updateExaminationStatus(id, status);

        ArgumentCaptor<QuestionnaireResponseModel> captor = ArgumentCaptor.forClass(QuestionnaireResponseModel.class);

        verify(fhirClient, times(1)).updateQuestionnaireResponse(captor.capture());

        assertEquals(status, captor.getValue().examinationStatus());
    }

    private QuestionnaireResponseModel buildQuestionnaireResponse(String questionnaireResponseId, String questionnaireId) {
        return buildQuestionnaireResponse(questionnaireResponseId, questionnaireId, ORGANIZATION_ID_1);
    }

    private QuestionnaireResponseModel buildQuestionnaireResponse(String questionnaireResponseId, String questionnaireId, String organizationId) {
        return QuestionnaireResponseModel.builder()
                .id(new QualifiedId(questionnaireResponseId))
                .questionnaireId(new QualifiedId(questionnaireId))
                .organizationId(organizationId)
                .build();

    }
}