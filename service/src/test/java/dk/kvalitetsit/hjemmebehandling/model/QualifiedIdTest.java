package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QualifiedIdTest {
    @Test
    public void getId_plainId_returnsId() {
        // Arrange
        String id = "2";
        ResourceType qualifier = ResourceType.CarePlan;

        // Act
        String result = new QualifiedId(id, qualifier).getId();

        // Assert
        assertEquals(id, result);
    }

    @Test
    public void getId_multipleSlashes_throwsException() {
        // Arrange
        String qualifiedId = "Patient/2/3";

        // Act

        // Assert
        assertThrows(IllegalArgumentException.class, () -> new QualifiedId(qualifiedId).getId());
    }

    @Test
    public void getId_illegalQualifier_throwsException() {
        // Arrange
        String qualifiedId = "Car/2";

        // Act

        // Assert
        assertThrows(IllegalArgumentException.class, () -> new QualifiedId(qualifiedId).getId());
    }

    @Test
    public void getId_illegalId_throwsException() {
        // Arrange
        String id = "###";
        ResourceType qualifier = ResourceType.Questionnaire;

        // Act

        // Assert
        assertThrows(IllegalArgumentException.class, () -> new QualifiedId(id, qualifier).getId());
    }

    @Test
    public void getId_validQualifiedId_returnsPlainPart() {
        // Arrange
        String qualifiedId = "CarePlan/3";

        // Act
        String result = new QualifiedId(qualifiedId).getId();

        // Assert
        assertEquals("3", result);
    }

    @Test
    public void getQualifier_malformedId_throwsException() {
        // Arrange
        String qualifiedId = "Patient/()";

        // Act

        // Assert
        assertThrows(IllegalArgumentException.class, () -> new QualifiedId(qualifiedId).getQualifier());
    }

    @Test
    public void toString_success() {
        // Arrange
        String id = "2";
        ResourceType qualifier = ResourceType.Patient;

        // Act
        String result = new QualifiedId(id, qualifier).toString();

        // Assert
        assertEquals("Patient/2", result);
    }
}