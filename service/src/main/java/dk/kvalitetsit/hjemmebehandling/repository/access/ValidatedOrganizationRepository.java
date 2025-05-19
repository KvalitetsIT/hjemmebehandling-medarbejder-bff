package dk.kvalitetsit.hjemmebehandling.repository.access;

import dk.kvalitetsit.hjemmebehandling.model.OrganizationModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.OrganizationRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.List;
import java.util.Optional;

public class ValidatedOrganizationRepository implements OrganizationRepository<OrganizationModel> {

    private final AccessValidator<OrganizationModel> validator;
    private final OrganizationRepository<OrganizationModel> repository;


    public ValidatedOrganizationRepository(AccessValidator<OrganizationModel> validator, OrganizationRepository<OrganizationModel> repository) {
        this.validator = validator;
        this.repository = repository;
    }

    @Override
    public Optional<OrganizationModel> lookupOrganizationBySorCode(QualifiedId.OrganizationId sorCode) throws ServiceException {
        return repository.lookupOrganizationBySorCode(sorCode);
    }

    @Override
    public QualifiedId.OrganizationId getOrganizationId() throws ServiceException {
        return repository.getOrganizationId();
    }

    @Override
    public OrganizationModel fetchCurrentUsersOrganization() throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.fetchCurrentUsersOrganization());
    }

    @Override
    public void update(OrganizationModel resource) throws ServiceException {
        repository.update(resource);
    }

    @Override
    public QualifiedId.OrganizationId save(OrganizationModel resource) throws ServiceException {
        return repository.save(resource);
    }

    @Override
    public Optional<OrganizationModel> fetch(QualifiedId.OrganizationId id) throws ServiceException, AccessValidationException {
        return repository.fetch(id);
    }

    @Override
    public List<OrganizationModel> fetch(List<QualifiedId.OrganizationId> id) throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.fetch(id));
    }

    @Override
    public List<OrganizationModel> fetch() throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.fetch());
    }

    @Override
    public List<OrganizationModel> history(QualifiedId.OrganizationId id) throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.history(id));
    }

    @Override
    public List<OrganizationModel> history(List<QualifiedId.OrganizationId> organizationIds) throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.history(organizationIds));
    }
}
