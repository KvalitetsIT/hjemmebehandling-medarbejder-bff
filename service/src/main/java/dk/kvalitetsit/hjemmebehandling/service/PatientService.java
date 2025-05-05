package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.model.CPR;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;

import java.util.List;

public interface PatientService {

    void createPatient(PatientModel patientModel) throws ServiceException ;

    PatientModel getPatient(CPR cpr) throws ServiceException ;

    // TODO: Bad Practice... replace 'includeActive' and 'includeCompleted' with 'CarePlanStatus...  status'
    List<PatientModel> getPatients(boolean includeActive, boolean includeCompleted) throws ServiceException ;

    List<PatientModel> getPatients(boolean includeActive, boolean includeCompleted, Pagination pagination) throws ServiceException ;

    List<PatientModel> searchPatients(List<String> searchStrings) throws ServiceException;
}
