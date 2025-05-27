package dk.kvalitetsit.hjemmebehandling.repository.access;

import dk.kvalitetsit.hjemmebehandling.model.CPR;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.repository.PatientRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.List;
import java.util.Optional;


public class ValidatedPatientRepository implements PatientRepository<PatientModel, CarePlanStatus> {

    private final AccessValidator<PatientModel> accessValidator;
    private final PatientRepository<PatientModel, CarePlanStatus> repository;

    public ValidatedPatientRepository(AccessValidator<PatientModel> accessValidator, PatientRepository<PatientModel, CarePlanStatus> repository) {
        this.accessValidator = accessValidator;
        this.repository = repository;
    }

    @Override
    public Optional<PatientModel> fetch(CPR cpr) throws ServiceException {
        return repository.fetch(cpr);
    }

    @Override
    public List<PatientModel> searchPatients(List<String> searchStrings, CarePlanStatus carePlanStatus) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.searchPatients(searchStrings, carePlanStatus));
    }

    @Override
    public List<PatientModel> fetchByStatus(CarePlanStatus... carePlanStatus) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetchByStatus(carePlanStatus));
    }

    @Override
    public void update(PatientModel resource) throws ServiceException, AccessValidationException {
        repository.update(resource);
    }

    @Override
    public QualifiedId.PatientId save(PatientModel resource) throws ServiceException {
        return repository.save(resource);
    }

    @Override
    public Optional<PatientModel> fetch(QualifiedId.PatientId id) throws ServiceException, AccessValidationException {
        var result = repository.fetch(id);
        if (result.isPresent()) return Optional.of(accessValidator.validateAccess(result.get()));
        return result;
    }

    @Override
    public List<PatientModel> fetch(List<QualifiedId.PatientId> id) throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch(id));
    }

    @Override
    public List<PatientModel> fetch() throws ServiceException, AccessValidationException {
        return accessValidator.validateAccess(repository.fetch());
    }

    @Override
    public List<PatientModel> history(QualifiedId.PatientId id) throws ServiceException, AccessValidationException {
        return repository.history(id);
    }

    @Override
    public List<PatientModel> history(List<QualifiedId.PatientId> patientIds) throws ServiceException, AccessValidationException {
        return repository.history(patientIds);
    }
}
