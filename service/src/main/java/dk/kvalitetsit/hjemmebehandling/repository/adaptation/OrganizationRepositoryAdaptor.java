package dk.kvalitetsit.hjemmebehandling.repository.adaptation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.OrganizationModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.OrganizationRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
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
public class OrganizationRepositoryAdaptor implements OrganizationRepository<OrganizationModel> {

    private final OrganizationRepository<Organization> repository;
    private final FhirMapper mapper;

    public OrganizationRepositoryAdaptor(OrganizationRepository<Organization> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<OrganizationModel> lookupOrganizationBySorCode(QualifiedId.OrganizationId sorCode) throws ServiceException {
        return this.repository.lookupOrganizationBySorCode(sorCode).map(mapper::mapOrganization);
    }

    @Override
    public QualifiedId.OrganizationId getOrganizationId() throws ServiceException {
        return repository.getOrganizationId();
    }

    @Override
    public OrganizationModel fetchCurrentUsersOrganization() throws ServiceException, AccessValidationException {
        return mapper.mapOrganization(repository.fetchCurrentUsersOrganization());
    }

    @Override
    public void update(OrganizationModel resource) throws ServiceException {
        repository.update(mapper.mapOrganization(resource));
    }

    @Override
    public QualifiedId.OrganizationId save(OrganizationModel resource) throws ServiceException {
        return repository.save(mapper.mapOrganization(resource));
    }

    @Override
    public Optional<OrganizationModel> fetch(QualifiedId.OrganizationId id) throws ServiceException, AccessValidationException {
        return repository.fetch(id).map(mapper::mapOrganization);
    }

    @Override
    public List<OrganizationModel> fetch(List<QualifiedId.OrganizationId> ids) throws ServiceException, AccessValidationException {
        return repository.fetch(ids).stream().map(mapper::mapOrganization).toList();
    }

    @Override
    public List<OrganizationModel> fetch() throws ServiceException, AccessValidationException {
        return repository.fetch().stream().map(mapper::mapOrganization).toList();
    }

    @Override
    public List<OrganizationModel> history(QualifiedId.OrganizationId id) throws ServiceException, AccessValidationException {
        return repository.history(id).stream().map(mapper::mapOrganization).toList();
    }

    @Override
    public List<OrganizationModel> history(List<QualifiedId.OrganizationId> organizationIds) throws ServiceException, AccessValidationException {
        return repository.history(organizationIds).stream().map(mapper::mapOrganization).toList();
    }
}
