package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.PlanDefinitionDto;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.service.PlanDefinitionService;
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
public class PlanDefinitionControllerTest {
    @InjectMocks
    private PlanDefinitionController subject;

    @Mock
    private PlanDefinitionService planDefinitionService;

    @Mock
    private DtoMapper dtoMapper;

    @Test
    public void getPlanDefinitions_planDefinitionsPresent_200() throws Exception {
        // Arrange
        PlanDefinitionModel planDefinitionModel1 = new PlanDefinitionModel();
        PlanDefinitionModel planDefinitionModel2 = new PlanDefinitionModel();
        PlanDefinitionDto planDefinitionDto1 = new PlanDefinitionDto();
        PlanDefinitionDto planDefinitionDto2 = new PlanDefinitionDto();

        Mockito.when(planDefinitionService.getPlanDefinitions()).thenReturn(List.of(planDefinitionModel1, planDefinitionModel2));
        Mockito.when(dtoMapper.mapPlanDefinitionModel(planDefinitionModel1)).thenReturn(planDefinitionDto1);
        Mockito.when(dtoMapper.mapPlanDefinitionModel(planDefinitionModel2)).thenReturn(planDefinitionDto2);

        // Act
        ResponseEntity<List<PlanDefinitionDto>> result = subject.getPlanDefinitions();

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(planDefinitionDto1));
        assertTrue(result.getBody().contains(planDefinitionDto2));
    }

    @Test
    public void getPlanDefinitions_planDefinitionsMissing_204() throws Exception {
        // Arrange
        Mockito.when(planDefinitionService.getPlanDefinitions()).thenReturn(List.of());

        // Act
        ResponseEntity<List<PlanDefinitionDto>> result = subject.getPlanDefinitions();

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    public void getPlanDefinitions_failureToFetch_500() throws Exception {
        // Arrange
        Mockito.when(planDefinitionService.getPlanDefinitions()).thenThrow(ServiceException.class);

        // Act
        ResponseEntity<List<PlanDefinitionDto>> result = subject.getPlanDefinitions();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }
}