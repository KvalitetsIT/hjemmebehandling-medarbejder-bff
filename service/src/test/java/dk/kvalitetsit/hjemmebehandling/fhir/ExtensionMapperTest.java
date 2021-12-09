package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
        assertEquals(Date.from(pointInTime), ((DateTimeType) result.getValue()).getValue());
    }

    @Test
    public void mapCarePlanSatisfiedUntil_success() {
        // Arrange
        Instant pointInTime = Instant.parse("2021-12-07T10:11:12.124Z");

        // Act
        Extension result = ExtensionMapper.mapCarePlanSatisfiedUntil(pointInTime);

        // Assert
        assertEquals(Systems.CAREPLAN_SATISFIED_UNTIL, result.getUrl());
        assertEquals(Date.from(pointInTime), ((DateTimeType) result.getValue()).getValue());
    }

    @Test
    public void mapOrganizationId_success() {
        // Arrange
        String organizationId = "Organization/organization-1";

        // Act
        Extension result = ExtensionMapper.mapOrganizationId(organizationId);

        // Assert
        assertEquals(Systems.ORGANIZATION, result.getUrl());
        assertEquals(Reference.class, result.getValue().getClass());
        assertEquals(organizationId, ((Reference) result.getValue()).getReference());
    }

    @Test
    public void extractActivitySatisfiedUntil_success() {
        // Arrange
        Extension extension = new Extension(Systems.ACTIVITY_SATISFIED_UNTIL, new DateTimeType(Date.from(Instant.parse("2021-12-07T10:11:12.124Z"))));

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
    public void extractOrganizationId_success() {
        // Arrange
        Extension extension = new Extension(Systems.ORGANIZATION, new Reference("Organization/organization-1"));

        // Act
        String result = ExtensionMapper.extractOrganizationId(List.of(extension));

        // Assert
        assertEquals("Organization/organization-1", result);
    }

    @Test
    public void tryExtractOrganizationId_idPresent_success() {
        // Arrange
        Extension extension = new Extension(Systems.ORGANIZATION, new Reference("Organization/organization-1"));

        // Act
        Optional<String> result = ExtensionMapper.tryExtractOrganizationId(List.of(extension));

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Organization/organization-1", result.get());
    }

    @Test
    public void tryExtractOrganizationId_idMissing_success() {
        // Arrange

        // Act
        Optional<String> result = ExtensionMapper.tryExtractOrganizationId(List.of());

        // Assert
        assertFalse(result.isPresent());
    }
}