package dk.kvalitetsit.hjemmebehandling.fhir.client;

import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.List;
import java.util.Optional;

public interface CRUD<R> {

    // TODO: Should return the the updated resource if successful
    void update(R resource);

    // TODO: Should return the the saved resource if successful
    String save(R resource) throws ServiceException;

    Optional<R> fetch (String id) throws ServiceException;

    List<R> fetch (String... id);

    List<R> fetch ();


}
