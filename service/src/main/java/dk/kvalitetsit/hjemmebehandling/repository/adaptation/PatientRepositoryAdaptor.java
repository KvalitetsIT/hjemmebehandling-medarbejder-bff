package dk.kvalitetsit.hjemmebehandling.repository.adaptation;


import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.CPR;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.repository.PatientRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Adapter responsible for translating between FHIR resources and domain-specific logic.
 * <p>
 * This class primarily handles the mapping of business models to domain representations
 * and delegates calls deeper into the application stack with the appropriate arguments.
 * <p>
 * Currently, it implements the {@link PatientRepository} interface for {@link PatientModel} entities.
 * Note that this implementation detail may change in the future.
 */
public class PatientRepositoryAdaptor implements PatientRepository<PatientModel, CarePlanStatus> {

    private final PatientRepository<Patient, CarePlan.CarePlanStatus> repository;
    private final FhirMapper mapper;

    public PatientRepositoryAdaptor(PatientRepository<Patient, CarePlan.CarePlanStatus> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void update(PatientModel patientModel) throws ServiceException, AccessValidationException {
        repository.update(mapper.mapPatientModel(patientModel));
    }

    @Override
    public Optional<PatientModel> fetch(CPR cpr) throws ServiceException {
        return repository.fetch(cpr).map(mapper::mapPatient);
    }

    @Override
    public List<PatientModel> searchPatients(List<String> searchStrings, CarePlanStatus carePlanStatus) throws ServiceException, AccessValidationException {
        return repository.searchPatients(searchStrings, mapper.mapCarePlanStatus(carePlanStatus)).stream().map(mapper::mapPatient).toList();
    }

    @Override
    public List<PatientModel> fetchByStatus(CarePlanStatus... carePlanStatus) throws ServiceException, AccessValidationException {
        var status = Arrays.stream(carePlanStatus).map(mapper::mapCarePlanStatus).toList();
        var result = repository.fetchByStatus(status.toArray(new CarePlan.CarePlanStatus[0]));
        return result.stream().map(mapper::mapPatient).toList();
    }

    @Override
    public QualifiedId.PatientId save(PatientModel resource) throws ServiceException {
        return repository.save(mapper.mapPatientModel(resource));
    }

    @Override
    public Optional<PatientModel> fetch(QualifiedId.PatientId id) throws ServiceException, AccessValidationException {
        return repository.fetch(id).map(mapper::mapPatient);
    }

    @Override
    public List<PatientModel> fetch(List<QualifiedId.PatientId> id) throws ServiceException, AccessValidationException {
        return repository.fetch(id).stream().map(mapper::mapPatient).toList();
    }

    @Override
    public List<PatientModel> fetch() throws ServiceException, AccessValidationException {
        return repository.fetch().stream().map(mapper::mapPatient).toList();
    }

    @Override
    public List<PatientModel> history(QualifiedId.PatientId id) throws ServiceException, AccessValidationException {
        return repository.history(id).stream().map(mapper::mapPatient).toList();
    }

    @Override
    public List<PatientModel> history(List<QualifiedId.PatientId> ids) throws ServiceException, AccessValidationException {
        return repository.history(ids).stream().map(mapper::mapPatient).toList();
    }
}
