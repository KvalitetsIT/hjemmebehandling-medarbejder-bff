package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;
import dk.kvalitetsit.hjemmebehandling.api.PaginatedList;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.client.Client;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PatientService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    // TODO: Should be split into one which is only concerned about patient
    private final Client<
            CarePlanModel,
            PlanDefinitionModel,
            PractitionerModel,
            PatientModel,
            QuestionnaireModel,
            QuestionnaireResponseModel,
            Organization,
            CarePlanStatus> fhirClient;

    private CustomUserClient customUserService;

    public PatientService(
            Client<
                    CarePlanModel,
                    PlanDefinitionModel,
                    PractitionerModel,
                    PatientModel,
                    QuestionnaireModel,
                    QuestionnaireResponseModel,
                    Organization,
                    CarePlanStatus> fhirClient,
            FhirMapper fhirMapper,
            AccessValidator accessValidator
    ) {
        super(accessValidator);

        this.fhirClient = fhirClient;

    }

    public void createPatient(PatientModel patientModel) throws ServiceException {
        try {
            var customerUserLinkId = customUserService.createUser(patientModel).map(CustomUserResponseDto::getId).orElseThrow();
            var modifiedPatient = PatientModel.Builder
                    .from(patientModel)
                    .customUserId(customerUserLinkId)
                    .build();

            fhirClient.savePatient(modifiedPatient);
        } catch (Exception e) {
            throw new ServiceException("Error saving patient", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }


    public PatientModel getPatient(String cpr) throws ServiceException {
        return fhirClient.lookupPatientByCpr(cpr).orElse(null);
    }

    // TODO: Bad Practice... replace 'includeActive' and 'includeCompleted' with 'CarePlanStatus...  status'
    public List<PatientModel> getPatients(boolean includeActive, boolean includeCompleted) throws ServiceException {

        var patients = new ArrayList<PatientModel>();

        var patientsWithActiveCarePlan = fhirClient.getPatientsByStatus(CarePlanStatus.ACTIVE);

        if (includeActive)
            patients.addAll(patientsWithActiveCarePlan);

        if (includeCompleted) {
            var patientsWithInactiveCarePlan = fhirClient.getPatientsByStatus(CarePlanStatus.COMPLETED).stream()
                    .filter(potentialPatient -> patientsWithActiveCarePlan.stream().anyMatch(p -> p.cpr().equals(potentialPatient.cpr())))
                    .toList();

            patients.addAll(patientsWithInactiveCarePlan);
        }

        // Map the resources
        return patients
                .stream()
                .sorted(Comparator.comparing((PatientModel x) -> x.name().given().getFirst()))
                .toList();
    }

    public List<PatientModel> getPatients(boolean includeActive, boolean includeCompleted, Pagination pagination) throws ServiceException {
        List<PatientModel> patients = this.getPatients(includeActive, includeCompleted);
        return new PaginatedList<>(patients, pagination).getList();
    }



    public List<PatientModel> searchPatients(List<String> searchStrings) throws ServiceException {
        return fhirClient.searchPatients(searchStrings, CarePlanStatus.ACTIVE);
    }
}
