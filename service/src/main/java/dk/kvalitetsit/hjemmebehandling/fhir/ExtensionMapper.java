package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.TriagingCategory;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;

import java.util.List;

public class ExtensionMapper {
    public static Extension mapExaminationStatus(ExaminationStatus examinationStatus) {
        return buildExtension(Systems.EXAMINATION_STATUS, examinationStatus.toString());
    }

    public static Extension mapTriagingCategory(TriagingCategory triagingCategory) {
        return buildExtension(Systems.TRIAGING_CATEGORY, triagingCategory.toString());
    }

    public static ExaminationStatus extractExaminationStatus(List<Extension> extensions) {
        return extractEnumFromExtensions(extensions, Systems.EXAMINATION_STATUS, ExaminationStatus.class);
    }

    public static TriagingCategory extractTriagingCategoory(List<Extension> extensions) {
        return extractEnumFromExtensions(extensions, Systems.TRIAGING_CATEGORY, TriagingCategory.class);
    }

    private static Extension buildExtension(String url, String value) {
        return new Extension(url, new StringType(value));
    }

    private static <T extends Enum<T>> T extractEnumFromExtensions(List<Extension> extensions, String url, Class<T> type) {
        for(Extension extension : extensions) {
            if(extension.getUrl().equals(url)) {
                return Enum.valueOf(type, extension.getValue().toString());
            }
        }
        throw new IllegalStateException(String.format("Could not look up url %s among the candidate extensions!", url));
    }
}
