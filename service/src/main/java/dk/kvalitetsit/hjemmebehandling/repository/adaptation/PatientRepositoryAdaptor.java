package dk.kvalitetsit.hjemmebehandling.repository.adaptation;



import dk.kvalitetsit.hjemmebehandling.repository.PatientRepository;
import dk.kvalitetsit.hjemmebehandling.model.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.CPR;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;
import java.util.Optional;

/**
 * An adapter whose responsibility is to adapt between FHIR and the domain logic.
 * This primarily covers mapping from business models and calling further into the stack with the expected arguments
 * For now, it implements the PatientRepository interface, but this might change in the future
 */
public class PatientRepositoryAdaptor implements PatientRepository<PatientModel, CarePlanStatus> {

    private final PatientRepository<Patient, CarePlan.CarePlanStatus> repository;
    private final FhirMapper mapper;

    public PatientRepositoryAdaptor(PatientRepository<Patient, CarePlan.CarePlanStatus> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void update(PatientModel patientModel) throws ServiceException {
        repository.update(mapper.mapPatientModel(patientModel));
    }

    @Override
    public Optional<PatientModel> fetch(CPR cpr) throws ServiceException {
        return repository.fetch(cpr).map(mapper::mapPatient);
    }

    @Override
    public List<PatientModel> searchPatients(List<String> searchStrings, CarePlanStatus carePlanStatus) throws ServiceException {
        return repository.searchPatients(searchStrings, mapper.mapCarePlanStatus(carePlanStatus)).stream().map(mapper::mapPatient).toList();
    }

    @Override
    public List<PatientModel> getPatientsByStatus(CarePlanStatus carePlanStatus) throws ServiceException {
        return repository.getPatientsByStatus(mapper.mapCarePlanStatus(carePlanStatus)).stream().map(mapper::mapPatient).toList();
    }

    @Override
    public QualifiedId.PatientId save(PatientModel resource) throws ServiceException {
        return repository.save(mapper.mapPatientModel(resource));
    }

    @Override
    public Optional<PatientModel> fetch(QualifiedId.PatientId id) throws ServiceException {
        return repository.fetch(id).map(mapper::mapPatient);
    }

    @Override
    public List<PatientModel> fetch(List<QualifiedId.PatientId> id) throws ServiceException {
        return repository.fetch(id).stream().map(mapper::mapPatient).toList();
    }

    @Override
    public List<PatientModel> fetch() throws ServiceException {
        return repository.fetch().stream().map(mapper::mapPatient).toList();
    }
}
