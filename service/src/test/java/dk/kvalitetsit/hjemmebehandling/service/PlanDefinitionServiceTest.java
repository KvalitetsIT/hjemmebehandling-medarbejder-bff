package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PlanDefinitionServiceTest {
    @InjectMocks
    private PlanDefinitionService subject;

    @Mock
    private FhirClient fhirClient;

    @Mock
    private FhirMapper fhirMapper;

    @Test
    public void getPlanDefinitions_sucecss() throws Exception {
        // Arrange
        PlanDefinition planDefinition = new PlanDefinition();
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        Mockito.when(fhirClient.lookupPlanDefinitions()).thenReturn(List.of(planDefinition));
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition)).thenReturn(planDefinitionModel);

        // Act
        List<PlanDefinitionModel> result = subject.getPlanDefinitions();

        // Assert
        assertEquals(1, result.size());
        assertEquals(planDefinitionModel, result.get(0));
    }
}