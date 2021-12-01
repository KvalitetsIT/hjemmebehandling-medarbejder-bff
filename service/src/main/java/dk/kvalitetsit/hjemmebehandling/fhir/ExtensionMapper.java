package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.TriagingCategory;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

import java.sql.Date;
import java.time.Instant;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Function;

public class ExtensionMapper {
    public static Extension mapActivitySatisfiedUntil(Instant pointInTime) {
        return buildDateTimeExtension(Systems.ACTIVITY_SATISFIED_UNTIL, pointInTime);
    }

    public static Extension mapCarePlanSatisfiedUntil(Instant pointInTime) {
        return buildDateTimeExtension(Systems.CAREPLAN_SATISFIED_UNTIL, pointInTime);
    }

    public static Extension mapExaminationStatus(ExaminationStatus examinationStatus) {
        return buildStringExtension(Systems.EXAMINATION_STATUS, examinationStatus.toString());
    }

    public static Extension mapOrganizationId(String organizationId) {
        return buildReferenceExtension(Systems.ORGANIZATION, organizationId);
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

    public static String extractOrganizationId(List<Extension> extensions) {
        return extractReferenceFromExtensions(extensions, Systems.ORGANIZATION);
    }

    public static TriagingCategory extractTriagingCategoory(List<Extension> extensions) {
        return extractEnumFromExtensions(extensions, Systems.TRIAGING_CATEGORY, TriagingCategory.class);
    }

    private static Extension buildDateTimeExtension(String url, Instant value) {
        return new Extension(url, new DateTimeType(Date.from(value), TemporalPrecisionEnum.MILLI, TimeZone.getTimeZone("UTC")));
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
        return extractFromExtensions(extensions, url, v -> ((DateTimeType) v).getValue().toInstant());
    }

    private static String extractReferenceFromExtensions(List<Extension> extensions, String url) {
        return extractFromExtensions(extensions, url, v -> ((Reference) v).getReference());
    }

    private static <T> T extractFromExtensions(List<Extension> extensions, String url, Function<Type, T> extractor) {
        for(Extension extension : extensions) {
            if(extension.getUrl().equals(url)) {
                return extractor.apply(extension.getValue());
            }
        }
        throw new IllegalStateException(String.format("Could not look up url %s among the candidate extensions!", url));
    }

    public static List<ThresholdModel> extractThresholds(List<Extension> extensions) {
        List<ThresholdModel> result = new ArrayList<>();

        for (Extension thresholdExtension : extensions) {
            ThresholdModel thresholdModel = new ThresholdModel();
            thresholdModel.setQuestionnaireItemLinkId( thresholdExtension.getExtensionString(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID) );
            thresholdModel.setType( Enum.valueOf(ThresholdType.class, thresholdExtension.getExtensionString(Systems.THRESHOLD_TYPE)) );
            if ( thresholdExtension.hasExtension(Systems.THRESHOLD_VALUE_BOOLEAN) ) {
                BooleanType valueBoolean = (BooleanType) thresholdExtension.getExtensionByUrl(Systems.THRESHOLD_VALUE_BOOLEAN).getValue();
                thresholdModel.setValueBoolean( valueBoolean.booleanValue() );
            }
            if ( thresholdExtension.hasExtension(Systems.THRESHOLD_VALUE_RANGE) ) {
                Range valueRange = (Range) thresholdExtension.getExtensionByUrl(Systems.THRESHOLD_VALUE_RANGE).getValue();
                if (valueRange.hasLow()) {
                    thresholdModel.setValueQuantityLow( valueRange.getLow().getValue().doubleValue() );
                }
                if (valueRange.hasHigh()) {
                    thresholdModel.setValueQuantityHigh( valueRange.getHigh().getValue().doubleValue() );
                }
            }
            result.add(thresholdModel);
        }
        return result;
    }
}
