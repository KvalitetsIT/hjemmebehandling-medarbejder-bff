package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExtensionMapperTest {
    @Test
    public void mapActivitySatisfiedUntil_success() {
        // Arrange
        Instant pointInTime = Instant.parse("2021-11-07T10:11:12.124Z");

        // Act
        Extension result = ExtensionMapper.mapActivitySatisfiedUntil(pointInTime);

        // Assert
        assertEquals(Systems.ACTIVITY_SATISFIED_UNTIL, result.getUrl());
        assertEquals(pointInTime.toString(), result.getValue().toString());
    }

    @Test
    public void mapCarePlanSatisfiedUntil_success() {
        // Arrange
        Instant pointInTime = Instant.parse("2021-12-07T10:11:12.124Z");

        // Act
        Extension result = ExtensionMapper.mapCarePlanSatisfiedUntil(pointInTime);

        // Assert
        assertEquals(Systems.CAREPLAN_SATISFIED_UNTIL, result.getUrl());
        assertEquals(pointInTime.toString(), result.getValue().toString());
    }

    @Test
    public void extractActivitySatisfiedUntil_success() {
        // Arrange
        Extension extension = new Extension(Systems.ACTIVITY_SATISFIED_UNTIL, new StringType("2021-12-07T10:11:12.124Z"));

        // Act
        Instant result = ExtensionMapper.extractActivitySatisfiedUntil(List.of(extension));

        // Assert
        assertEquals(Instant.parse("2021-12-07T10:11:12.124Z"), result);
    }

    @Test
    public void extractExaminationStatus_success() {
        // Arrange
        Extension extension = new Extension(Systems.EXAMINATION_STATUS, new StringType(ExaminationStatus.EXAMINED.toString()));

        // Act
        ExaminationStatus result = ExtensionMapper.extractExaminationStatus(List.of(extension));

        // Assert
        assertEquals(ExaminationStatus.EXAMINED, result);
    }

    @Test
    public void extractActivitySatisfiedUntil_illegalDate() {
        // Arrange
        Extension extension = new Extension(Systems.ACTIVITY_SATISFIED_UNTIL, new StringType("next monday"));

        // Act

        // Assert
        assertThrows(DateTimeParseException.class, () -> ExtensionMapper.extractActivitySatisfiedUntil(List.of(extension)));
    }
}