package dk.kvalitetsit.hjemmebehandling.repository;

import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
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
    Optional<Organization> lookupOrganizationBySorCode(QualifiedId.OrganizationId sorCode) throws ServiceException;


    /**
     * Retrieves the organization ID from the current context.
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
    Organization fetchCurrentUsersOrganization() throws ServiceException, AccessValidationException;

}
