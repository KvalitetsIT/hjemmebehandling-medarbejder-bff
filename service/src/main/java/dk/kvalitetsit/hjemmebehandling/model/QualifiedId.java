package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import org.hl7.fhir.r4.model.ResourceType;

import java.util.Objects;

public class QualifiedId {
    private String id;
    private ResourceType qualifier;

    public QualifiedId(String id, ResourceType qualifier) {
        if (!FhirUtils.isPlainId(id)) {
            throw new IllegalArgumentException(String.format("Provided id was not a plain id: %s!", id));
        }

        this.id = id;
        this.qualifier = qualifier;
    }

    public QualifiedId(String qualifiedId) {
        var parts = qualifiedId.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException(String.format("Cannot unqualify id: %s! Illegal format", id));
        }
        ResourceType qualifier = Enum.valueOf(ResourceType.class, parts[0]);
        if (!FhirUtils.isPlainId(parts[1])) {
            throw new IllegalArgumentException(String.format("Cannot unqualify id: %s! Illegal id", id));
        }
        String id = parts[1];

        this.id = id;
        this.qualifier = qualifier;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ResourceType getQualifier() {
        return qualifier;
    }

    public void setQualifier(ResourceType qualifier) {
        this.qualifier = qualifier;
    }

    public String toString() {
        return String.format("%s/%s", qualifier.toString(), id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QualifiedId that = (QualifiedId) o;
        return Objects.equals(id, that.id) && qualifier == that.qualifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, qualifier);
    }
}
