package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ForbiddenException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.service.PlanDefinitionService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.CreatePlanDefinitionRequest;
import org.openapitools.model.PlanDefinitionDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PlanDefinitionControllerTest {
    @InjectMocks
    private PlanDefinitionController subject;

    @Mock
    private PlanDefinitionService planDefinitionService;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private LocationHeaderBuilder locationHeaderBuilder;

    @Test
    public void getPlanDefinitions_planDefinitionsPresent_200() throws Exception {
        // Arrange
        PlanDefinitionModel planDefinitionModel1 = new PlanDefinitionModel();
        PlanDefinitionModel planDefinitionModel2 = new PlanDefinitionModel();
        PlanDefinitionDto planDefinitionDto1 = new PlanDefinitionDto();
        PlanDefinitionDto planDefinitionDto2 = new PlanDefinitionDto();

        Mockito.when(planDefinitionService.getPlanDefinitions(List.of())).thenReturn(List.of(planDefinitionModel1, planDefinitionModel2));
        Mockito.when(dtoMapper.mapPlanDefinitionModel(planDefinitionModel1)).thenReturn(planDefinitionDto1);
        Mockito.when(dtoMapper.mapPlanDefinitionModel(planDefinitionModel2)).thenReturn(planDefinitionDto2);

        // Act
        ResponseEntity<List<PlanDefinitionDto>> result = subject.getPlanDefinitions(Optional.empty());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(planDefinitionDto1));
        assertTrue(result.getBody().contains(planDefinitionDto2));
    }

    @Test
    public void getPlanDefinitions_planDefinitionsMissing_200() throws Exception {
        // Arrange
        Mockito.when(planDefinitionService.getPlanDefinitions(List.of())).thenReturn(List.of());

        // Act
        ResponseEntity<List<PlanDefinitionDto>> result = subject.getPlanDefinitions(Optional.empty());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    public void getPlanDefinitions_failureToFetch_500() throws Exception {
        // Arrange
        Mockito.when(planDefinitionService.getPlanDefinitions(List.of())).thenThrow(new ServiceException("Error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR));

        // Act

        // Assert

        assertThrows(InternalServerErrorException.class, () -> subject.getPlanDefinitions(Optional.empty()));
    }

    @Test
    public void getPlandefinitions_sorting() throws ServiceException {
        // Arrange
        PlanDefinitionModel planDefinitionModel1 = new PlanDefinitionModel();
        PlanDefinitionModel planDefinitionModel2 = new PlanDefinitionModel();
        PlanDefinitionModel planDefinitionModel3 = new PlanDefinitionModel();
        PlanDefinitionDto planDefinitionDto1 = new PlanDefinitionDto();
        PlanDefinitionDto planDefinitionDto2 = new PlanDefinitionDto();
        PlanDefinitionDto planDefinitionDto3 = new PlanDefinitionDto();
        planDefinitionDto1.setLastUpdated(dtoMapper.mapInstant(Instant.now().minus(1, ChronoUnit.DAYS)));
        planDefinitionDto2.setLastUpdated(null);
        planDefinitionDto3.setLastUpdated(dtoMapper.mapInstant(Instant.now()));

        Mockito.when(planDefinitionService.getPlanDefinitions(Collections.emptyList())).thenReturn(List.of(planDefinitionModel1, planDefinitionModel2, planDefinitionModel3));
        Mockito.when(dtoMapper.mapPlanDefinitionModel(planDefinitionModel1)).thenReturn(planDefinitionDto1);
        Mockito.when(dtoMapper.mapPlanDefinitionModel(planDefinitionModel2)).thenReturn(planDefinitionDto2);
        Mockito.when(dtoMapper.mapPlanDefinitionModel(planDefinitionModel3)).thenReturn(planDefinitionDto3);

        // Act
        ResponseEntity<List<PlanDefinitionDto>> result = subject.getPlanDefinitions(Optional.empty());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3, result.getBody().size());
        assertEquals(planDefinitionDto3, result.getBody().get(0));
        assertEquals(planDefinitionDto1, result.getBody().get(1));
        assertEquals(planDefinitionDto2, result.getBody().get(2));
    }

    @Test
    public void createPlanDefinition_success_201() {
        // Arrange
        CreatePlanDefinitionRequest request = new CreatePlanDefinitionRequest();
        request.setPlanDefinition(new PlanDefinitionDto());

        Mockito.when(dtoMapper.mapPlanDefinitionDto(request.getPlanDefinition())).thenReturn(new PlanDefinitionModel());

        // Act
        ResponseEntity<Void> result = subject.createPlanDefinition(request);

        // Assert
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    public void createPlanDefinition_success_setsLocationHeader() throws Exception {
        // Arrange
        CreatePlanDefinitionRequest request = new CreatePlanDefinitionRequest();
        request.setPlanDefinition(new PlanDefinitionDto());

        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        Mockito.when(dtoMapper.mapPlanDefinitionDto(request.getPlanDefinition())).thenReturn(planDefinitionModel);
        Mockito.when(planDefinitionService.createPlanDefinition(planDefinitionModel)).thenReturn("plandefinition-1");

        String location = "http://localhost:8080/api/v1/plandefinition/plandefinition-1";
        Mockito.when(locationHeaderBuilder.buildLocationHeader("plandefinition-1")).thenReturn(URI.create(location));

        // Act
        ResponseEntity<Void> result = subject.createPlanDefinition(request);

        // Assert
        assertNotNull(result.getHeaders().get("Location"));
        assertEquals(location, result.getHeaders().get("Location").get(0));
    }

    @Test
    public void createPlanDefinition_accessViolation_403() throws Exception {
        // Arrange
        CreatePlanDefinitionRequest request = new CreatePlanDefinitionRequest();
        request.setPlanDefinition(new PlanDefinitionDto());

        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        Mockito.when(dtoMapper.mapPlanDefinitionDto(request.getPlanDefinition())).thenReturn(planDefinitionModel);

        Mockito.when(planDefinitionService.createPlanDefinition(planDefinitionModel)).thenThrow(AccessValidationException.class);

        // Act

        // Assert
        assertThrows(ForbiddenException.class, () -> subject.createPlanDefinition(request));
    }

    @Test
    public void updatePlanDefinition_throwsUnsupportedOperationException() {
        // Arrange
        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();

        // Act

        // Assert
        assertThrows(UnsupportedOperationException.class, () -> subject.updatePlanDefinition(planDefinitionDto));
    }

    @Test
    public void patchPlanDefinition_success() throws Exception {
        // Arrange
        PatchPlanDefinitionRequest request = new PatchPlanDefinitionRequest();
        //Mockito.when(planDefinitionService.updatePlanDefinition("plandefinition-1", Mockito.anyString(), Mockito.anyList(), Mockito.anyList())).thenReturn(new PlanDefinitionModel());

        // Act
        ResponseEntity<Void> result = subject.patchPlanDefinition("plandefinition-1", request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}