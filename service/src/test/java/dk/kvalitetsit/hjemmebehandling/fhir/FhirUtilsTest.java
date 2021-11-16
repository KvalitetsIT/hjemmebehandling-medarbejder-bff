package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FhirUtilsTest {
    @Test
    public void unqualify_plainId_returnsId() {
        // Arrange
        String id = "2";

        // Act
        String result = FhirUtils.unqualifyId(id);

        // Assert
        assertEquals(id, result);
    }

    @Test
    public void unqualify_multipleSlashes_throwsException() {
        // Arrange
        String id = "Patient/2/3";

        // Act

        // Assert
        assertThrows(IllegalArgumentException.class, () -> FhirUtils.unqualifyId(id));
    }

    @Test
    public void unqualify_illegalQualifier_throwsException() {
        // Arrange
        String id = "Car/2";

        // Act

        // Assert
        assertThrows(IllegalArgumentException.class, () -> FhirUtils.unqualifyId(id));
    }

    @Test
    public void unqualify_illegalId_throwsException() {
        // Arrange
        String id = "Questionnaire/###";

        // Act

        // Assert
        assertThrows(IllegalArgumentException.class, () -> FhirUtils.unqualifyId(id));
    }

    @Test
    public void unqualify_validQualifiedId_returnsPlainPart() {
        // Arrange
        String id = "CarePlan/3";

        // Act
        String result = FhirUtils.unqualifyId(id);

        // Assert
        assertEquals("3", result);
    }

    @Test
    public void qualify_qualifiedId_returnsId() {
        // Arrange
        String id = "CarePlan/4";

        // Act
        String result = FhirUtils.qualifyId(id, ResourceType.CarePlan);

        // Assert
        assertEquals(id, result);
    }

    @Test
    public void qualify_qualifierMismatch_throwsException() {
        // Arrange
        String id = "CarePlan/4";

        // Act

        // Assert
        assertThrows(IllegalArgumentException.class, () ->  FhirUtils.qualifyId(id, ResourceType.Questionnaire));
    }

    @Test
    public void qualify_malformedId_throwsException() {
        // Arrange
        String id = "Patient/()";

        // Act

        // Assert
        assertThrows(IllegalArgumentException.class, () -> FhirUtils.qualifyId(id, ResourceType.Patient));
    }

    @Test
    public void qualify_plainId_success() {
        // Arrange
        String id = "2";

        // Act
        String result = FhirUtils.qualifyId(id, ResourceType.Patient);

        // Assert
        assertEquals("Patient/2", result);
    }
}