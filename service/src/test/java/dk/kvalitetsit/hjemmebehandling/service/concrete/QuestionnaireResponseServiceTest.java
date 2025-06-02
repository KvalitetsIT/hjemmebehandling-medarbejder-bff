package dk.kvalitetsit.hjemmebehandling.service.concrete;

import dk.kvalitetsit.hjemmebehandling.fhir.QuestionnaireResponseModelPriorityComparator;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.PractitionerRepository;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireResponseRepository;
import dk.kvalitetsit.hjemmebehandling.repository.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteQuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
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

import static dk.kvalitetsit.hjemmebehandling.MockFactory.buildQuestionnaireResponse;
import static dk.kvalitetsit.hjemmebehandling.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireResponseServiceTest {

    @InjectMocks
    private ConcreteQuestionnaireResponseService subject;

    @Mock
    private QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository;

    @Mock
    private QuestionnaireRepository<QuestionnaireModel> questionnaireRepository;

    @Mock
    private PractitionerRepository<PractitionerModel> practitionerRepository;


    @Mock
    private QuestionnaireResponseModelPriorityComparator priorityComparator;

    @Mock
    private AccessValidator<QuestionnaireResponseModel> accessValidator;


    @Test
    public void getQuestionnaireResponses_responsesPresent_returnsResponses() throws Exception {
        QualifiedId.CarePlanId carePlanId = CAREPLAN_ID_1;
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);

        Mockito.when(questionnaireResponseRepository.fetch(carePlanId, questionnaireIds)).thenReturn(List.of(response));
        Mockito.when(questionnaireRepository.history(questionnaireIds)).thenReturn(null);

        Pagination pagination = new Pagination(0, 5);
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(carePlanId, questionnaireIds, pagination);

        assertEquals(1, result.size());
        assertTrue(result.contains(response));
    }

    @Test
    public void getQuestionnaireResponses_ReturnOneItem_WhenPagesizeIsOne() throws Exception {
        QuestionnaireResponseModel response = QuestionnaireResponseModel.builder().build();

        Mockito.when(questionnaireResponseRepository.fetch((QualifiedId.CarePlanId) null, null)).thenReturn(List.of(response));

        Pagination pagination = new Pagination(0, 1);
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(null, null, pagination);

        assertEquals(1, result.size());
    }

    @Test
    public void getQuestionnaireResponses_ReturnSortedPages_WhenTwoPages() throws Exception {


        QuestionnaireResponseModel response1 = QuestionnaireResponseModel.builder()
                .id(new QualifiedId.QuestionnaireResponseId("1"))
                .answered(new Date(1, Calendar.JANUARY, 1).toInstant())
                .build();

        QuestionnaireResponseModel response2 = QuestionnaireResponseModel.builder()
                .id(new QualifiedId.QuestionnaireResponseId("2"))
                .answered(new Date(2, Calendar.FEBRUARY, 2).toInstant())
                .build();


        QuestionnaireResponseModel response3 = QuestionnaireResponseModel.builder()
                .id(new QualifiedId.QuestionnaireResponseId("3"))
                .answered(new Date(3, Calendar.MARCH, 3).toInstant())
                .build();

        QuestionnaireResponseModel response4 = QuestionnaireResponseModel.builder()
                .id(new QualifiedId.QuestionnaireResponseId("4"))
                .answered(new Date(4, Calendar.APRIL, 4).toInstant())
                .build();

        Mockito.when(questionnaireResponseRepository.fetch((QualifiedId.CarePlanId) null, null)).thenReturn(List.of(response1, response3, response4, response2));
        Mockito.when(questionnaireRepository.history((List<QualifiedId.QuestionnaireId>) null)).thenReturn(List.of());

        Pagination pagination1 = new Pagination(0, 2);
        List<QuestionnaireResponseModel> result1 = subject.getQuestionnaireResponses(null, null, pagination1);

        Pagination pagination2 = new Pagination(1, 2);
        List<QuestionnaireResponseModel> result2 = subject.getQuestionnaireResponses(null, null, pagination2);

        assertEquals(2, result1.size());
        assertEquals(2, result2.size());

        assertEquals("4", result1.get(0).id().unqualified());
        assertEquals("3", result1.get(1).id().unqualified());
        assertEquals("2", result2.get(0).id().unqualified());
        assertEquals("1", result2.get(1).id().unqualified());
    }

    @Test
    public void getQuestionnaireResponses_ReturnZeroItem_WhenPagesizeIsZero() throws Exception {
        QuestionnaireResponseModel response = QuestionnaireResponseModel.builder().build();

        Mockito.when(questionnaireResponseRepository.fetch((QualifiedId.CarePlanId) null, null)).thenReturn(List.of(response));

        Pagination pagination = new Pagination(1, 0);
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(null, null, pagination);

        assertEquals(0, result.size());
    }

    @Test
    public void getQuestionnaireResponses_ReturnZeroItem_WhenOnSecondPage() throws Exception {
        QuestionnaireResponseModel response = QuestionnaireResponseModel.builder().build();

        Mockito.when(questionnaireResponseRepository.fetch((QualifiedId.CarePlanId) null, null)).thenReturn(List.of(response));

        Pagination pagination = new Pagination(2, 1);
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(null, null, pagination);

        assertEquals(0, result.size());
    }

    @Test
    public void getQuestionnaireResponses_responsesMissing_returnsEmptyList() throws Exception {
        QualifiedId.CarePlanId carePlanId = CAREPLAN_ID_1;
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        Mockito.when(questionnaireResponseRepository.fetch(carePlanId, questionnaireIds)).thenReturn(List.of());

        Pagination pagination = new Pagination(1, 1);
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(carePlanId, questionnaireIds, pagination);

        assertEquals(0, result.size());
    }


    @Test
    public void getQuestionnaireResponsesByStatus_responsesPresent_returnsResponses() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);
        Mockito.when(questionnaireResponseRepository.fetchByStatus(statuses)).thenReturn(List.of(response));
        Mockito.when(questionnaireRepository.history(List.of(response.questionnaireId()))).thenReturn(null);

        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        assertEquals(1, result.size());
        assertEquals(response, result.getFirst());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesMissing_returnsEmptyList() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        Mockito.when(questionnaireResponseRepository.fetchByStatus(statuses)).thenReturn(List.of());
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);
        assertEquals(0, result.size());
    }


    @Test
    public void getQuestionnaireResponsesByStatus_multipleEntriesForPatient_sameQuestionnaire() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponseModel firstResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);
        QuestionnaireResponseModel secondResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_1);

        Mockito.when(questionnaireResponseRepository.fetchByStatus(statuses)).thenReturn(List.of(firstResponse, secondResponse));

        List<QualifiedId.QuestionnaireId> ids = List.of(firstResponse.questionnaireId(), secondResponse.questionnaireId());

        Mockito.when(questionnaireRepository.history(ids)).thenReturn(null);


        Mockito.when(priorityComparator.compare(firstResponse, secondResponse)).thenReturn(1);

        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        assertEquals(1, result.size());
        assertEquals(secondResponse, result.getFirst());
    }


    @Test
    public void getQuestionnaireResponsesByStatus_multipleEntriesForPatient_differentQuestionnaires() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponseModel firstResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);
        QuestionnaireResponseModel secondResponse = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_2);

        Mockito.when(questionnaireResponseRepository.fetchByStatus(statuses)).thenReturn(List.of(firstResponse, secondResponse));
        Mockito.when(questionnaireRepository.history(List.of(firstResponse.questionnaireId(), secondResponse.questionnaireId()))).thenReturn(null);

        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        assertEquals(2, result.size());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_handlesPagingParameters_page1() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        Pagination pagination = new Pagination(0, 2);

        QuestionnaireResponseModel response1 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);
        QuestionnaireResponseModel response2 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_2);
        QuestionnaireResponseModel response3 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_3, QUESTIONNAIRE_ID_3);

        Mockito.when(questionnaireResponseRepository.fetchByStatus(statuses)).thenReturn(List.of(response1, response2, response3));
        List<QualifiedId.QuestionnaireId> ids = Stream.of(response1, response2, response3).map(QuestionnaireResponseModel::questionnaireId).toList();
        Mockito.when(questionnaireRepository.history(ids)).thenReturn(List.of());

        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses, pagination);

        assertEquals(2, result.size());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_handlesPagingParameters_page2() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        Pagination pagination = new Pagination(1, 2);
        QuestionnaireResponseModel response1 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);
        QuestionnaireResponseModel response2 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_2, QUESTIONNAIRE_ID_2);
        QuestionnaireResponseModel response3 = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_3, QUESTIONNAIRE_ID_3);
        Mockito.when(questionnaireResponseRepository.fetchByStatus(statuses)).thenReturn(List.of(response1, response2, response3));
        Mockito.when(questionnaireRepository.history(List.of(response1.questionnaireId(), response2.questionnaireId(), response3.questionnaireId()))).thenReturn(List.of());
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses, pagination);
        assertEquals(1, result.size());
    }

    @Test
    public void updateExaminationStatus_resourceNotFound_throwsException() throws Exception {
        QualifiedId.QuestionnaireResponseId id = QUESTIONNAIRE_RESPONSE_ID_1;
        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;
        Mockito.when(questionnaireResponseRepository.fetch(id)).thenReturn(Optional.empty());
        assertThrows(ServiceException.class, () -> subject.updateExaminationStatus(id, status));
    }

    @Test
    public void updateExaminationStatus_successfulUpdate() throws Exception {
        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;
        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, ORGANIZATION_ID_1);
        Mockito.when(questionnaireResponseRepository.fetch(QUESTIONNAIRE_RESPONSE_ID_1)).thenReturn(Optional.of(response));
        subject.updateExaminationStatus(QUESTIONNAIRE_RESPONSE_ID_1, status);
        ArgumentCaptor<QuestionnaireResponseModel> captor = ArgumentCaptor.forClass(QuestionnaireResponseModel.class);
        verify(questionnaireResponseRepository, times(1)).update(captor.capture());
        assertEquals(status, captor.getValue().examinationStatus());
    }



}


