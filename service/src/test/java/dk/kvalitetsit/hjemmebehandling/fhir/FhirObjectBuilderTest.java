package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class FhirObjectBuilderTest {
    @InjectMocks
    private FhirObjectBuilder subject;

    @Mock
    private DateProvider dateProvider;

    @Test
    public void buildCarePlan_mapsInstantiatesCanonical() {
        // Arrange
        Patient patient = new Patient();
        PlanDefinition planDefinition = new PlanDefinition();
        planDefinition.setId("PlanDefinition/plandefinition-1");

        // Act
        CarePlan result = subject.buildCarePlan(patient, planDefinition);

        // Assert
        assertEquals(1, result.getInstantiatesCanonical().size());
        assertEquals("PlanDefinition/plandefinition-1", result.getInstantiatesCanonical().get(0).getValue());
    }

    @Test
    public void buildCarePlan_setsCreated() {
        // Arrange
        Patient patient = new Patient();
        PlanDefinition planDefinition = new PlanDefinition();

        Mockito.when(dateProvider.now()).thenReturn(Date.valueOf("2021-11-09"));

        // Act
        CarePlan result = subject.buildCarePlan(patient, planDefinition);

        // Assert
        assertEquals(Date.valueOf("2021-11-09"), result.getCreated());
    }
}