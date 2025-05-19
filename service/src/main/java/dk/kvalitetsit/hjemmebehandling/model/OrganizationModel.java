package dk.kvalitetsit.hjemmebehandling.model;

import org.hl7.fhir.r4.model.TimeType;

public record OrganizationModel(QualifiedId.OrganizationId id, String name, TimeType defaultDeadlineTime) implements BaseModel {

    @Override
    public QualifiedId.OrganizationId organizationId() {
        return id;
    }


}
