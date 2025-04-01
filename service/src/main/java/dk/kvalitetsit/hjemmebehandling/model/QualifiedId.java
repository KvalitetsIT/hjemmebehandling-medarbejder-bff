package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import org.hl7.fhir.r4.model.ResourceType;

import java.util.Objects;
public record QualifiedId(String id, ResourceType qualifier) {

    public QualifiedId {
        if (!FhirUtils.isPlainId(id)) {
            throw new IllegalArgumentException("Provided id was not a plain id: " + id);
        }
    }

    public QualifiedId(String qualifiedId) {
        this(parseId(qualifiedId), parseQualifier(qualifiedId));
    }

    private static String parseId(String qualifiedId) {
        var parts = qualifiedId.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Cannot unqualify id: " + qualifiedId + "! Illegal format");
        }
        if (!FhirUtils.isPlainId(parts[1])) {
            throw new IllegalArgumentException("Cannot unqualify id: " + qualifiedId + "! Illegal id");
        }
        return parts[1];
    }

    private static ResourceType parseQualifier(String qualifiedId) {
        var parts = qualifiedId.split("/");
        return Enum.valueOf(ResourceType.class, parts[0]);
    }

    @Override
    public String toString() {
        return qualifier + "/" + id;
    }
}
