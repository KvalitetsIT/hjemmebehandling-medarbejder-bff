package dk.kvalitetsit.hjemmebehandling.model;

public abstract class BaseModel {
    private QualifiedId id;
    private String organizationId;

    public QualifiedId getId() {
        return id;
    }

    public void setId(QualifiedId id) {
        this.id = id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
}
