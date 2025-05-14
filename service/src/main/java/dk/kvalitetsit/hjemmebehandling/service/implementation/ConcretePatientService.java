package dk.kvalitetsit.hjemmebehandling.service.implementation;

import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;
import dk.kvalitetsit.hjemmebehandling.api.PaginatedList;
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

public class ConcretePatientService implements PatientService {

    // TODO: Should be split into one which is only concerned about patient
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

    // TODO: Bad Practice... replace 'includeActive' and 'includeCompleted' with 'CarePlanStatus...  status'
    public List<PatientModel> getPatients(boolean includeActive, boolean includeCompleted) throws ServiceException, AccessValidationException {
        var patients = new ArrayList<PatientModel>();

        if (includeActive) {
            var patientsWithActiveCarePlan = patientRepository.fetchByStatus(CarePlanStatus.ACTIVE);
            patients.addAll(patientsWithActiveCarePlan);
        }

        if (includeCompleted) {
            var patientsWithInactiveCarePlan = patientRepository.fetchByStatus(CarePlanStatus.COMPLETED);

            var uniquePatient = patientsWithInactiveCarePlan.stream()
                    .filter(potentialPatient -> patients.stream().anyMatch(p -> p.cpr().equals(potentialPatient.cpr())))
                    .toList();

            patients.addAll(uniquePatient);
        }

        // Map the resources
        return patients
                .stream()
                .sorted(Comparator.comparing((PatientModel x) -> x.name().given().getFirst()))
                .toList();
    }

    public List<PatientModel> getPatients(boolean includeActive, boolean includeCompleted, Pagination pagination) throws ServiceException, AccessValidationException {
        List<PatientModel> patients = this.getPatients(includeActive, includeCompleted);
        return new PaginatedList<>(patients, pagination).getList();
    }

    public List<PatientModel> searchPatients(List<String> searchStrings) throws ServiceException, AccessValidationException {
        return patientRepository.searchPatients(searchStrings, CarePlanStatus.ACTIVE);
    }
}
