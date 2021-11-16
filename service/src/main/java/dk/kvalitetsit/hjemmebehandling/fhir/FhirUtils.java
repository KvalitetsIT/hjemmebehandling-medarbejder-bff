package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.ResourceType;

import java.util.regex.Pattern;

public class FhirUtils {
    public static String unqualifyId(String id) {
        if(isPlainId(id)) {
            return id;
        }
        var parts = id.split("/");
        if(parts.length != 2) {
            throw new IllegalArgumentException(String.format("Cannot unqualify id: %s! Illegal format", id));
        }
        ResourceType qualifier = Enum.valueOf(ResourceType.class, parts[0]);
        if(!isPlainId(parts[1])) {
            throw new IllegalArgumentException(String.format("Cannot unqualify id: %s! Illegal id", id));
        }
        return parts[1];
    }

    public static String qualifyId(String id, ResourceType qualifier) {
        if(idQualifiedByQualifier(id, qualifier)) {
            return id;
        }
        if(!isPlainId(id)) {
            throw new IllegalArgumentException(String.format("Cannot qualify id: %s", id));
        }
        return qualifier + "/" + id;
    }

    private static boolean isPlainId(String id) {
        Pattern p = Pattern.compile("^[a-z0-9\\-]+$");
        return p.matcher(id).matches();
    }

    private static boolean idQualifiedByQualifier(String id, ResourceType qualifier) {
        String prefix = qualifier.toString() + "/";
        return id.startsWith(prefix) && isPlainId(id.substring(prefix.length()));
    }
}
