package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.comparator.QuestionnaireResponsePriorityComparator;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.repository.OrganizationRepository;
import dk.kvalitetsit.hjemmebehandling.repository.PractitionerRepository;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireRepository;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireResponseRepository;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteQuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Questionnaire;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireResponseServiceTest {
    private static final QualifiedId.CarePlanId CAREPLAN_ID_1 = new QualifiedId.CarePlanId("careplan-1");
    private static final QualifiedId.OrganizationId ORGANIZATION_ID_1 = new QualifiedId.OrganizationId("organization-1");
    private static final QualifiedId.OrganizationId ORGANIZATION_ID_2 = new QualifiedId.OrganizationId("organization-2");
    private static final QualifiedId.PatientId PATIENT_ID = new QualifiedId.PatientId("patient-1");
    private static final QualifiedId.QuestionnaireId QUESTIONNAIRE_ID_1 = new QualifiedId.QuestionnaireId("questionnaire-1");
    private static final QualifiedId.QuestionnaireId QUESTIONNAIRE_ID_2 = new QualifiedId.QuestionnaireId("questionnaire-2");
    private static final QualifiedId.QuestionnaireId QUESTIONNAIRE_ID_3 = new QualifiedId.QuestionnaireId("questionnaire-3");
    private static final QualifiedId.QuestionnaireResponseId QUESTIONNAIRE_RESPONSE_ID_1 = new QualifiedId.QuestionnaireResponseId("questionnaireresponse-1");
    private static final QualifiedId.QuestionnaireResponseId QUESTIONNAIRE_RESPONSE_ID_2 = new QualifiedId.QuestionnaireResponseId("questionnaireresponse-2");
    private static final QualifiedId.QuestionnaireResponseId QUESTIONNAIRE_RESPONSE_ID_3 = new QualifiedId.QuestionnaireResponseId("questionnaireresponse-3");

    @InjectMocks
    private ConcreteQuestionnaireResponseService subject;

    @Mock
    private QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository;

    @Mock
    private QuestionnaireRepository<QuestionnaireModel> questionnaireRepository;

    @Mock
    private PractitionerRepository<PractitionerModel> practitionerRepository;

    @Mock
    private OrganizationRepository<Organization> organizationRepository;

    @Mock
    private QuestionnaireResponsePriorityComparator priorityComparator;

    @Mock
    private AccessValidator accessValidator;

    @Test
    public void getQuestionnaireResponses_responsesPresent_returnsResponses() throws Exception {
        QualifiedId.CarePlanId carePlanId = CAREPLAN_ID_1;
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);

        Mockito.when(questionnaireResponseRepository.fetch(carePlanId, questionnaireIds)).thenReturn(List.of(response));
        Mockito.when(questionnaireRepository.lookupVersionsOfQuestionnaireById(questionnaireIds)).thenReturn(null);

        Pagination pagination = new Pagination(1, 5);
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(carePlanId, questionnaireIds, pagination);

        ArgumentCaptor<QuestionnaireResponseModel> captor = ArgumentCaptor.forClass(QuestionnaireResponseModel.class);
        verify(questionnaireResponseRepository, times(1)).update(captor.capture());


        assertEquals(1, result.size());

    }

    @Test
    public void getQuestionnaireResponses_ReturnOneItem_WhenPagesizeIsOne() throws Exception {
        QuestionnaireResponseModel response = QuestionnaireResponseModel.builder().build();

        Mockito.when(questionnaireResponseRepository.fetch(null, (QualifiedId.CarePlanId) null)).thenReturn(List.of(response));

        Pagination pagination = new Pagination(1, 1);
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

        Mockito.when(questionnaireResponseRepository.fetch(null, (QualifiedId.CarePlanId) null)).thenReturn(List.of(response1, response3, response4, response2));
        Mockito.when(questionnaireRepository.lookupVersionsOfQuestionnaireById(null)).thenReturn(List.of());

        Pagination pagination1 = new Pagination(1, 2);
        List<QuestionnaireResponseModel> result1 = subject.getQuestionnaireResponses(null, null, pagination1);

        Pagination pagination2 = new Pagination(2, 2);
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

        Mockito.when(questionnaireResponseRepository.fetch(null, (QualifiedId.CarePlanId) null)).thenReturn(List.of(response));

        Pagination pagination = new Pagination(1, 0);
        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponses(null, null, pagination);

        assertEquals(0, result.size());
    }

    @Test
    public void getQuestionnaireResponses_ReturnZeroItem_WhenOnSecondPage() throws Exception {
        QuestionnaireResponseModel response = QuestionnaireResponseModel.builder().build();

        Mockito.when(questionnaireResponseRepository.fetch(null, (QualifiedId.CarePlanId) null)).thenReturn(List.of(response));

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
    public void getQuestionnaireResponses_accessViolation_throwsException() throws Exception {
        QualifiedId.CarePlanId carePlanId = CAREPLAN_ID_1;
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        QuestionnaireResponseModel response = QuestionnaireResponseModel.builder().build();

        Mockito.when(questionnaireResponseRepository.fetch(carePlanId, questionnaireIds)).thenReturn(List.of(response));

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(response));

        Pagination pagination = new Pagination(1, 0);
        assertThrows(AccessValidationException.class, () -> subject.getQuestionnaireResponses(carePlanId, questionnaireIds, pagination));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesPresent_returnsResponses() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1);
        Mockito.when(questionnaireResponseRepository.fetchByStatus(statuses)).thenReturn(List.of(response));
        Mockito.when(questionnaireRepository.lookupVersionsOfQuestionnaireById(List.of())).thenReturn(null);

        QuestionnaireResponseModel model = QuestionnaireResponseModel.builder().build();

        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses);

        assertEquals(1, result.size());
        assertEquals(model, result.getFirst());
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

        Mockito.when(questionnaireRepository.lookupVersionsOfQuestionnaireById(ids)).thenReturn(null);

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

        Mockito.when(questionnaireResponseRepository.fetchByStatus(statuses)).thenReturn(List.of(firstResponse, secondResponse));
        Mockito.when(questionnaireRepository.lookupVersionsOfQuestionnaireById(List.of())).thenReturn(null);

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

        Mockito.when(questionnaireResponseRepository.fetchByStatus(statuses)).thenReturn(List.of(response1, response2, response3));
        List<QualifiedId.QuestionnaireId> ids = Stream.of(response1, response2, response3).map(questionnaire -> questionnaire.questionnaireId()).toList();
        Mockito.when(questionnaireRepository.lookupVersionsOfQuestionnaireById(ids)).thenReturn(List.of());

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

        Mockito.when(questionnaireResponseRepository.fetchByStatus(statuses)).thenReturn(List.of(response1, response2, response3));
        Mockito.when(questionnaireRepository.lookupVersionsOfQuestionnaireById(List.of())).thenReturn(List.of());

        List<QuestionnaireResponseModel> result = subject.getQuestionnaireResponsesByStatus(statuses, pagination);

        assertEquals(1, result.size());
    }

    @Test
    public void updateExaminationStatus_resourceNotFound_throwsException() throws Exception {
        QualifiedId.QuestionnaireResponseId id = new QualifiedId.QuestionnaireResponseId("questionnaireresponse-1");
        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;
        Mockito.when(questionnaireResponseRepository.fetch(id)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> subject.updateExaminationStatus(id, status));
    }

    @Test
    public void updateExaminationStatus_accessViolation_throwsException() throws Exception {
        QualifiedId.QuestionnaireResponseId id = new QualifiedId.QuestionnaireResponseId("questionnaireresponse-1");
        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;

        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, ORGANIZATION_ID_2);
        Mockito.when(questionnaireResponseRepository.fetch(id)).thenReturn(Optional.of(response));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(response);

        assertThrows(AccessValidationException.class, () -> subject.updateExaminationStatus(id, status));
    }

    @Test
    public void updateExaminationStatus_successfulUpdate() throws Exception {
        QualifiedId.QuestionnaireResponseId id = new QualifiedId.QuestionnaireResponseId("questionnaireresponse-1");
        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;
        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, ORGANIZATION_ID_1);

        Mockito.when(questionnaireResponseRepository.fetch(id)).thenReturn(Optional.of(response));

        subject.updateExaminationStatus(id, status);

        ArgumentCaptor<QuestionnaireResponseModel> captor = ArgumentCaptor.forClass(QuestionnaireResponseModel.class);
        verify(questionnaireResponseRepository, times(1)).update(captor.capture());
        assertEquals(status, captor.getValue().examinationStatus());
    }

    private QuestionnaireResponseModel buildQuestionnaireResponse(QualifiedId.QuestionnaireResponseId questionnaireResponseId, QualifiedId.QuestionnaireId questionnaireId) {
        return buildQuestionnaireResponse(questionnaireResponseId, questionnaireId, ORGANIZATION_ID_1);
    }

    private QuestionnaireResponseModel buildQuestionnaireResponse(QualifiedId.QuestionnaireResponseId questionnaireResponseId, QualifiedId.QuestionnaireId questionnaireId, QualifiedId.OrganizationId organizationId) {
        return QuestionnaireResponseModel.builder()
                .id(questionnaireResponseId)
                .questionnaireId(questionnaireId)
                .organizationId(organizationId)
                .build();

    }
}