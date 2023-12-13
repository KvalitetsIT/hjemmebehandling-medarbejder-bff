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

    // Proposal: forces the implementer to include the mapping
    // Same could be done for the BaseDto - public abstract T toDto() etc.
    // TODO: public abstract T toDto();


}
