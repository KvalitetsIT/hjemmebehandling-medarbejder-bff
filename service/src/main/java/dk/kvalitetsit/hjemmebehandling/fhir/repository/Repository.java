package dk.kvalitetsit.hjemmebehandling.fhir.repository;

import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.List;
import java.util.Optional;

public interface Repository<R> {

    // TODO: Should return the the updated resource if successful
    void update(R resource);

    // TODO: Should return the the saved resource if successful
    String save(R resource) throws ServiceException;

    Optional<R> fetch (QualifiedId id) throws ServiceException;

    List<R> fetch (List<QualifiedId> id) throws ServiceException;


    List<R> fetch () throws ServiceException;


}
