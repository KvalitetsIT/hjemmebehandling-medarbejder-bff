package dk.kvalitetsit.hjemmebehandling.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;

import dk.kvalitetsit.hjemmebehandling.api.PaginatedList;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PatientService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    // TODO: Should be split into one which is only concerned about patient
    private final FhirClient<
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
            FhirClient<
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

    public List<PatientModel> getPatients(String clinicalIdentifier) {
        FhirContext context = FhirContext.forR4();
        IGenericClient client = context.newRestfulGenericClient("http://hapi-server:8080/fhir");

        Bundle bundle = (Bundle) client.search().forResource("Patient").prettyPrint().execute();

        PatientModel p = PatientModel.builder()
                .cpr("0101010101")
                .familyName("Ærtegærde Ømø Ååstrup")
                .givenName("Torgot")
                .build();

        return List.of(p);
    }

    public PatientModel patient(String cpr) throws ServiceException {
        // Look up the patient
        Optional<PatientModel> patient = fhirClient.lookupPatientByCpr(cpr);
        if (patient.isEmpty()) {
            return null;
        }

        var orgId = fhirClient.getOrganizationId();

        // Map to the domain model
        return patient.get();
    }

    boolean patientIsInList(PatientModel patientToSearchFor, List<PatientModel> listToSearchForPatient) {
        return listToSearchForPatient
                .stream().
                anyMatch(listP -> Objects.equals(
                        listP.cpr(),
                        patientToSearchFor.cpr()
                ));
    }

    public List<PatientModel> getPatients(boolean includeActive, boolean includeCompleted) throws ServiceException {

        var patients = new ArrayList<PatientModel>();

        var patientsWithActiveCareplan = fhirClient.getPatientsByStatus(CarePlanStatus.ACTIVE);

        if (includeActive)
            patients.addAll(patientsWithActiveCareplan);

        if (includeCompleted) {
            var patientsWithInactiveCareplan = fhirClient.getPatientsByStatus(CarePlanStatus.COMPLETED);
            patientsWithInactiveCareplan.removeIf(potentialPatient -> patientIsInList(potentialPatient, patientsWithActiveCareplan));
            patients.addAll(patientsWithInactiveCareplan);
        }

        var orgId = fhirClient.getOrganizationId();

        // Map the resources
        return patients
                .stream()
                .sorted(Comparator.comparing(PatientModel::givenName))
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
