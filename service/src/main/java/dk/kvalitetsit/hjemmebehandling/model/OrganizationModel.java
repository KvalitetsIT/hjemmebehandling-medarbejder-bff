package dk.kvalitetsit.hjemmebehandling.model;

public record OrganizationModel(QualifiedId.OrganizationId id, String name) implements BaseModel {

    @Override
    public QualifiedId.OrganizationId organizationId() {
        return id;
    }
}
