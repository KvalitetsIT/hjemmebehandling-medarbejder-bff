package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import org.hl7.fhir.r4.model.DomainResource;

public abstract class AccessValidatingService {
    private AccessValidator accessValidator;

    public AccessValidatingService(AccessValidator accessValidator) {
        this.accessValidator = accessValidator;
    }

    protected void validateAccess(DomainResource resource) throws AccessValidationException {
        accessValidator.validateAccess(resource);
    }
}
