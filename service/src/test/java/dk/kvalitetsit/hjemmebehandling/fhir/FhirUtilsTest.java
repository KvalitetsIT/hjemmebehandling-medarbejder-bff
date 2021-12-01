package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FhirUtilsTest {
    @Test
    public void qualifyId_qualifiedId_returnsId() {
        // Arrange
        String id = "CarePlan/4";

        // Act
        String result = FhirUtils.qualifyId(id, ResourceType.CarePlan);

        // Assert
        assertEquals(id, result);
    }

    @Test
    public void qualifyId_qualifierMismatch_throwsException() {
        // Arrange
        String id = "CarePlan/4";

        // Act

        // Assert
        assertThrows(IllegalArgumentException.class, () ->  FhirUtils.qualifyId(id, ResourceType.Questionnaire));
    }

    @Test
    public void qualifyId_malformedId_throwsException() {
        // Arrange
        String id = "Patient/()";

        // Act

        // Assert
        assertThrows(IllegalArgumentException.class, () -> FhirUtils.qualifyId(id, ResourceType.Patient));
    }

    @Test
    public void qualifyId_plainId_success() {
        // Arrange
        String id = "2";

        // Act
        String result = FhirUtils.qualifyId(id, ResourceType.Patient);

        // Assert
        assertEquals("Patient/2", result);
    }

    @Test
    public void isPlainId_plainId_success() {
        // Arrange
        String id = "2";

        // Act

        // Assert
        assertTrue(FhirUtils.isPlainId(id));
    }

    @Test
    public void isPlainId_slashes_failure() {
        // Arrange
        String id = "CarePlan/2";

        // Act

        // Assert
        assertFalse(FhirUtils.isPlainId(id));
    }

    @Test
    public void isQualifiedId_qualifiedId_success() {
        // Arrange
        String qualifiedId = "CarePlan/2";
        ResourceType qualifier = ResourceType.CarePlan;

        // Act

        // Assert
        assertTrue(FhirUtils.isQualifiedId(qualifiedId, qualifier));
    }

    @Test
    public void isQualifiedId_multipleSlashes_failure() {
        // Arrange
        String qualifiedId = "Patient/2/3";
        ResourceType qualifier = ResourceType.Patient;

        // Act

        // Assert
        assertFalse(FhirUtils.isQualifiedId(qualifiedId, qualifier));
    }

    @Test
    public void isQualifiedId_illegalQualifier_failure() {
        // Arrange
        String qualifiedId = "Car/2";
        ResourceType qualifier = ResourceType.Patient;

        // Act

        // Assert
        assertFalse(FhirUtils.isQualifiedId(qualifiedId, qualifier));
    }

    @Test
    public void isQualifiedId_illegalId_failure() {
        // Arrange
        String qualifiedId = "Questionnaire/###";;
        ResourceType qualifier = ResourceType.Questionnaire;

        // Act

        // Assert
        assertFalse(FhirUtils.isQualifiedId(qualifiedId, qualifier));
    }
}