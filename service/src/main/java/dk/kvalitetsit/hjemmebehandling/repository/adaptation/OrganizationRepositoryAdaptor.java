package dk.kvalitetsit.hjemmebehandling.repository.adaptation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.repository.OrganizationRepository;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Organization;

import java.util.List;
import java.util.Optional;

/**
 * Adapter responsible for translating between FHIR resources and domain-specific logic.
 * <p>
 * This class primarily handles the mapping of business models to domain representations
 * and delegates calls deeper into the application stack with the appropriate arguments.
 * <p>
 * Currently, it implements the {@link OrganizationRepository} interface for {@link Organization} entities.
 * Note that this implementation detail may change in the future.
 */
public class OrganizationRepositoryAdaptor implements OrganizationRepository<Organization> {

    private final OrganizationRepository<Organization> repository;
    private final FhirMapper mapper;

    public OrganizationRepositoryAdaptor(OrganizationRepository<Organization> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Organization> lookupOrganizationBySorCode(QualifiedId.OrganizationId sorCode) throws ServiceException {
        return this.repository.lookupOrganizationBySorCode(sorCode);
    }

    @Override
    public QualifiedId.OrganizationId getOrganizationId() throws ServiceException {
        return repository.getOrganizationId();
    }

    @Override
    public Organization fetchCurrentUsersOrganization() throws ServiceException {
        return repository.fetchCurrentUsersOrganization();
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
