package dk.kvalitetsit.hjemmebehandling.fhir;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import dk.kvalitetsit.hjemmebehandling.model.PractitionerModel;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Range;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TimeType;
import org.hl7.fhir.r4.model.Type;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.TriagingCategory;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;

public class ExtensionMapper {
    protected static final LocalDateTime MAX_SATISFIED_UNTIL_DATE = LocalDateTime.of(9999, Month.DECEMBER, 31, 11, 00);

    public static Extension mapActivitySatisfiedUntil(Instant pointInTime) {
        return buildDateTimeExtension(Systems.ACTIVITY_SATISFIED_UNTIL, pointInTime);
    }

    public static Extension mapCarePlanSatisfiedUntil(Instant pointInTime) {
        return buildDateTimeExtension(Systems.CAREPLAN_SATISFIED_UNTIL, pointInTime);
    }

    public static Extension mapCustomUserId(String customUserId) {
        return buildStringExtension(Systems.CUSTOM_USER_ID, customUserId);
    }
    
	public static Extension mapCustomUserName(String customUserName) {
        return buildStringExtension(Systems.CUSTOM_USER_NAME, customUserName);
	}
    
    public static Extension mapExaminationStatus(ExaminationStatus examinationStatus) {
        return buildStringExtension(Systems.EXAMINATION_STATUS, examinationStatus.toString());
    }

    public static Extension mapExaminationAuthor(PractitionerModel practitioner) {
        return buildStringExtension(Systems.EXAMINATION_AUTHOR, practitioner.getId().toString());
    }

    public static Extension mapOrganizationId(String organizationId) {
        return buildReferenceExtension(Systems.ORGANIZATION, organizationId);
    }

    public static Extension mapOrganizationDeadlineTimeDefault(TimeType time) {
        return new Extension(Systems.ORGANIZATION_QUESTIONNAIRE_DEADLINE_TIME_DEFAULT, time);
    }

    public static List<Extension> mapThresholds(List<ThresholdModel> thresholds) {
        return thresholds.stream().map(ExtensionMapper::mapThreshold).collect(Collectors.toList());
    }

    public static Extension mapThreshold(ThresholdModel threshold) {
        Extension linkIdExtension = buildStringExtension(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID, threshold.getQuestionnaireItemLinkId());
        Extension thresholdTypeExtension = buildStringExtension(Systems.THRESHOLD_TYPE, threshold.getType().toString());
        Extension thresholdValueExtension = null;
        if(threshold.getValueBoolean() != null) {
            thresholdValueExtension = buildBooleanExtension(Systems.THRESHOLD_VALUE_BOOLEAN, threshold.getValueBoolean());
        }
        else if(threshold.getValueOption() != null) {
            thresholdValueExtension = buildStringExtension(Systems.THRESHOLD_VALUE_OPTION, threshold.getValueOption());
        }
        else if(threshold.getValueQuantityLow() != null || threshold.getValueQuantityHigh() != null) {
            thresholdValueExtension = buildRangeExtension(Systems.THRESHOLD_VALUE_RANGE, threshold.getValueQuantityLow(), threshold.getValueQuantityHigh());
        }
        if(thresholdValueExtension != null)  return buildCompositeExtension(Systems.THRESHOLD, List.of(linkIdExtension, thresholdTypeExtension, thresholdValueExtension));
        return buildCompositeExtension(Systems.THRESHOLD, List.of(linkIdExtension, thresholdTypeExtension));
    }

    public static Extension mapAnswerOptionComment(String comment) {
        return buildStringExtension(Systems.ANSWER_OPTION_COMMENT, comment);
    }

    public static String extractAnswerOptionComment(List<Extension> extensions) {
        return extractStringFromExtensions(extensions, Systems.ANSWER_OPTION_COMMENT);
    }

    public static Extension mapTriagingCategory(TriagingCategory triagingCategory) {
        return buildStringExtension(Systems.TRIAGING_CATEGORY, triagingCategory.toString());
    }

    public static Instant extractActivitySatisfiedUntil(List<Extension> extensions) {
        return extractInstantFromExtensions(extensions, Systems.ACTIVITY_SATISFIED_UNTIL);
    }

    public static Instant extractCarePlanSatisfiedUntil(List<Extension> extensions) {
        return extractInstantFromExtensions(extensions, Systems.CAREPLAN_SATISFIED_UNTIL);
    }

    public static ExaminationStatus extractExaminationStatus(List<Extension> extensions) {
        return extractEnumFromExtensions(extensions, Systems.EXAMINATION_STATUS, ExaminationStatus.class);
    }

    public static String tryExtractExaminationAuthorPractitionerId(List<Extension> extensions) {
        return extractStringFromExtensions(extensions, Systems.EXAMINATION_AUTHOR);
    }

    public static String extractOrganizationId(List<Extension> extensions) {
        return extractReferenceFromExtensions(extensions, Systems.ORGANIZATION);
    }

    public static TimeType extractOrganizationDeadlineTimeDefault(List<Extension> extensions) {
        return extractFromExtensions(extensions, Systems.ORGANIZATION_QUESTIONNAIRE_DEADLINE_TIME_DEFAULT, v -> ((TimeType) v));
    }
    
    public static String extractCustomUserId(List<Extension> extensions) {
        return extractStringFromExtensions(extensions, Systems.CUSTOM_USER_ID);
    }
    
    public static String extractCustomUserName(List<Extension> extensions) {
        return extractStringFromExtensions(extensions, Systems.CUSTOM_USER_NAME);
    }

    public static Optional<String> tryExtractOrganizationId(List<Extension> extensions) {
        return tryExtractReferenceFromExtensions(extensions, Systems.ORGANIZATION);
    }

