package dk.kvalitetsit.hjemmebehandling.model;

import org.hl7.fhir.r4.model.TimeType;

import java.util.Optional;

public record OrganizationModel(
        QualifiedId.OrganizationId id,
        String name,
        TimeType defaultDeadlineTime
) implements BaseModel<OrganizationModel> {

    @Override
    public QualifiedId.OrganizationId organizationId() {
        return id;
    }

    @Override
    public OrganizationModel substitute(OrganizationModel other) {
        return new OrganizationModel(
                Optional.of(other.id).orElse(id),
                Optional.of(other.name).orElse(name),
                Optional.of(other.defaultDeadlineTime).orElse(defaultDeadlineTime)
        );
    }


}
