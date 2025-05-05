package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ForbiddenException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteQuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.service.logging.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.ExaminationStatusDto;
import org.openapitools.model.PaginatedListQuestionnaireResponseDto;
import org.openapitools.model.PartialUpdateQuestionnaireResponseRequest;
import org.openapitools.model.QuestionnaireResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireResponseControllerTest {
    @InjectMocks
    private QuestionnaireResponseController subject;

    @Mock
    private ConcreteQuestionnaireResponseService questionnaireResponseService;

    @Mock
    private AuditLoggingService auditLoggingService;

    @Mock
    private DtoMapper dtoMapper;

    private static final QualifiedId.CarePlanId CARE_PLAN_ID = new QualifiedId.CarePlanId("careplan-1");
    private static final List<QualifiedId.QuestionnaireId> QUESTIONNAIRE_IDS = List.of(new QualifiedId.QuestionnaireId("questionnaire-1"));
    private static final QualifiedId.QuestionnaireResponseId QUESTIONNAIRE_RESPONSE_ID = new QualifiedId.QuestionnaireResponseId("questionnaireresponse-1");

    @Test
    public void getQuestionnaireResponsesByCpr_cprParameterMissing_400() {
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(new QualifiedId.QuestionnaireId("questionnaire-1"));
        assertThrows(BadRequestException.class, () -> subject.getQuestionnaireResponsesByCarePlanId(null, questionnaireIds.stream().map(QualifiedId::unqualified).toList(), 1, 5));
    }

    @Test
    public void getQuestionnaireResponsesByCpr_questionnaireIdsParameterMissing_400() {
        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
        assertThrows(BadRequestException.class, () -> subject.getQuestionnaireResponsesByCarePlanId(carePlanId.unqualified(), null, 1, 5));
    }

    @Test
    public void getQuestionnaireResponsesByCpr_responsesPresent_200() throws Exception {
        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(new QualifiedId.QuestionnaireId("questionnaire-1"));

        QuestionnaireResponseModel responseModel1 = QuestionnaireResponseModel.builder().build();
        QuestionnaireResponseModel responseModel2 = QuestionnaireResponseModel.builder().build();
        QuestionnaireResponseDto responseDto1 = new QuestionnaireResponseDto();
        QuestionnaireResponseDto responseDto2 = new QuestionnaireResponseDto();

        Mockito.when(questionnaireResponseService.getQuestionnaireResponses(carePlanId, questionnaireIds)).thenReturn(List.of(responseModel1, responseModel2));
        Mockito.when(dtoMapper.mapQuestionnaireResponseModel(responseModel1)).thenReturn(responseDto1);
        Mockito.when(dtoMapper.mapQuestionnaireResponseModel(responseModel2)).thenReturn(responseDto2);

        ResponseEntity<PaginatedListQuestionnaireResponseDto> result = subject.getQuestionnaireResponsesByCarePlanId(carePlanId.unqualified(), questionnaireIds.stream().map(QualifiedId.QuestionnaireId::unqualified).toList(), 1, 5);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getList().size());
        assertTrue(result.getBody().getList().contains(responseDto1));
        assertTrue(result.getBody().getList().contains(responseDto2));
    }

    @Test
    public void getQuestionnaireResponsesByCpr_responsesMissing_200() throws Exception {
        Mockito.when(questionnaireResponseService.getQuestionnaireResponses(CARE_PLAN_ID, QUESTIONNAIRE_IDS)).thenReturn(List.of());

        ResponseEntity<PaginatedListQuestionnaireResponseDto> result = subject.getQuestionnaireResponsesByCarePlanId(
                CARE_PLAN_ID.unqualified(),
                QUESTIONNAIRE_IDS.stream().map(QualifiedId::unqualified).toList(),
                1, 5);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().getList().isEmpty());
    }

    @Test
    public void getQuestionnaireResponsesByCpr_accessViolation_403() throws Exception {
        Pagination pagination = new Pagination(1, 5);
        Mockito.when(questionnaireResponseService.getQuestionnaireResponses(CARE_PLAN_ID, QUESTIONNAIRE_IDS)).thenThrow(AccessValidationException.class);

        assertThrows(ForbiddenException.class, () -> subject.getQuestionnaireResponsesByCarePlanId(CARE_PLAN_ID.unqualified(), QUESTIONNAIRE_IDS.stream().map(QualifiedId::unqualified).toList(), 1, 5));
    }

    @Test
    public void getQuestionnaireResponsesByCpr_failureToFetch_500() throws Exception {
        Mockito.when(questionnaireResponseService.getQuestionnaireResponses(CARE_PLAN_ID, QUESTIONNAIRE_IDS))
                .thenThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR));
        assertThrows(InternalServerErrorException.class, () -> subject.getQuestionnaireResponsesByCarePlanId(CARE_PLAN_ID.unqualified(), QUESTIONNAIRE_IDS.stream().map(QualifiedId::unqualified).toList(), 1, 5));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_parameterMissing_400() {
        assertThrows(BadRequestException.class, () -> subject.getQuestionnaireResponsesByStatus(null, 1, 10));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesPresent_200() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        List<ExaminationStatusDto> statusesdto = List.of(ExaminationStatusDto.NOT_EXAMINED);

        Pagination pagination = new Pagination(1, 10);

        QuestionnaireResponseModel responseModel1 = QuestionnaireResponseModel.builder().build();
        QuestionnaireResponseModel responseModel2 = QuestionnaireResponseModel.builder().build();
        QuestionnaireResponseDto responseDto1 = new QuestionnaireResponseDto();
        QuestionnaireResponseDto responseDto2 = new QuestionnaireResponseDto();

        Mockito.when(questionnaireResponseService.getQuestionnaireResponsesByStatus(statuses, pagination)).thenReturn(List.of(responseModel1, responseModel2));
        Mockito.when(dtoMapper.mapQuestionnaireResponseModel(responseModel1)).thenReturn(responseDto1);
        Mockito.when(dtoMapper.mapQuestionnaireResponseModel(responseModel2)).thenReturn(responseDto2);
        Mockito.when(dtoMapper.mapExaminationStatusDto(ExaminationStatusDto.NOT_EXAMINED)).thenReturn(ExaminationStatus.NOT_EXAMINED);

        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByStatus(statusesdto, pagination.offset(), pagination.limit());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(responseDto1));
        assertTrue(result.getBody().contains(responseDto2));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesMissing_200() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.UNDER_EXAMINATION);
        Pagination pagination = new Pagination(1, 10);

        Mockito.when(questionnaireResponseService.getQuestionnaireResponsesByStatus(statuses, pagination)).thenReturn(List.of());

        Mockito.when(dtoMapper.mapExaminationStatusDto(ExaminationStatusDto.UNDER_EXAMINATION)).thenReturn(ExaminationStatus.UNDER_EXAMINATION);
        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByStatus(List.of(ExaminationStatusDto.UNDER_EXAMINATION), pagination.offset(), pagination.limit());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_failureToFetch_500() throws Exception {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.EXAMINED);
        List<ExaminationStatusDto> statusesDtos = List.of(ExaminationStatusDto.UNDER_EXAMINATION, ExaminationStatusDto.EXAMINED);

        Pagination pagination = new Pagination(1, 10);

        Mockito.when(dtoMapper.mapExaminationStatusDto(ExaminationStatusDto.UNDER_EXAMINATION)).thenReturn(ExaminationStatus.UNDER_EXAMINATION);
        Mockito.when(dtoMapper.mapExaminationStatusDto(ExaminationStatusDto.EXAMINED)).thenReturn(ExaminationStatus.EXAMINED);
        Mockito.when(questionnaireResponseService.getQuestionnaireResponsesByStatus(statuses, pagination)).thenThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR));

        assertThrows(InternalServerErrorException.class, () -> subject.getQuestionnaireResponsesByStatus(statusesDtos, pagination.offset(), pagination.limit()));
    }

    @Test
    public void patchQuestionnaireResponse_malformedRequest_400() {
        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();
        assertThrows(BadRequestException.class, () -> subject.patchQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID.unqualified(), request));
    }

    @Test
    public void patchQuestionnaireResponse_accessViolation_403() throws Exception {

        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();
        request.setExaminationStatus(Optional.of(ExaminationStatusDto.UNDER_EXAMINATION));

        Mockito.when(dtoMapper.mapExaminationStatusDto(ExaminationStatusDto.UNDER_EXAMINATION)).thenReturn(ExaminationStatus.UNDER_EXAMINATION);
        Mockito.doThrow(AccessValidationException.class).when(questionnaireResponseService).updateExaminationStatus(QUESTIONNAIRE_RESPONSE_ID, ExaminationStatus.UNDER_EXAMINATION);

        assertThrows(ForbiddenException.class, () -> subject.patchQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID.unqualified(), request));
    }

    @Test
    public void patchQuestionnaireResponse_failureToUpdate_500() throws Exception {

        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();
        request.setExaminationStatus(Optional.of(ExaminationStatusDto.UNDER_EXAMINATION));

        Mockito.when(dtoMapper.mapExaminationStatusDto(ExaminationStatusDto.UNDER_EXAMINATION)).thenReturn(ExaminationStatus.UNDER_EXAMINATION);
        Mockito.doThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR)).when(questionnaireResponseService).updateExaminationStatus(QUESTIONNAIRE_RESPONSE_ID, ExaminationStatus.UNDER_EXAMINATION);

        assertThrows(InternalServerErrorException.class, () -> subject.patchQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID.unqualified(), request));
    }

    @Test
    public void patchQuestionnaireResponse_success_200() throws Exception {
        QualifiedId.QuestionnaireResponseId id = new QualifiedId.QuestionnaireResponseId("questionnaireresponse-1");
        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();
        request.setExaminationStatus(Optional.of(ExaminationStatusDto.UNDER_EXAMINATION));

        Mockito.when(dtoMapper.mapExaminationStatusDto(ExaminationStatusDto.UNDER_EXAMINATION)).thenReturn(ExaminationStatus.UNDER_EXAMINATION);
        Mockito.when(questionnaireResponseService.updateExaminationStatus(id, ExaminationStatus.UNDER_EXAMINATION)).thenReturn(QuestionnaireResponseModel.builder().build());

        ResponseEntity<Void> result = subject.patchQuestionnaireResponse(id.unqualified(), request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}