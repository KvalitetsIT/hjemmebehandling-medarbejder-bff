package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
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
        Instant satisfiedUntil = Instant.MAX;

        Extension result = null;
        if (extensionUrl.equals(Systems.ACTIVITY_SATISFIED_UNTIL)) {
            result = ExtensionMapper.mapActivitySatisfiedUntil(satisfiedUntil);
        } else if (extensionUrl.equals(Systems.CAREPLAN_SATISFIED_UNTIL)) {
            result = ExtensionMapper.mapCarePlanSatisfiedUntil(satisfiedUntil);
        }

        assertEquals(extensionUrl, result.getUrl());
        Instant expected = ExtensionMapper.MAX_SATISFIED_UNTIL_DATE.toInstant(ZoneId.of("Europe/Copenhagen").getRules().getOffset(Instant.now()));
        assertEquals(Date.from(expected), ((DateTimeType) result.getValue()).getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {Systems.CAREPLAN_SATISFIED_UNTIL, Systems.ACTIVITY_SATISFIED_UNTIL})
    public void extractActivitySatisfiedUntil_maxValue_mapsCorrect_success(String extensionUrl) {
        Instant satisfiedUntilMax = ExtensionMapper.MAX_SATISFIED_UNTIL_DATE.toInstant(ZoneId.of("Europe/Copenhagen").getRules().getOffset(Instant.now()));
        Extension extension = new Extension(extensionUrl, new DateTimeType(Date.from(satisfiedUntilMax)));

        Instant result = null;
        if (extensionUrl.equals(Systems.ACTIVITY_SATISFIED_UNTIL)) {
            result = ExtensionMapper.extractActivitySatisfiedUntil(List.of(extension));
        } else if (extensionUrl.equals(Systems.CAREPLAN_SATISFIED_UNTIL)) {
            result = ExtensionMapper.extractCarePlanSatisfiedUntil(List.of(extension));
        }

        Instant expected = Instant.MAX;
        assertEquals(expected, result);
    }

    @Test
    public void mapActivitySatisfiedUntil_success() {
        Instant pointInTime = Instant.parse("2021-11-07T10:11:12.124Z");
        Extension result = ExtensionMapper.mapActivitySatisfiedUntil(pointInTime);
        assertEquals(Systems.ACTIVITY_SATISFIED_UNTIL, result.getUrl());
        assertEquals(Date.from(pointInTime), ((DateTimeType) result.getValue()).getValue());
    }

    @Test
    public void mapCarePlanSatisfiedUntil_success() {
        Instant pointInTime = Instant.parse("2021-12-07T10:11:12.124Z");
        Extension result = ExtensionMapper.mapCarePlanSatisfiedUntil(pointInTime);
        assertEquals(Systems.CAREPLAN_SATISFIED_UNTIL, result.getUrl());
        assertEquals(Date.from(pointInTime), ((DateTimeType) result.getValue()).getValue());
    }

    @Test
    public void mapOrganizationId_success() {
        String organizationId = "Organization/organization-1";
        Extension result = ExtensionMapper.mapOrganizationId(organizationId);

        assertEquals(Systems.ORGANIZATION, result.getUrl());
        assertEquals(Reference.class, result.getValue().getClass());
        assertEquals(organizationId, ((Reference) result.getValue()).getReference());
    }

    @Test
    public void mapOrganizationDeadlineTimeDefault_success() {
        TimeType defaultTime = new TimeType("11:00");

        Extension result = ExtensionMapper.mapOrganizationDeadlineTimeDefault(defaultTime);
        Organization organization = new Organization();
        organization.addExtension(result);
        System.out.println(FhirContext.forR4().newXmlParser().setPrettyPrint(true).encodeResourceToString(organization));

        assertEquals(Systems.ORGANIZATION_QUESTIONNAIRE_DEADLINE_TIME_DEFAULT, result.getUrl());
        assertEquals(TimeType.class, result.getValue().getClass());
        assertEquals(defaultTime, result.getValue());
        assertEquals(defaultTime.getValue(), ((TimeType) result.getValue()).getValue());
    }

    @Test
    public void extractActivitySatisfiedUntil_success() {
        Extension extension = new Extension(Systems.ACTIVITY_SATISFIED_UNTIL, new DateTimeType(Date.from(Instant.parse("2021-12-07T10:11:12.124Z"))));
        Instant result = ExtensionMapper.extractActivitySatisfiedUntil(List.of(extension));
        assertEquals(Instant.parse("2021-12-07T10:11:12.124Z"), result);
    }

    @Test
    public void extractExaminationStatus_success() {
        Extension extension = new Extension(Systems.EXAMINATION_STATUS, new StringType(ExaminationStatus.EXAMINED.toString()));
        ExaminationStatus result = ExtensionMapper.extractExaminationStatus(List.of(extension));
        assertEquals(ExaminationStatus.EXAMINED, result);
    }

    @Test
    public void extractOrganizationId_success() {
        Extension extension = new Extension(Systems.ORGANIZATION, new Reference("Organization/organization-1"));
        String result = ExtensionMapper.extractOrganizationId(List.of(extension));
        assertEquals("Organization/organization-1", result);
    }

    @Test
    public void extractOrganizationDeadlineTimeDefault_success() {
        TimeType time = new TimeType("11:00");
        Extension extension = new Extension(Systems.ORGANIZATION_QUESTIONNAIRE_DEADLINE_TIME_DEFAULT, time);
        TimeType result = ExtensionMapper.extractOrganizationDeadlineTimeDefault(List.of(extension));
        assertEquals(time, result);
    }

    @Test
    public void tryExtractOrganizationId_idPresent_success() {
        Extension extension = new Extension(Systems.ORGANIZATION, new Reference("Organization/organization-1"));
        Optional<String> result = ExtensionMapper.tryExtractOrganizationId(List.of(extension));
        assertTrue(result.isPresent());
        assertEquals("Organization/organization-1", result.get());
    }

    @Test
    public void tryExtractOrganizationId_idMissing_success() {
        Optional<String> result = ExtensionMapper.tryExtractOrganizationId(List.of());
        assertFalse(result.isPresent());
    }

    @Test
    public void mapThreshold_boolean() {
        ThresholdModel threshold = new ThresholdModel(
                "foo",
                ThresholdType.NORMAL,
                null,
                null,
                true,
                null
        );

        Extension result = ExtensionMapper.mapThreshold(threshold);

        assertEquals("foo", result.getExtensionString(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID));
        assertEquals(ThresholdType.NORMAL.toString(), result.getExtensionString(Systems.THRESHOLD_TYPE));
        assertTrue(((BooleanType) result.getExtensionByUrl(Systems.THRESHOLD_VALUE_BOOLEAN).getValue()).booleanValue());
    }

    @Test
    public void mapThreshold_range() {
        ThresholdModel threshold = new ThresholdModel(
                "bar",
                ThresholdType.ABNORMAL,
                2.0,
                4.0,
                null,
                null
        );

        Extension result = ExtensionMapper.mapThreshold(threshold);

        assertEquals("bar", result.getExtensionString(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID));
        assertEquals(ThresholdType.ABNORMAL.toString(), result.getExtensionString(Systems.THRESHOLD_TYPE));
        assertEquals(2.0, ((Range) result.getExtensionByUrl(Systems.THRESHOLD_VALUE_RANGE).getValue()).getLow().getValue().doubleValue());
        assertEquals(4.0, ((Range) result.getExtensionByUrl(Systems.THRESHOLD_VALUE_RANGE).getValue()).getHigh().getValue().doubleValue());
    }

    @Test
    public void extractThreshold_boolean() {
        Extension extension = new Extension();
        extension.addExtension(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID, new StringType("foo"));
        extension.addExtension(Systems.THRESHOLD_TYPE, new StringType(ThresholdType.NORMAL.toString()));
        extension.addExtension(Systems.THRESHOLD_VALUE_BOOLEAN, new BooleanType(true));

        ThresholdModel result = ExtensionMapper.extractThreshold(extension);

        assertEquals("foo", result.questionnaireItemLinkId());
        assertEquals(ThresholdType.NORMAL, result.type());
        assertTrue(result.valueBoolean());
    }

    @Test
    public void extractThreshold_range() {
        Extension extension = new Extension();
        extension.addExtension(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID, new StringType("bar"));
        extension.addExtension(Systems.THRESHOLD_TYPE, new StringType(ThresholdType.ABNORMAL.toString()));
        Range r = new Range();
        r.setLow(new Quantity(2.0));
        r.setHigh(new Quantity(4.0));
        extension.addExtension(Systems.THRESHOLD_VALUE_RANGE, r);

        ThresholdModel result = ExtensionMapper.extractThreshold(extension);

        assertEquals("bar", result.questionnaireItemLinkId());
        assertEquals(ThresholdType.ABNORMAL, result.type());
        assertEquals(2.0, result.valueQuantityLow());
        assertEquals(4.0, result.valueQuantityHigh());
    }
}