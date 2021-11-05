package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.PartialUpdateQuestionnaireResponseRequest;
import dk.kvalitetsit.hjemmebehandling.api.QuestionnaireResponseDto;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private DtoMapper dtoMapper;

    @Test
    public void getQuestionnaireResponsesByCpr_cprParameterMissing_400() {
        // Arrange
        String cpr = null;
        List<String> questionnaireIds = List.of("questionnaire-1");

        // Act
        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByCpr(cpr, questionnaireIds);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    public void getQuestionnaireResponsesByCpr_questionnaireIdsParameterMissing_400() {
        // Arrange
        String cpr = "0101010101";
        List<String> questionnaireIds = null;

        // Act
        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByCpr(cpr, questionnaireIds);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    public void getQuestionnaireResponsesByCpr_responsesPresent_200() throws Exception {
        // Arrange
        String cpr = "0101010101";
        List<String> questionnaireIds = List.of("questionnaire-1");

        QuestionnaireResponseModel responseModel1 = new QuestionnaireResponseModel();
        QuestionnaireResponseModel responseModel2 = new QuestionnaireResponseModel();
        QuestionnaireResponseDto responseDto1 = new QuestionnaireResponseDto();
        QuestionnaireResponseDto responseDto2 = new QuestionnaireResponseDto();

        Mockito.when(questionnaireResponseService.getQuestionnaireResponses(cpr, questionnaireIds)).thenReturn(List.of(responseModel1, responseModel2));
        Mockito.when(dtoMapper.mapQuestionnaireResponseModel(responseModel1)).thenReturn(responseDto1);
        Mockito.when(dtoMapper.mapQuestionnaireResponseModel(responseModel2)).thenReturn(responseDto2);

        // Act
        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByCpr(cpr, questionnaireIds);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(responseDto1));
        assertTrue(result.getBody().contains(responseDto2));
    }

    @Test
    public void getQuestionnaireResponsesByCpr_responsesMissing_204() throws Exception {
        // Arrange
        String cpr = "0101010101";
        List<String> questionnaireIds = List.of("questionnaire-1");

        Mockito.when(questionnaireResponseService.getQuestionnaireResponses(cpr, questionnaireIds)).thenReturn(List.of());

        // Act
        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByCpr(cpr, questionnaireIds);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    public void getQuestionnaireResponsesByCpr_failureToFetch_500() throws Exception {
        // Arrange
        String cpr = "0101010101";
        List<String> questionnaireIds = List.of("questionnaire-1");

        Mockito.when(questionnaireResponseService.getQuestionnaireResponses(cpr, questionnaireIds)).thenThrow(ServiceException.class);

        // Act
        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByCpr(cpr, questionnaireIds);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_parameterMissing_400() {
        // Arrange
        List<ExaminationStatus> statuses = null;

        // Act
        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesPresent_200() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponseModel responseModel1 = new QuestionnaireResponseModel();
        QuestionnaireResponseModel responseModel2 = new QuestionnaireResponseModel();
        QuestionnaireResponseDto responseDto1 = new QuestionnaireResponseDto();
        QuestionnaireResponseDto responseDto2 = new QuestionnaireResponseDto();

        Mockito.when(questionnaireResponseService.getQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of(responseModel1, responseModel2));
        Mockito.when(dtoMapper.mapQuestionnaireResponseModel(responseModel1)).thenReturn(responseDto1);
        Mockito.when(dtoMapper.mapQuestionnaireResponseModel(responseModel2)).thenReturn(responseDto2);

        // Act
        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(responseDto1));
        assertTrue(result.getBody().contains(responseDto2));
    }

    @Test
    public void getQuestionnaireResponsesByStatus_responsesMissing_204() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.UNDER_EXAMINATION);

        Mockito.when(questionnaireResponseService.getQuestionnaireResponsesByStatus(statuses)).thenReturn(List.of());

        // Act
        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_failureToFetch_500() throws Exception {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.EXAMINED);

        Mockito.when(questionnaireResponseService.getQuestionnaireResponsesByStatus(statuses)).thenThrow(ServiceException.class);

        // Act
        ResponseEntity<List<QuestionnaireResponseDto>> result = subject.getQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    public void patchQuestionnaireResponse_malformedRequest_400() {
        // Arrange
        String id = "questionnaireresponse-1";
        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();

        // Act
        ResponseEntity<Void> result = subject.patchQuestionnaireResponse(id, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    public void patchQuestionnaireResponse_failureToUpdate_500() throws Exception {
        // Arrange
        String id = "questionnaireresponse-1";
        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();
        request.setExaminationStatus(ExaminationStatus.UNDER_EXAMINATION);

        Mockito.doThrow(ServiceException.class).when(questionnaireResponseService).updateExaminationStatus(id, request.getExaminationStatus());

        // Act
        ResponseEntity<Void> result = subject.patchQuestionnaireResponse(id, request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    public void patchQuestionnaireResponse_success_200() throws Exception {
        // Arrange
        String id = "questionnaireresponse-1";
        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();
        request.setExaminationStatus(ExaminationStatus.UNDER_EXAMINATION);

        Mockito.doNothing().when(questionnaireResponseService).updateExaminationStatus(id, request.getExaminationStatus());

        // Act
        ResponseEntity<Void> result = subject.patchQuestionnaireResponse(id, request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}