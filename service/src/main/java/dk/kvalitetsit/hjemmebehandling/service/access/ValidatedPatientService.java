package dk.kvalitetsit.hjemmebehandling.service.access;

import dk.kvalitetsit.hjemmebehandling.model.CPR;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;


public class ValidatedPatientService implements PatientService {

    private final AccessValidator accessValidator;
    private final PatientService service;

    public ValidatedPatientService(AccessValidator accessValidator, PatientService service) {
        this.accessValidator = accessValidator;
        this.service = service;
    }

    @Override
    public void createPatient(PatientModel patientModel) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public PatientModel getPatient(CPR cpr) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<PatientModel> getPatients(boolean includeActive, boolean includeCompleted) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<PatientModel> getPatients(boolean includeActive, boolean includeCompleted, Pagination pagination) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<PatientModel> searchPatients(List<String> searchStrings) throws ServiceException {
        throw new NotImplementedException();
    }
}
