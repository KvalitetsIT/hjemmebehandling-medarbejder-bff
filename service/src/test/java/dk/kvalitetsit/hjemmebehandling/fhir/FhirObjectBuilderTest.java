package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FhirObjectBuilderTest {
    private FhirObjectBuilder subject;

    @BeforeEach
    public void setup() {
        subject = new FhirObjectBuilder();
    }

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
}