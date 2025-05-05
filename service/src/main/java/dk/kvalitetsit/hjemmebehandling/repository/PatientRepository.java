package dk.kvalitetsit.hjemmebehandling.repository;

import dk.kvalitetsit.hjemmebehandling.model.CPR;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.List;
import java.util.Optional;

public interface PatientRepository<Patient, CarePlanStatus> extends Repository<Patient, QualifiedId.PatientId> {


    /**
     * Looks up a patient by CPR number.
     *
     * @param cpr The CPR number.
     * @return An optional patient.
     */
    Optional<Patient> fetch(CPR cpr) throws ServiceException; // TODO: Model the cpr and rename method from lookupPatientByCpr to fetch(CPR cpr)

    /**
     * Searches patients by given terms and care plan status.
     *
     * @param searchStrings List of search terms.
     * @param status        Care plan status filter.
     * @return List of matching patients.
     * @throws ServiceException If the operation fails.
     */
    List<Patient> searchPatients(List<String> searchStrings, CarePlanStatus status) throws ServiceException;

    /**
     * Retrieves patients by care plan status.
     *
     * @param status The care plan status.
     * @return List of matching patients.
     * @throws ServiceException If the operation fails.
     */
    List<Patient> fetchByStatus(CarePlanStatus status) throws ServiceException;



}
