package dk.kvalitetsit.hjemmebehandling.repository;

import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.List;
import java.util.Optional;

/**
 * A generic repository interface for managing resources.
 *
 * @param <R>  the type of resource managed by the repository
 * @param <ID> the type of identifier for the resource, must extend {@link QualifiedId}
 */
public interface Repository<R, ID extends QualifiedId> {

    /**
     * Updates the given resource in the repository.
     * <p>
     * Implementations should update the resource if it exists, and may throw an exception if not.
     *
     * @param resource the resource to update
     * @throws ServiceException if the update fails due to a service-level issue
     */
    void update(R resource) throws ServiceException, AccessValidationException;

    /**
     * Saves the given resource to the repository.
     * <p>
     * If the resource is new, it will be created; otherwise, it may be updated depending on implementation.
     *
     * @param resource the resource to save
     * @return the identifier of the saved resource
     * @throws ServiceException if the save operation fails
     */
    ID save(R resource) throws ServiceException;

    /**
     * Fetches a resource by its identifier.
     *
     * @param id the identifier of the resource to fetch
     * @return an {@code Optional} containing the resource if found, or empty if not
     * @throws ServiceException if the fetch operation fails
     */
    Optional<R> fetch(ID id) throws ServiceException, AccessValidationException;

    /**
     * Fetches multiple resources by their identifiers.
     *
     * @param id a list of resource identifiers to fetch
     * @return a list of resources that match the given identifiers
     * @throws ServiceException if the fetch operation fails
     */
    List<R> fetch(List<ID> id) throws ServiceException, AccessValidationException;

    /**
     * Fetches all resources from the repository.
     *
     * @return a list of all resources
     * @throws ServiceException if the fetch operation fails
     */
    List<R> fetch() throws ServiceException, AccessValidationException;

    /**
     * Fetches historical entries of a certain resource.
     *
     * @return a list historical entries
     * @throws ServiceException if the fetch operation fails
     */
    List<R> history(ID id) throws ServiceException, AccessValidationException;

    /**
     * Fetches historical entries of multiple resources.
     *
     * @return a list of historical entries
     * @throws ServiceException if the fetch operation fails
     */
    List<R> history(List<ID> ids) throws ServiceException, AccessValidationException;



}
