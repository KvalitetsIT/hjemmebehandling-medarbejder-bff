package dk.kvalitetsit.hjemmebehandling.model;

public interface BaseModel<T extends BaseModel<T>> {
    QualifiedId id();
    QualifiedId.OrganizationId organizationId();

    /**
     * This method is supposed to overwrite every field with the corresponding field of the other if this is not null
     * The method is in other words doing a "PUT" operation
     * @param other the instance which is substituted from
     * @return T
     */
    T substitute(T other);
}
