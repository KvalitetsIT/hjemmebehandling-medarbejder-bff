package dk.kvalitetsit.hjemmebehandling.repository.access;

import dk.kvalitetsit.hjemmebehandling.model.PractitionerModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.PractitionerRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.List;
import java.util.Optional;

public class ValidatedPractitionerRepository implements PractitionerRepository<PractitionerModel> {

    private final AccessValidator<PractitionerModel> accessValidator;
    private final PractitionerRepository<PractitionerModel> repository;

    public ValidatedPractitionerRepository(AccessValidator<PractitionerModel> accessValidator, PractitionerRepository<PractitionerModel> repository) {
        this.accessValidator = accessValidator;
        this.repository = repository;
    }

    @Override
    public PractitionerModel getOrCreateUserAsPractitioner() throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.getOrCreateUserAsPractitioner());
    }

    @Override
    public void update(PractitionerModel resource) throws ServiceException {
        repository.update(resource);
    }

    @Override
    public QualifiedId.PractitionerId save(PractitionerModel resource) throws ServiceException {
        return repository.save(resource);
    }

    @Override
    public Optional<PractitionerModel> fetch(QualifiedId.PractitionerId id) throws ServiceException, AccessValidationException {
        var result = repository.fetch(id);
        if (result.isPresent()) return Optional.of(accessValidator.validateAccess(result.get()));
        return result;
    }

    @Override
    public List<PractitionerModel> fetch(List<QualifiedId.PractitionerId> id) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch(id));
    }

    @Override
    public List<PractitionerModel> fetch() throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch());
    }

    @Override
    public List<PractitionerModel> history(List<QualifiedId.PractitionerId> ids) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.history(ids));
    }

    @Override
    public List<PractitionerModel> history(QualifiedId.PractitionerId id) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.history(id));
    }
}
