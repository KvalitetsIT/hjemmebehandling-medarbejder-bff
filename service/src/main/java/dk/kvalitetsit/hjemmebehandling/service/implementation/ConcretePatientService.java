package dk.kvalitetsit.hjemmebehandling.service.implementation;

import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;
import dk.kvalitetsit.hjemmebehandling.api.Paginator;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.model.CPR;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.repository.PatientRepository;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConcretePatientService implements PatientService {

    private final PatientRepository<PatientModel, CarePlanStatus> patientRepository;

    private CustomUserClient customUserService;

    public ConcretePatientService(PatientRepository<PatientModel, CarePlanStatus> patientRepository) {
        this.patientRepository = patientRepository;
    }

    public void createPatient(PatientModel patientModel) throws ServiceException {
        try {
            var customerUserLinkId = customUserService.createUser(patientModel).map(CustomUserResponseDto::getId).orElseThrow();
            var modifiedPatient = PatientModel.Builder
                    .from(patientModel)
                    .customUserId(customerUserLinkId)
                    .build();
            patientRepository.save(modifiedPatient);
        } catch (Exception e) {
            throw new ServiceException("Error saving patient", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }


    public PatientModel getPatient(CPR cpr) throws ServiceException {
        return patientRepository.fetch(cpr).orElse(null);
    }

    /**
     * Note: patients with a missing CPR are excluded during a distinction/filter
     * @param includeActive
     * @param includeCompleted
     * @return a paginated list of patients with a status of either active, complete or both
     * @throws ServiceException
     * @throws AccessValidationException
     */
    // TODO: Bad Practice... replace 'includeActive' and 'includeCompleted' with 'CarePlanStatus...  status'
    public List<PatientModel> getPatients(boolean includeActive, boolean includeCompleted) throws ServiceException, AccessValidationException {
        var status = new ArrayList<CarePlanStatus>();

        if (includeActive) status.add(CarePlanStatus.ACTIVE);
        if (includeCompleted) status.add(CarePlanStatus.COMPLETED);

        var distinctPatients = patientRepository.fetchByStatus(status.toArray(new CarePlanStatus[0])).stream()
                .collect(Collectors.toMap(PatientModel::cpr, Function.identity(),(existing, replacement) -> existing))
                .values();

        return distinctPatients
                .stream()
                .sorted(Comparator.comparing((x) -> x.name().given().getFirst()))
                .toList();
    }

    public List<PatientModel> getPatients(boolean includeActive, boolean includeCompleted, Pagination pagination) throws ServiceException, AccessValidationException {
        List<PatientModel> patients = this.getPatients(includeActive, includeCompleted);
        return Paginator.paginate(patients, pagination);
    }

    public List<PatientModel> searchActivePatients(List<String> searchStrings) throws ServiceException, AccessValidationException {
        return patientRepository.searchPatients(searchStrings, CarePlanStatus.ACTIVE);
    }
}
