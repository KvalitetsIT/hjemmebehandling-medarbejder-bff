package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.TriagingCategory;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdSet;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ExtensionMapper {
    public static Extension mapActivitySatisfiedUntil(Instant pointInTime) {
        return buildExtension(Systems.ACTIVITY_SATISFIED_UNTIL, pointInTime.toString());
    }

    public static Extension mapCarePlanSatisfiedUntil(Instant pointInTime) {
        return buildExtension(Systems.CAREPLAN_SATISFIED_UNTIL, pointInTime.toString());
    }

    public static Extension mapExaminationStatus(ExaminationStatus examinationStatus) {
        return buildExtension(Systems.EXAMINATION_STATUS, examinationStatus.toString());
    }

    public static Extension mapTriagingCategory(TriagingCategory triagingCategory) {
        return buildExtension(Systems.TRIAGING_CATEGORY, triagingCategory.toString());
    }

    public static Extension mapTresholdSet(ThresholdSet thresholdSet) {
        Extension result = buildExtension(Systems.THRESHOLDSET, "");
        result.addExtension( buildExtension(Systems.THRESHOLDSET_QUESTIONNAIRE_ID, thresholdSet.getQuestionnaireId()) );
        for (ThresholdSet.Threshold threshold : thresholdSet.getThresholdList()) {
            Extension thresholdItem = buildExtension(Systems.THRESHOLD, "");
            thresholdItem.addExtension( buildExtension(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID, threshold.getQuestionnaireItemLinkId()) );
            thresholdItem.addExtension( buildExtension(Systems.THRESHOLD_TYPE, threshold.getType()) );
            thresholdItem.addExtension( buildExtension(Systems.THRESHOLD_OPERATOR, threshold.getOperator()) );
            if (threshold.getValueBoolean() != null) {
                thresholdItem.addExtension(buildExtension(Systems.THRESHOLD_VALUE_BOOLEAN, String.valueOf(threshold.getValueBoolean())) );
            }
            else if (threshold.getValueQuantity() != null) {
                thresholdItem.addExtension(buildExtension(Systems.THRESHOLD_VALUE_BOOLEAN, String.valueOf(threshold.getValueBoolean())) );
            }

            result.addExtension(thresholdItem);
        }
        return result;
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

    public static TriagingCategory extractTriagingCategoory(List<Extension> extensions) {
        return extractEnumFromExtensions(extensions, Systems.TRIAGING_CATEGORY, TriagingCategory.class);
    }

    public static ThresholdSet extractTresholdSet(Extension thresholdSetExtension) {
        if (thresholdSetExtension == null) {
            return null;
        }
        ThresholdSet result = new ThresholdSet();
        result.setQuestionnaireId( thresholdSetExtension.getExtensionString(Systems.THRESHOLDSET_QUESTIONNAIRE_ID) );

        for (Extension ext : thresholdSetExtension.getExtensionsByUrl(Systems.THRESHOLD)) {
            ThresholdSet.Threshold threshold = new ThresholdSet.Threshold();

            threshold.setQuestionnaireItemLinkId( ext.getExtensionString(Systems.THRESHOLD_QUESTIONNAIRE_ITEM_LINKID) );
            threshold.setType( ext.getExtensionString(Systems.THRESHOLD_TYPE) );
            threshold.setOperator( ext.getExtensionString(Systems.THRESHOLD_OPERATOR) );
            if ( ext.hasExtension(Systems.THRESHOLD_VALUE_BOOLEAN) ) {
                String valueBoolean = ext.getExtensionString(Systems.THRESHOLD_VALUE_BOOLEAN);
                threshold.setValueBoolean( Boolean.valueOf(valueBoolean) );
            }
            else if ( ext.hasExtension(Systems.THRESHOLD_VALUE_QUANTITY) ) {
                String valueQuantity = ext.getExtensionString(Systems.THRESHOLD_VALUE_QUANTITY);
                threshold.setValueQuantity( Double.valueOf(valueQuantity) );
            }
            result.getThresholdList().add(threshold);
        }
        return result;
    }

    private static Extension buildExtension(String url, String value) {
        return new Extension(url, new StringType(value));
    }

    private static <T extends Enum<T>> T extractEnumFromExtensions(List<Extension> extensions, String url, Class<T> type) {
        return extractFromExtensions(extensions, url, v -> Enum.valueOf(type, v.toString()));
    }

    private static Instant extractInstantFromExtensions(List<Extension> extensions, String url) {
        return extractFromExtensions(extensions, url, v -> Instant.parse(v.primitiveValue()));
    }

    private static <T> T extractFromExtensions(List<Extension> extensions, String url, Function<Type, T> extractor) {
        for(Extension extension : extensions) {
            if(extension.getUrl().equals(url)) {
                return extractor.apply(extension.getValue());
            }
        }
        throw new IllegalStateException(String.format("Could not look up url %s among the candidate extensions!", url));
    }
}
