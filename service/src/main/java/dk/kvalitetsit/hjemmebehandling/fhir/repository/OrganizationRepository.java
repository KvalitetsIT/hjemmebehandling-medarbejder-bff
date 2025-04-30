package dk.kvalitetsit.hjemmebehandling.fhir.repository;

import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.Optional;

public interface OrganizationRepository<Organization> extends Repository<Organization, QualifiedId.OrganizationId> {

    /**
     * Looks up an organization by its SOR code.
     *
     * @param sorCode The SOR code.
     * @return An optional organization.
     * @throws ServiceException If the operation fails.
     */
    Optional<Organization> lookupOrganizationBySorCode(String sorCode) throws ServiceException;


    /**
     * Retrieves the organization ID for the current context.
     *
     * @return The organization ID.
     * @throws ServiceException If the operation fails.
     */
    QualifiedId.OrganizationId getOrganizationId() throws ServiceException;

    /**
     * Gets the organization of the currently authenticated user.
     *
     * @return The user's organization.
     * @throws ServiceException If the operation fails.
     */
    Organization getCurrentUsersOrganization() throws ServiceException;

}