    public static List<ThresholdModel> extractThresholds(List<Extension> extensions) {
        return extensions.stream().map(ExtensionMapper::extractThreshold).collect(Collectors.toList());
    }

    public static ThresholdModel extractThreshold(Extension extension) {
        ThresholdModel thresholdModel = new ThresholdModel();

        thresholdModel.setQuestionnaireItemLinkId( extension.getExtensionString(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID) );
        thresholdModel.setType( Enum.valueOf(ThresholdType.class, extension.getExtensionString(Systems.THRESHOLD_TYPE)) );
        if ( extension.hasExtension(Systems.THRESHOLD_VALUE_BOOLEAN) ) {
            BooleanType valueBoolean = (BooleanType) extension.getExtensionByUrl(Systems.THRESHOLD_VALUE_BOOLEAN).getValue();
            thresholdModel.setValueBoolean( valueBoolean.booleanValue() );
        }

        if ( extension.hasExtension(Systems.THRESHOLD_VALUE_OPTION) ) {
            StringType valueOption = (StringType) extension.getExtensionByUrl(Systems.THRESHOLD_VALUE_OPTION).getValue();
            thresholdModel.setValueOption( valueOption.getValue() );
        }



        if ( extension.hasExtension(Systems.THRESHOLD_VALUE_RANGE) ) {
            Range valueRange = (Range) extension.getExtensionByUrl(Systems.THRESHOLD_VALUE_RANGE).getValue();
            if (valueRange.hasLow()) {
                thresholdModel.setValueQuantityLow( valueRange.getLow().getValue().doubleValue() );
            }
            if (valueRange.hasHigh()) {
                thresholdModel.setValueQuantityHigh( valueRange.getHigh().getValue().doubleValue() );
            }
        }

        return thresholdModel;
    }

    public static TriagingCategory extractTriagingCategoory(List<Extension> extensions) {
        return extractEnumFromExtensions(extensions, Systems.TRIAGING_CATEGORY, TriagingCategory.class);
    }

    public static Extension mapQuestionAbbreviation(String abbreviation) {
        return buildStringExtension(Systems.QUESTION_ABBREVIATION, abbreviation);
    }

    public static String extractQuestionAbbreviation(List<Extension> extensions) {
        return extractStringFromExtensions(extensions, Systems.QUESTION_ABBREVIATION);
    }

    private static Extension buildBooleanExtension(String url, boolean value) {
        return new Extension(url, new BooleanType(value));
    }

    private static Extension buildCompositeExtension(String url, List<Extension> extensions) {
        Extension extension = new Extension(url);

        for(Extension e : extensions) {
            extension.addExtension(e);
        }

        return extension;
    }

    private static Extension buildDateTimeExtension(String url, Instant value) {
        // the need for this is so stupid..
        if (value.equals(Instant.MAX)) {
            // men hapi-fhir clientens json parser af dato har fixed dato-format med 4 digits år, så den knækker hvis man sender år 'en million millard'
            value = MAX_SATISFIED_UNTIL_DATE.toInstant(ZoneId.of("Europe/Copenhagen").getRules().getOffset(Instant.now()));
        }
        return new Extension(url, new DateTimeType(Date.from(value), TemporalPrecisionEnum.MILLI, TimeZone.getTimeZone("UTC")));
    }

    private static Extension buildRangeExtension(String url, Double low, Double high) {
        Range range = new Range();

        if(low != null) {
            range.setLow(new Quantity(low));
        }
        if(high != null) {
            range.setHigh(new Quantity(high));
        }

        return new Extension(url, range);
    }

    private static Extension buildReferenceExtension(String url, String value) {
        return new Extension(url, new Reference(value));
    }

    private static Extension buildStringExtension(String url, String value) {
        return new Extension(url, new StringType(value));
    }

    private static <T extends Enum<T>> T extractEnumFromExtensions(List<Extension> extensions, String url, Class<T> type) {
        return extractFromExtensions(extensions, url, v -> Enum.valueOf(type, v.toString()));
    }

    private static Instant extractInstantFromExtensions(List<Extension> extensions, String url) {
        return extractFromExtensions(extensions, url, v -> {
            Instant result = ((DateTimeType) v).getValue().toInstant();

            if (result.equals(MAX_SATISFIED_UNTIL_DATE.toInstant(ZoneId.of("Europe/Copenhagen").getRules().getOffset(Instant.now())))) {
                return Instant.MAX;
            }
            return result;
        });
    }
    
    private static String extractStringFromExtensions(List<Extension> extensions, String url) {
        return extractFromOptionalExtensions(extensions, url, v -> ((StringType) v).getValue());
    }

    private static String extractReferenceFromExtensions(List<Extension> extensions, String url) {
        return extractFromExtensions(extensions, url, v -> ((Reference) v).getReference());
    }

    private static Optional<String> tryExtractReferenceFromExtensions(List<Extension> extensions, String url) {
        return tryExtractFromExtensions(extensions, url, v -> ((Reference) v).getReference());
    }

    private static <T> T extractFromExtensions(List<Extension> extensions, String url, Function<Type, T> extractor) {
        return tryExtractFromExtensions(extensions, url, extractor)
                .orElseThrow(() -> new IllegalStateException(String.format("Could not look up url %s among the candidate extensions!", url)));
    }
    
    private static <T> T extractFromOptionalExtensions(List<Extension> extensions, String url, Function<Type, T> extractor) {
        return tryExtractFromExtensions(extensions, url, extractor).orElse(null);
    }

    private static <T> Optional<T> tryExtractFromExtensions(List<Extension> extensions, String url, Function<Type, T> extractor) {
        for(Extension extension : extensions) {
            if(extension.getUrl().equals(url)) {
                return Optional.of(extractor.apply(extension.getValue()));
            }
        }
        return Optional.empty();
    }

}
