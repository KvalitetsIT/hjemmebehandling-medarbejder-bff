package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ForbiddenException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteQuestionnaireService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.CreateQuestionnaireRequest;
import org.openapitools.model.PatchQuestionnaireRequest;
import org.openapitools.model.QuestionDto;
import org.openapitools.model.QuestionnaireDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class QuestionnaireControllerTest {
    @InjectMocks
    private QuestionnaireController subject;

    @Mock
    private ConcreteQuestionnaireService questionnaireService;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private LocationHeaderBuilder locationHeaderBuilder;

    @Test
    public void getQuestionnaire_questionnairesPresent_200() throws Exception {
        QuestionnaireModel questionnaireModel1 = QuestionnaireModel.builder().build();
        QuestionnaireModel questionnaireModel2 = QuestionnaireModel.builder().build();
        QuestionnaireDto questionnaireDto1 = new QuestionnaireDto();
        QuestionnaireDto questionnaireDto2 = new QuestionnaireDto();

        Mockito.when(questionnaireService.getQuestionnaires(Collections.emptyList())).thenReturn(List.of(questionnaireModel1, questionnaireModel2));
        Mockito.when(dtoMapper.mapQuestionnaireModel(questionnaireModel1)).thenReturn(questionnaireDto1);
        Mockito.when(dtoMapper.mapQuestionnaireModel(questionnaireModel2)).thenReturn(questionnaireDto2);

        ResponseEntity<List<QuestionnaireDto>> result = subject.getQuestionnaires(Optional.empty());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, Objects.requireNonNull(result.getBody()).size());
        assertTrue(result.getBody().contains(questionnaireDto1));
        assertTrue(result.getBody().contains(questionnaireDto2));
    }

    @Test
    public void getQuestionnaires_sorting() throws ServiceException {
        QuestionnaireModel questionnaireModel1 =  QuestionnaireModel.builder().build();
        QuestionnaireModel questionnaireModel2 =  QuestionnaireModel.builder().build();
        QuestionnaireModel questionnaireModel3 =  QuestionnaireModel.builder().build();
        QuestionnaireDto questionnaireDto1 = new QuestionnaireDto();
        QuestionnaireDto questionnaireDto2 = new QuestionnaireDto();
        QuestionnaireDto questionnaireDto3 = new QuestionnaireDto();

        Function<Date, OffsetDateTime> convert = (Date date) -> OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);

        questionnaireDto1.setLastUpdated(Optional.ofNullable(convert.apply(java.sql.Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))));
        questionnaireDto2.setLastUpdated(Optional.empty());
        questionnaireDto3.setLastUpdated(Optional.ofNullable(convert.apply(java.sql.Date.from(Instant.now()))));

        Mockito.when(questionnaireService.getQuestionnaires(Collections.emptyList())).thenReturn(List.of(questionnaireModel1, questionnaireModel2, questionnaireModel3));
        Mockito.when(dtoMapper.mapQuestionnaireModel(questionnaireModel1)).thenReturn(questionnaireDto1);
        Mockito.when(dtoMapper.mapQuestionnaireModel(questionnaireModel2)).thenReturn(questionnaireDto2);
        Mockito.when(dtoMapper.mapQuestionnaireModel(questionnaireModel3)).thenReturn(questionnaireDto3);

        ResponseEntity<List<QuestionnaireDto>> result = subject.getQuestionnaires(Optional.empty());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3, Objects.requireNonNull(result.getBody()).size());
        assertEquals(questionnaireDto3, result.getBody().get(0));
        assertEquals(questionnaireDto1, result.getBody().get(1));
        assertEquals(questionnaireDto2, result.getBody().get(2));
    }

    @Test
    public void getQuestionnaire_questionnairesMissing_200() throws Exception {
        Mockito.when(questionnaireService.getQuestionnaires(List.of())).thenReturn(List.of());

        ResponseEntity<List<QuestionnaireDto>> result = subject.getQuestionnaires(Optional.empty());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(Objects.requireNonNull(result.getBody()).isEmpty());
    }

    @Test
    public void createQuestionnaire_success_201() {
        CreateQuestionnaireRequest request = new CreateQuestionnaireRequest();
        var questionnaireDto = new QuestionnaireDto();
        questionnaireDto.setQuestions(List.of(buildQuestionDto()));
        request.setQuestionnaire(questionnaireDto);

        Mockito.when(dtoMapper.mapQuestionnaireDto(request.getQuestionnaire())).thenReturn(QuestionnaireModel.builder().build());

        ResponseEntity<Void> result = subject.createQuestionnaire(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    public void createQuestionnaire_success_setsLocationHeader() throws Exception {
        CreateQuestionnaireRequest request = new CreateQuestionnaireRequest();
        var questionnaireDto = new QuestionnaireDto();
        questionnaireDto.setQuestions(List.of(buildQuestionDto()));
        request.setQuestionnaire(questionnaireDto);

        QuestionnaireModel questionnaireModel = QuestionnaireModel.builder().build();
        Mockito.when(dtoMapper.mapQuestionnaireDto(request.getQuestionnaire())).thenReturn(questionnaireModel);
        Mockito.when(questionnaireService.createQuestionnaire(questionnaireModel)).thenReturn("questionnaire-1");

        String location = "http://localhost:8080/api/v1/questionnaire/questionnaire-1";
        Mockito.when(locationHeaderBuilder.buildLocationHeader("questionnaire-1")).thenReturn(URI.create(location));

        ResponseEntity<Void> result = subject.createQuestionnaire(request);

        assertNotNull(result.getHeaders().get("Location"));
        assertEquals(location, result.getHeaders().get("Location").get(0));
    }

    @Test
    public void patchQuestionnaire_success_() throws Exception {
        PatchQuestionnaireRequest request = new PatchQuestionnaireRequest();
        request.setQuestions(List.of(buildQuestionDto()));

        ResponseEntity<Void> result = subject.patchQuestionnaire("questionnaire-1", request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    public void patchQuestionnaire_nullQuestions_throwsBadRequestException() throws Exception {
        String id = "questionnaire-1";
        PatchQuestionnaireRequest request = new PatchQuestionnaireRequest();
        request.setQuestions(null);

        assertThrows(BadRequestException.class, () -> subject.patchQuestionnaire(id, request));
    }

    @Test
    public void patchQuestionnaire_emptyQuestions_throwsBadRequestException() throws Exception {
        String id = "questionnaire-1";
        PatchQuestionnaireRequest request = new PatchQuestionnaireRequest();
        request.setQuestions(List.of());

        assertThrows(BadRequestException.class, () -> subject.patchQuestionnaire(id, request));
    }

    @Test
    public void patchQuestionnaire_accessViolation() throws Exception {
        String id = "questionnaire-1";
        String qualifyId = FhirUtils.qualifyId(id, ResourceType.Questionnaire);

        PatchQuestionnaireRequest request = new PatchQuestionnaireRequest();
        QuestionDto questionDto = buildQuestionDto();
        request.setQuestions(List.of(questionDto));

        QuestionModel questionModel = QuestionModel.builder().build();
        Mockito.when(dtoMapper.mapQuestion(questionDto)).thenReturn(questionModel);

        Mockito.doThrow(AccessValidationException.class).when(questionnaireService).updateQuestionnaire(qualifyId, null, null, null, List.of(questionModel), null);

        assertThrows(ForbiddenException.class, () -> subject.patchQuestionnaire(id, request));
    }


    private QuestionDto buildQuestionDto() {
        return new QuestionDto().questionType(QuestionDto.QuestionTypeEnum.BOOLEAN);
    }
}