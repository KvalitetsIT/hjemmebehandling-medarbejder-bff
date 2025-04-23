package dk.kvalitetsit.hjemmebehandling.fhir.repository;



import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.CPR;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;
import java.util.Optional;

public class PatientRepositoryAdaptor implements PatientRepository<PatientModel, CarePlanStatus> {

    private final PatientRepository<Patient, CarePlan.CarePlanStatus> repository;
    private final FhirMapper mapper;

    public PatientRepositoryAdaptor(PatientRepository<Patient, CarePlan.CarePlanStatus> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }


    @Override
    public void update(PatientModel patientModel) {
        repository.update(mapper.mapPatientModel(patientModel));
    }

    @Override
    public Optional<PatientModel> fetch(CPR cpr) {
        throw new NotImplementedException();
    }

    @Override
    public List<PatientModel> searchPatients(List<String> searchStrings, CarePlanStatus carePlanStatus) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<PatientModel> getPatientsByStatus(CarePlanStatus carePlanStatus) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public String save(PatientModel resource) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public Optional<PatientModel> fetch(QualifiedId id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<PatientModel> fetch(List<QualifiedId> id) {
        throw new NotImplementedException();
    }

    @Override
    public List<PatientModel> fetch() {
        throw new NotImplementedException();
    }
}
