package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.DomainResource;

import java.util.List;

public abstract class AccessValidatingService {
    private final AccessValidator accessValidator;

    public AccessValidatingService(AccessValidator accessValidator) {
        this.accessValidator = accessValidator;
    }

    protected void validateAccess(DomainResource resource) throws AccessValidationException, ServiceException {
        accessValidator.validateAccess(resource);
    }

    protected void validateAccess(List<? extends DomainResource> resources) throws AccessValidationException, ServiceException {
        accessValidator.validateAccess(resources);
    }
}
