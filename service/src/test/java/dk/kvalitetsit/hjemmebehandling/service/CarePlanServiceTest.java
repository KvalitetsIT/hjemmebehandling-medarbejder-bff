package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import org.hl7.fhir.r4.model.CarePlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CarePlanServiceTest {
    @InjectMocks
    private CarePlanService subject;

    @Mock
    private FhirClient fhirClient;

    @Mock
    private FhirMapper fhirMapper;

    @Mock
    private FhirObjectBuilder fhirObjectBuilder;

    @Test
    public void getCarePlan_carePlanPresent_returnsCarePlan() {
        // Arrange
        String carePlanId = "careplan-1";

        CarePlan carePlan = new CarePlan();
        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(fhirClient.lookupCarePlan(carePlanId)).thenReturn(Optional.of(carePlan));
        Mockito.when(fhirMapper.mapCarePlan(carePlan)).thenReturn(carePlanModel);

        // Act
        Optional<CarePlanModel> result = subject.getCarePlan(carePlanId);

        // Assert
        assertEquals(carePlanModel, result.get());
    }

    @Test
    public void getCarePlan_carePlanMissing_returnsEmpty() {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.when(fhirClient.lookupCarePlan(carePlanId)).thenReturn(Optional.empty());

        // Act
        Optional<CarePlanModel> result = subject.getCarePlan(carePlanId);

        // Assert
        assertFalse(result.isPresent());
    }
}