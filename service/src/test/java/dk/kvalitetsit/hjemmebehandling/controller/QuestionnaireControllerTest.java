package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.CarePlanDto;
import dk.kvalitetsit.hjemmebehandling.api.CreateCarePlanRequest;
import dk.kvalitetsit.hjemmebehandling.api.CreateQuestionnaireRequest;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.PlanDefinitionDto;
import dk.kvalitetsit.hjemmebehandling.api.QuestionnaireDto;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.service.PlanDefinitionService;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireControllerTest {
    @InjectMocks
    private QuestionnaireController subject;

    @Mock
    private QuestionnaireService questionnaireService;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private LocationHeaderBuilder locationHeaderBuilder;

    @Test
    public void getQuestionnaire_questionnairesPresent_200() throws Exception {
        // Arrange
        QuestionnaireModel questionnaireModel1 = new QuestionnaireModel();
        QuestionnaireModel questionnaireModel2 = new QuestionnaireModel();
        QuestionnaireDto questionnaireDto1 = new QuestionnaireDto();
        QuestionnaireDto questionnaireDto2 = new QuestionnaireDto();

        Mockito.when(questionnaireService.getQuestionnaires()).thenReturn(List.of(questionnaireModel1, questionnaireModel2));
        Mockito.when(dtoMapper.mapQuestionnaireModel(questionnaireModel1)).thenReturn(questionnaireDto1);
        Mockito.when(dtoMapper.mapQuestionnaireModel(questionnaireModel2)).thenReturn(questionnaireDto2);

        // Act
        ResponseEntity<List<QuestionnaireDto>> result = subject.getQuestionnaires();

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(questionnaireDto1));
        assertTrue(result.getBody().contains(questionnaireDto2));
    }

    @Test
    public void getQuestionnaire_questionnairesMissing_200() throws Exception {
        // Arrange
        Mockito.when(questionnaireService.getQuestionnaires()).thenReturn(List.of());

        // Act
        ResponseEntity<List<QuestionnaireDto>> result = subject.getQuestionnaires();

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    public void createQuestionnaire_success_201() {
        // Arrange
        CreateQuestionnaireRequest request = new CreateQuestionnaireRequest();
        request.setQuestionnaire(new QuestionnaireDto());

        Mockito.when(dtoMapper.mapQuestionnaireDto(request.getQuestionnaire())).thenReturn(new QuestionnaireModel());

        // Act
        ResponseEntity<Void> result = subject.createQuestionnaire(request);

        // Assert
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    public void createQuestionnaire_success_setsLocationHeader() throws Exception {
        // Arrange
        CreateQuestionnaireRequest request = new CreateQuestionnaireRequest();
        request.setQuestionnaire(new QuestionnaireDto());

        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        Mockito.when(dtoMapper.mapQuestionnaireDto(request.getQuestionnaire())).thenReturn(questionnaireModel);
        Mockito.when(questionnaireService.createQuestionnaire(questionnaireModel)).thenReturn("questionnaire-1");

        String location = "http://localhost:8080/api/v1/questionnaire/questionnaire-1";
        Mockito.when(locationHeaderBuilder.buildLocationHeader("questionnaire-1")).thenReturn(URI.create(location));

        // Act
        ResponseEntity<Void> result = subject.createQuestionnaire(request);

        // Assert
        assertNotNull(result.getHeaders().get("Location"));
        assertEquals(location, result.getHeaders().get("Location").get(0));
    }
}