package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.request.CreateQuestionnaireRequest;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.QuestionnaireDto;
import dk.kvalitetsit.hjemmebehandling.api.request.PatchQuestionnaireRequest;
import dk.kvalitetsit.hjemmebehandling.api.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ForbiddenException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

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

        Mockito.when(questionnaireService.getQuestionnaires(Collections.emptyList())).thenReturn(List.of(questionnaireModel1, questionnaireModel2));
        Mockito.when(dtoMapper.mapQuestionnaireModel(questionnaireModel1)).thenReturn(questionnaireDto1);
        Mockito.when(dtoMapper.mapQuestionnaireModel(questionnaireModel2)).thenReturn(questionnaireDto2);

        // Act
        ResponseEntity<List<QuestionnaireDto>> result = subject.getQuestionnaires(Optional.empty());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(questionnaireDto1));
        assertTrue(result.getBody().contains(questionnaireDto2));
    }

    @Test
    public void getQuestionnaires_sorting() {
        // Arrange
        QuestionnaireModel questionnaireModel1 = new QuestionnaireModel();
        QuestionnaireModel questionnaireModel2 = new QuestionnaireModel();
        QuestionnaireModel questionnaireModel3 = new QuestionnaireModel();
        QuestionnaireDto questionnaireDto1 = new QuestionnaireDto();
        QuestionnaireDto questionnaireDto2 = new QuestionnaireDto();
        QuestionnaireDto questionnaireDto3 = new QuestionnaireDto();
        questionnaireDto1.setLastUpdated(java.sql.Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
        questionnaireDto2.setLastUpdated(null);
        questionnaireDto3.setLastUpdated(java.sql.Date.from(Instant.now()));

        Mockito.when(questionnaireService.getQuestionnaires(Collections.emptyList())).thenReturn(List.of(questionnaireModel1, questionnaireModel2, questionnaireModel3));
        Mockito.when(dtoMapper.mapQuestionnaireModel(questionnaireModel1)).thenReturn(questionnaireDto1);
        Mockito.when(dtoMapper.mapQuestionnaireModel(questionnaireModel2)).thenReturn(questionnaireDto2);
        Mockito.when(dtoMapper.mapQuestionnaireModel(questionnaireModel3)).thenReturn(questionnaireDto3);

        // Act
        ResponseEntity<List<QuestionnaireDto>> result = subject.getQuestionnaires(Optional.empty());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3, result.getBody().size());
        assertEquals(questionnaireDto3, result.getBody().get(0));
        assertEquals(questionnaireDto1, result.getBody().get(1));
        assertEquals(questionnaireDto2, result.getBody().get(2));
    }

    @Test
    public void getQuestionnaire_questionnairesMissing_200() throws Exception {
        // Arrange
        Mockito.when(questionnaireService.getQuestionnaires(Collections.emptyList())).thenReturn(List.of());

        // Act
        ResponseEntity<List<QuestionnaireDto>> result = subject.getQuestionnaires(Optional.empty());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    public void createQuestionnaire_success_201() {
        // Arrange
        CreateQuestionnaireRequest request = new CreateQuestionnaireRequest();
        var questionnaireDto = new QuestionnaireDto();
        questionnaireDto.setQuestions(List.of(buildQuestionDto()));
        request.setQuestionnaire(questionnaireDto);

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
        var questionnaireDto = new QuestionnaireDto();
        questionnaireDto.setQuestions(List.of(buildQuestionDto()));
        request.setQuestionnaire(questionnaireDto);

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

    @Test
    public void patchQuestionnaire_success_() throws Exception {
        // Arrange
        PatchQuestionnaireRequest request = new PatchQuestionnaireRequest();
        request.setQuestions(List.of(buildQuestionDto()));

        // Act
        ResponseEntity<Void> result = subject.patchQuestionnaire("questionnaire-1", request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    public void patchQuestionnaire_nullQuestions_throwsBadRequestException() throws Exception {
        // Arrange
        String id = "questionnaire-1";
        PatchQuestionnaireRequest request = new PatchQuestionnaireRequest();
        request.setQuestions(null);

        // Act

        // Assert
        assertThrows(BadRequestException.class, () -> subject.patchQuestionnaire(id, request));
    }

    @Test
    public void patchQuestionnaire_emptyQuestions_throwsBadRequestException() throws Exception {
        // Arrange
        String id = "questionnaire-1";
        PatchQuestionnaireRequest request = new PatchQuestionnaireRequest();
        request.setQuestions(List.of());

        // Act

        // Assert
        assertThrows(BadRequestException.class, () -> subject.patchQuestionnaire(id, request));
    }

    @Test
    public void patchQuestionnaire_accessViolation() throws Exception {
        // Arrange
        String id = "questionnaire-1";
        String qualifyId = FhirUtils.qualifyId(id, ResourceType.Questionnaire);

        PatchQuestionnaireRequest request = new PatchQuestionnaireRequest();
        QuestionDto questionDto = buildQuestionDto();
        request.setQuestions(List.of(questionDto));

        QuestionModel questionModel =  new QuestionModel();
        Mockito.when(dtoMapper.mapQuestionDto(questionDto)).thenReturn(questionModel);

        Mockito.doThrow(AccessValidationException.class).when(questionnaireService).updateQuestionnaire(qualifyId, null, null, null, List.of(questionModel), List.of());


        // Act

        // Assert
        assertThrows(ForbiddenException.class, () -> subject.patchQuestionnaire(id, request));
    }



    private QuestionDto buildQuestionDto() {
        QuestionDto questionDto = new QuestionDto();
        questionDto.setQuestionType(QuestionType.BOOLEAN);

        return questionDto;
    }
}