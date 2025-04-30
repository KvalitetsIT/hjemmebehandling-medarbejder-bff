package dk.kvalitetsit.hjemmebehandling.fhir.repository;

import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

public interface PractitionerRepository<Practitioner> extends Repository<Practitioner, QualifiedId.PractitionerId> {

    /**
     * Retrieves the current user as a practitioner, creating one if not existing.
     *
     * @return The practitioner instance.
     * @throws ServiceException If the operation fails.
     */
    Practitioner getOrCreateUserAsPractitioner() throws ServiceException;

}
