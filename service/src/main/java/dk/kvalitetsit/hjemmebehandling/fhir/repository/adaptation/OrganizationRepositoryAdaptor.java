package dk.kvalitetsit.hjemmebehandling.fhir.repository.adaptation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.repository.OrganizationRepository;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Organization;

import java.util.List;
import java.util.Optional;

/**
 * An adapter whose responsibility is to adapt between FHIR and the domain logic.
 * This primarily covers mapping from business models and calling further into the stack with the expected arguments
 * For now, it implements the OrganizationRepository interface, but this might change in the future
 */
public class OrganizationRepositoryAdaptor implements OrganizationRepository<Organization> {

    private final OrganizationRepository<Organization> repository;
    private final FhirMapper mapper;

    public OrganizationRepositoryAdaptor(OrganizationRepository<Organization> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Organization> lookupOrganizationBySorCode(String sorCode) throws ServiceException {
        return this.repository.lookupOrganizationBySorCode(sorCode);
    }

    @Override
    public QualifiedId.OrganizationId getOrganizationId() throws ServiceException {
        return repository.getOrganizationId();
    }

    @Override
    public Organization getCurrentUsersOrganization() throws ServiceException {
        return repository.getCurrentUsersOrganization();
    }

    @Override
    public void update(Organization resource) throws ServiceException {
        repository.update(resource);
    }

    @Override
    public QualifiedId.OrganizationId save(Organization resource) throws ServiceException {
        return repository.save(resource);
    }

    @Override
    public Optional<Organization> fetch(QualifiedId.OrganizationId id) throws ServiceException {
        return repository.fetch(id);
    }

    @Override
    public List<Organization> fetch(List<QualifiedId.OrganizationId> ids) throws ServiceException {
        return repository.fetch(ids);
    }

    @Override
    public List<Organization> fetch() throws ServiceException {
        return repository.fetch();
    }
}
