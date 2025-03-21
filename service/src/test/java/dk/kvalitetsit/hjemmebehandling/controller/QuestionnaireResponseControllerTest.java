package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.PartialUpdateQuestionnaireResponseRequest;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ForbiddenException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.api.PaginatedList;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.QuestionnaireResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireResponseControllerTest {
    @InjectMocks
    private QuestionnaireResponseController subject;

    @Mock
    private QuestionnaireResponseService questionnaireResponseService;

    @Mock
    private AuditLoggingService auditLoggingService;

    @Mock
    private DtoMapper dtoMapper;

    @Test
    public void getQuestionnaireResponsesByCpr_cprParameterMissing_400() {
        // Arrange
        String carePlanId = null;
        List<String> questionnaireIds = List.of("questionnaire-1");

        // Act

        // Assert
        assertThrows(BadRequestException.class, () -> subject.getQuestionnaireResponsesByCarePlanId(carePlanId, questionnaireIds,1,5));
    }

    @Test
    public void getQuestionnaireResponsesByCpr_questionnaireIdsParameterMissing_400() {
        // Arrange
        String carePlanId = "careplan-1";
        List<String> questionnaireIds = null;

        // Act

        // Assert
        assertThrows(BadRequestException.class, () -> subject.getQuestionnaireResponsesByCarePlanId(carePlanId, questionnaireIds,1,5));
    }

    @Test
    public void getQuestionnaireResponsesByCpr_responsesPresent_200() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";
        List<String> questionnaireIds = List.of("questionnaire-1");

        QuestionnaireResponseModel responseModel1 = new QuestionnaireResponseModel();
        QuestionnaireResponseModel responseModel2 = new QuestionnaireResponseModel();
        QuestionnaireResponseDto responseDto1 = new QuestionnaireResponseDto();
        QuestionnaireResponseDto responseDto2 = new QuestionnaireResponseDto();

        Pagination pagination = new Pagination(1,5);
        Mockito.when(questionnaireResponseService.getQuestionnaireResponses(carePlanId, questionnaireIds)).thenReturn(List.of(responseModel1, responseModel2));
        Mockito.when(dtoMapper.mapQuestionnaireResponseModel(responseModel1)).thenReturn(responseDto1);
        Mockito.when(dtoMapper.mapQuestionnaireResponseModel(responseModel2)).thenReturn(responseDto2);

        // Act
        ResponseEntity<PaginatedList<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByCarePlanId(carePlanId, questionnaireIds,1,5);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getList().size());
        assertTrue(result.getBody().getList().contains(responseDto1));
        assertTrue(result.getBody().getList().contains(responseDto2));
    }

    @Test
    public void getQuestionnaireResponsesByCpr_responsesMissing_200() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";
        List<String> questionnaireIds = List.of("questionnaire-1");

        Mockito.when(questionnaireResponseService.getQuestionnaireResponses(carePlanId, questionnaireIds)).thenReturn(List.of());

        // Act
        ResponseEntity<PaginatedList<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByCarePlanId(carePlanId, questionnaireIds,1,5);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().getList().isEmpty());
    }

    @Test
    public void getQuestionnaireResponsesByCpr_accessViolation_403() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";
        List<String> questionnaireIds = List.of("questionnaire-1");

        Pagination pagination = new Pagination(1,5);
        Mockito.when(questionnaireResponseService.getQuestionnaireResponses(carePlanId, questionnaireIds)).thenThrow(AccessValidationException.class);

        // Act

        // Assert
        assertThrows(ForbiddenException.class, () -> subject.getQuestionnaireResponsesByCarePlanId(carePlanId, questionnaireIds,1,5));
    }

    @Test
    public void getQuestionnaireResponsesByCpr_failureToFetch_500() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";
        List<String> questionnaireIds = List.of("questionnaire-1");


        Mockito.when(questionnaireResponseService.getQuestionnaireResponses(carePlanId, questionnaireIds)).thenThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR));

        // Act

        // Assert
        assertThrows(InternalServerErrorException.class, () -> subject.getQuestionnaireResponsesByCarePlanId(carePlanId, questionnaireIds,1,5));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_parameterMissing_400() {
        // Arrange
        List<ExaminationStatus> statuses = null;
        Pagination pagination = null;

        // Act

        // Assert
        assertThrows(BadRequestException.class, () -> subject.getQuestionnaireResponsesByStatus(statuses, 1, 10));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesPresent_200() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        Pagination pagination = new Pagination(1, 10);

        QuestionnaireResponseModel responseModel1 = new QuestionnaireResponseModel();
        QuestionnaireResponseModel responseModel2 = new QuestionnaireResponseModel();
        QuestionnaireResponseDto responseDto1 = new QuestionnaireResponseDto();
        QuestionnaireResponseDto responseDto2 = new QuestionnaireResponseDto();

        Mockito.when(questionnaireResponseService.getQuestionnaireResponsesByStatus(statuses, pagination)).thenReturn(List.of(responseModel1, responseModel2));
        Mockito.when(dtoMapper.mapQuestionnaireResponseModel(responseModel1)).thenReturn(responseDto1);
        Mockito.when(dtoMapper.mapQuestionnaireResponseModel(responseModel2)).thenReturn(responseDto2);

        // Act
        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByStatus(statuses, pagination.getOffset(), pagination.getLimit());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(responseDto1));
        assertTrue(result.getBody().contains(responseDto2));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesMissing_200() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.UNDER_EXAMINATION);
        Pagination pagination = new Pagination(1, 10);

        Mockito.when(questionnaireResponseService.getQuestionnaireResponsesByStatus(statuses, pagination)).thenReturn(List.of());

        // Act
        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByStatus(statuses, pagination.getOffset(), pagination.getLimit());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_failureToFetch_500() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.EXAMINED);
        Pagination pagination = new Pagination(1, 10);

        Mockito.when(questionnaireResponseService.getQuestionnaireResponsesByStatus(statuses, pagination)).thenThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR));

        // Act

        // Assert
        assertThrows(InternalServerErrorException.class, () -> subject.getQuestionnaireResponsesByStatus(statuses, pagination.getOffset(), pagination.getLimit()));
    }

    @Test
    public void patchQuestionnaireResponse_malformedRequest_400() {
        // Arrange
        String id = "questionnaireresponse-1";
        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();

        // Act

        // Assert
        assertThrows(BadRequestException.class, () -> subject.patchQuestionnaireResponse(id, request));
    }

    @Test
    public void patchQuestionnaireResponse_accessViolation_403() throws Exception {
        // Arrange
        String id = "questionnaireresponse-1";
        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();
        request.setExaminationStatus(ExaminationStatus.UNDER_EXAMINATION);

        Mockito.doThrow(AccessValidationException.class).when(questionnaireResponseService).updateExaminationStatus(id, request.getExaminationStatus());

        // Act

        // Assert
        assertThrows(ForbiddenException.class, () -> subject.patchQuestionnaireResponse(id, request));
    }

    @Test
    public void patchQuestionnaireResponse_failureToUpdate_500() throws Exception {
        // Arrange
        String id = "questionnaireresponse-1";
        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();
        request.setExaminationStatus(ExaminationStatus.UNDER_EXAMINATION);

        Mockito.doThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR)).when(questionnaireResponseService).updateExaminationStatus(id, request.getExaminationStatus());

        // Act

        // Assert
        assertThrows(InternalServerErrorException.class, () -> subject.patchQuestionnaireResponse(id, request));
    }

    @Test
    public void patchQuestionnaireResponse_success_200() throws Exception {
        // Arrange
        String id = "questionnaireresponse-1";
        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();
        request.setExaminationStatus(ExaminationStatus.UNDER_EXAMINATION);

        //Mockito.doNothing().when(questionnaireResponseService).updateExaminationStatus(id, request.getExaminationStatus());
        Mockito.when(questionnaireResponseService.updateExaminationStatus(id, request.getExaminationStatus())).thenReturn(new QuestionnaireResponseModel());

        // Act
        ResponseEntity<Void> result = subject.patchQuestionnaireResponse(id, request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}