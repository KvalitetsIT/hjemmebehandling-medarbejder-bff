package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ForbiddenException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcretePlanDefinitionService;
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
import org.openapitools.model.PatchPlanDefinitionRequest;
import org.openapitools.model.PlanDefinitionDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PlanDefinitionControllerTest {
    @InjectMocks
    private PlanDefinitionController subject;

    @Mock
    private ConcretePlanDefinitionService planDefinitionService;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private LocationHeaderBuilder locationHeaderBuilder;

    @Test
    public void getPlanDefinitions_planDefinitionsPresent_200() throws Exception {
        PlanDefinitionModel planDefinitionModel1 = PlanDefinitionModel.builder().build();
        PlanDefinitionModel planDefinitionModel2 = PlanDefinitionModel.builder().build();
        PlanDefinitionDto planDefinitionDto1 = new PlanDefinitionDto();
        PlanDefinitionDto planDefinitionDto2 = new PlanDefinitionDto();

        Mockito.when(planDefinitionService.getPlanDefinitions(List.of())).thenReturn(List.of(planDefinitionModel1, planDefinitionModel2));
        Mockito.when(dtoMapper.mapPlanDefinitionModel(planDefinitionModel1)).thenReturn(planDefinitionDto1);
        Mockito.when(dtoMapper.mapPlanDefinitionModel(planDefinitionModel2)).thenReturn(planDefinitionDto2);

        ResponseEntity<List<PlanDefinitionDto>> result = subject.getPlanDefinitions(Optional.empty());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, Objects.requireNonNull(result.getBody()).size());
        assertTrue(result.getBody().contains(planDefinitionDto1));
        assertTrue(result.getBody().contains(planDefinitionDto2));
    }

    @Test
    public void getPlanDefinitions_planDefinitionsMissing_200() throws Exception {
        Mockito.when(planDefinitionService.getPlanDefinitions(List.of())).thenReturn(List.of());

        ResponseEntity<List<PlanDefinitionDto>> result = subject.getPlanDefinitions(Optional.empty());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(Objects.requireNonNull(result.getBody()).isEmpty());
    }

    @Test
    public void getPlanDefinitions_failureToFetch_500() throws Exception {
        Mockito.when(planDefinitionService.getPlanDefinitions(List.of())).thenThrow(new ServiceException("Error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR));
        assertThrows(InternalServerErrorException.class, () -> subject.getPlanDefinitions(Optional.empty()));
    }


    @Test
    public void getPlanDefinitions_planDefinitionsMissing_400() throws Exception {
        assertThrows(BadRequestException.class, () -> subject.getPlanDefinitions(Optional.of(List.of())));
    }

    @Test
    public void getPlandefinitions_sorting() throws ServiceException {
        PlanDefinitionModel planDefinitionModel1 = PlanDefinitionModel.builder().build();
        PlanDefinitionModel planDefinitionModel2 = PlanDefinitionModel.builder().build();
        PlanDefinitionModel planDefinitionModel3 = PlanDefinitionModel.builder().build();
        PlanDefinitionDto planDefinitionDto1 = new PlanDefinitionDto();
        PlanDefinitionDto planDefinitionDto2 = new PlanDefinitionDto();
        PlanDefinitionDto planDefinitionDto3 = new PlanDefinitionDto();
        planDefinitionDto1.setLastUpdated(Optional.ofNullable(dtoMapper.mapInstant(Instant.now().minus(1, ChronoUnit.DAYS))));
        planDefinitionDto2.setLastUpdated(Optional.empty());
        planDefinitionDto3.setLastUpdated(Optional.ofNullable(dtoMapper.mapInstant(Instant.now())));

        Mockito.when(planDefinitionService.getPlanDefinitions(List.of())).thenReturn(List.of(planDefinitionModel1, planDefinitionModel2, planDefinitionModel3));
        Mockito.when(dtoMapper.mapPlanDefinitionModel(planDefinitionModel1)).thenReturn(planDefinitionDto1);
        Mockito.when(dtoMapper.mapPlanDefinitionModel(planDefinitionModel2)).thenReturn(planDefinitionDto2);
        Mockito.when(dtoMapper.mapPlanDefinitionModel(planDefinitionModel3)).thenReturn(planDefinitionDto3);

        ResponseEntity<List<PlanDefinitionDto>> result = subject.getPlanDefinitions(Optional.empty());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3, Objects.requireNonNull(result.getBody()).size());
        assertEquals(planDefinitionDto3, result.getBody().get(0));
        assertEquals(planDefinitionDto1, result.getBody().get(1));
        assertEquals(planDefinitionDto2, result.getBody().get(2));
    }

    @Test
    public void createPlanDefinition_success_201() {
        CreatePlanDefinitionRequest request = new CreatePlanDefinitionRequest();
        request.setPlanDefinition(new PlanDefinitionDto());

        Mockito.when(dtoMapper.mapPlanDefinitionDto(request.getPlanDefinition())).thenReturn(PlanDefinitionModel.builder().build());

        ResponseEntity<Void> result = subject.createPlanDefinition(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    public void createPlanDefinition_success_setsLocationHeader() throws Exception {

        CreatePlanDefinitionRequest request = new CreatePlanDefinitionRequest();
        request.setPlanDefinition(new PlanDefinitionDto());

        PlanDefinitionModel planDefinitionModel = PlanDefinitionModel.builder().build();
        Mockito.when(dtoMapper.mapPlanDefinitionDto(request.getPlanDefinition())).thenReturn(planDefinitionModel);
        Mockito.when(planDefinitionService.createPlanDefinition(planDefinitionModel)).thenReturn("plandefinition-1");

        String location = "http://localhost:8080/api/v1/plandefinition/plandefinition-1";
        Mockito.when(locationHeaderBuilder.buildLocationHeader("plandefinition-1")).thenReturn(URI.create(location));

        ResponseEntity<Void> result = subject.createPlanDefinition(request);

        assertNotNull(result.getHeaders().get("Location"));
        assertEquals(location, Objects.requireNonNull(result.getHeaders().get("Location")).getFirst());
    }

    @Test
    public void createPlanDefinition_accessViolation_403() throws Exception {
        CreatePlanDefinitionRequest request = new CreatePlanDefinitionRequest();
        request.setPlanDefinition(new PlanDefinitionDto());

        PlanDefinitionModel planDefinitionModel =  PlanDefinitionModel.builder().build();
        Mockito.when(dtoMapper.mapPlanDefinitionDto(request.getPlanDefinition())).thenReturn(planDefinitionModel);

        Mockito.when(planDefinitionService.createPlanDefinition(planDefinitionModel)).thenThrow(AccessValidationException.class);

        assertThrows(ForbiddenException.class, () -> subject.createPlanDefinition(request));
    }

    @Test
    public void updatePlanDefinition_throwsUnsupportedOperationException() {
        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();

        assertThrows(UnsupportedOperationException.class, () -> subject.updatePlanDefinition(planDefinitionDto));
    }

    @Test
    public void patchPlanDefinition_success() throws Exception {
        PatchPlanDefinitionRequest request = new PatchPlanDefinitionRequest();

        ResponseEntity<Void> result = subject.patchPlanDefinition("plandefinition-1", request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}