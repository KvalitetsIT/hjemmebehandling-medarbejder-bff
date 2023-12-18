package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ExtensionMapperTest {

    @ParameterizedTest
    @ValueSource(strings = {Systems.CAREPLAN_SATISFIED_UNTIL, Systems.ACTIVITY_SATISFIED_UNTIL})
    public void mapActivitySatisfiedUntil_maxValue_mapsCorrect_success(String extensionUrl) {
        // Arrange
        Instant satisfiedUntil = Instant.MAX;

        // Act
        Extension result = null;
        if (extensionUrl.equals(Systems.ACTIVITY_SATISFIED_UNTIL)) {
            result = ExtensionMapper.mapActivitySatisfiedUntil(satisfiedUntil);
        }
        else if (extensionUrl.equals(Systems.CAREPLAN_SATISFIED_UNTIL)) {
            result = ExtensionMapper.mapCarePlanSatisfiedUntil(satisfiedUntil);
        }

        // Assert
        assertEquals(extensionUrl, result.getUrl());
        Instant expected = ExtensionMapper.MAX_SATISFIED_UNTIL_DATE.toInstant(ZoneId.of("Europe/Copenhagen").getRules().getOffset(Instant.now()));
        assertEquals(Date.from(expected), ((DateTimeType) result.getValue()).getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {Systems.CAREPLAN_SATISFIED_UNTIL, Systems.ACTIVITY_SATISFIED_UNTIL})
    public void extractActivitySatisfiedUntil_maxValue_mapsCorrect_success(String extensionUrl) {
        // Arrange
        Instant satisfiedUntilMax = ExtensionMapper.MAX_SATISFIED_UNTIL_DATE.toInstant(ZoneId.of("Europe/Copenhagen").getRules().getOffset(Instant.now()));
        Extension extension = new Extension(extensionUrl, new DateTimeType(Date.from(satisfiedUntilMax)));

        // Act
        Instant result = null;
        if (extensionUrl.equals(Systems.ACTIVITY_SATISFIED_UNTIL)) {
            result = ExtensionMapper.extractActivitySatisfiedUntil(List.of(extension));
        }
        else if (extensionUrl.equals(Systems.CAREPLAN_SATISFIED_UNTIL)) {
            result = ExtensionMapper.extractCarePlanSatisfiedUntil(List.of(extension));
        }

        // Assert
        Instant expected = Instant.MAX;
        assertEquals(expected, result);
    }

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
    public void mapOrganizationDeadlineTimeDefault_success() {
        // Arrange
        TimeType defaultTime = new TimeType("11:00");

        // Act
        Extension result = ExtensionMapper.mapOrganizationDeadlineTimeDefault(defaultTime);
        Organization organization = new Organization();
        organization.addExtension(result);
        System.out.println( FhirContext.forR4().newXmlParser().setPrettyPrint(true).encodeResourceToString(organization) );

        // Assert
        assertEquals(Systems.ORGANIZATION_QUESTIONNAIRE_DEADLINE_TIME_DEFAULT, result.getUrl());
        assertEquals(TimeType.class, result.getValue().getClass());
        assertEquals(defaultTime,  result.getValue());
        assertEquals(defaultTime.getValue(), ((TimeType) result.getValue()).getValue());
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
    public void extractOrganizationDeadlineTimeDefault_success() {
        // Arrange
        TimeType time = new TimeType("11:00");
        Extension extension = new Extension(Systems.ORGANIZATION_QUESTIONNAIRE_DEADLINE_TIME_DEFAULT, time);

        // Act
        TimeType result = ExtensionMapper.extractOrganizationDeadlineTimeDefault(List.of(extension));

        // Assert
        assertEquals(time, result);
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

    @Test
    public void mapThreshold_boolean() {
        // Arrange
        ThresholdModel threshold = new ThresholdModel();
        threshold.setQuestionnaireItemLinkId("foo");
        threshold.setType(ThresholdType.NORMAL);
        threshold.setValueBoolean(true);

        // Act
        Extension result = ExtensionMapper.mapThreshold(threshold);

        // Assert
        assertEquals("foo", result.getExtensionString(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID));
        assertEquals(ThresholdType.NORMAL.toString(), result.getExtensionString(Systems.THRESHOLD_TYPE));
        assertTrue(((BooleanType) result.getExtensionByUrl(Systems.THRESHOLD_VALUE_BOOLEAN).getValue()).booleanValue());
    }

    @Test
    public void mapThreshold_range() {
        // Arrange
        ThresholdModel threshold = new ThresholdModel();
        threshold.setQuestionnaireItemLinkId("bar");
        threshold.setType(ThresholdType.ABNORMAL);
        threshold.setValueQuantityLow(2.0);
        threshold.setValueQuantityHigh(4.0);

        // Act
        Extension result = ExtensionMapper.mapThreshold(threshold);

        // Assert
        assertEquals("bar", result.getExtensionString(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID));
        assertEquals(ThresholdType.ABNORMAL.toString(), result.getExtensionString(Systems.THRESHOLD_TYPE));
        assertEquals(2.0, ((Range) result.getExtensionByUrl(Systems.THRESHOLD_VALUE_RANGE).getValue()).getLow().getValue().doubleValue());
        assertEquals(4.0, ((Range) result.getExtensionByUrl(Systems.THRESHOLD_VALUE_RANGE).getValue()).getHigh().getValue().doubleValue());
    }

    @Test
    public void extractThreshold_boolean() {
        // Arrange
        Extension extension = new Extension();
        extension.addExtension(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID, new StringType("foo"));
        extension.addExtension(Systems.THRESHOLD_TYPE, new StringType(ThresholdType.NORMAL.toString()));
        extension.addExtension(Systems.THRESHOLD_VALUE_BOOLEAN, new BooleanType(true));

        // Act
        ThresholdModel result = ExtensionMapper.extractThreshold(extension);

        // Assert
        assertEquals("foo", result.getQuestionnaireItemLinkId());
        assertEquals(ThresholdType.NORMAL, result.getType());
        assertTrue(result.getValueBoolean());
    }

    @Test
    public void extractThreshold_range() {
        // Arrange
        Extension extension = new Extension();
        extension.addExtension(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID, new StringType("bar"));
        extension.addExtension(Systems.THRESHOLD_TYPE, new StringType(ThresholdType.ABNORMAL.toString()));
        Range r = new Range();
        r.setLow(new Quantity(2.0));
        r.setHigh(new Quantity(4.0));
        extension.addExtension(Systems.THRESHOLD_VALUE_RANGE, r);

        // Act
        ThresholdModel result = ExtensionMapper.extractThreshold(extension);

        // Assert
        assertEquals("bar", result.getQuestionnaireItemLinkId());
        assertEquals(ThresholdType.ABNORMAL, result.getType());
        assertEquals(2.0, result.getValueQuantityLow());
        assertEquals(4.0, result.getValueQuantityHigh());
    }
}