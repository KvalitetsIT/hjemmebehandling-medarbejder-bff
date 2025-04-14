package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.fhir.ClientAdaptor;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class PatientServiceTest {
    private static final String ORGANISATION_ID_1 = "";
    private static final String CPR_1 = "0101010101";
    private static final String CAREPLAN_ID_1 = "CarePlan/careplan-1";
    private static final String CAREPLAN_ID_2 = "CarePlan/careplan-2";
    private static final String CAREPLAN_ID_3 = "CarePlan/careplan-3";
    private static final String PATIENT_ID_1 = "Patient/patient-1";

    @InjectMocks
    private PatientService subject;
    @Mock
    private ClientAdaptor fhirClient;


    private static Stream<Arguments> getPatients_TwoResponses_ReturnAlphabetically() {
        return Stream.of(
                Arguments.of(List.of("A", "B"), List.of("A", "B"), 1, 2),
                Arguments.of(List.of("B", "A"), List.of("A", "B"), 1, 2),
                Arguments.of(List.of("C", "B", "A"), List.of("A", "B", "C"), 1, 3),
                Arguments.of(List.of("B", "A", "C"), List.of("C"), 2, 2),
                Arguments.of(List.of("C", "A", "B"), List.of("C"), 2, 2)
        );
    }

    @Test
    public void searchPatients() throws ServiceException {

        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1);
        PatientModel patient = buildPatient(PATIENT_ID_1, CPR_1);

        Mockito.when(fhirClient.searchPatients(List.of(CPR_1), CarePlanStatus.ACTIVE)).thenReturn(List.of(patient));

        var orgId = "";

        PatientModel patientModel = PatientModel.builder().build();
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);

        List<PatientModel> result = subject.searchPatients(List.of(CPR_1));


        assertEquals(1, result.size());
        assertEquals(patientModel, result.getFirst());
    }

    @Test
    public void getPatients_includeCompleted_notIncludeActive_patientWithActiveAndCompletedCareplan() throws ServiceException {

        PatientModel patient = buildPatient(PATIENT_ID_1, CPR_1);

        CarePlanModel carePlan1 = CarePlanModel.builder()
                .id(new QualifiedId(CAREPLAN_ID_1))
                .status(CarePlanStatus.ACTIVE)
                .patient(patient)
                .build();

        CarePlanModel carePlan2 = CarePlanModel.builder()
                .id(new QualifiedId(CAREPLAN_ID_2))
                .status(CarePlanStatus.COMPLETED)
                .patient(patient)
                .build();

        Mockito.when(fhirClient.getPatientsByStatus(CarePlanStatus.ACTIVE)).thenReturn(List.of(patient));
        Mockito.when(fhirClient.getPatientsByStatus(CarePlanStatus.COMPLETED)).thenReturn(List.of(patient));

        var pagedetails = new Pagination(1, 10);
        var includeActive = false;
        var includeCompleted = true;
        List<PatientModel> result = subject.getPatients(includeActive, includeCompleted, pagedetails);

        assertEquals(0, result.size());
    }

    @Test
    public void getPatients_includeCompleted_notIncludeActive_patientWithOnlyCompletedCareplan() throws ServiceException {

        CarePlanModel carePlan1 =  CarePlanModel.builder()
                .id(new QualifiedId(CAREPLAN_ID_1))
                .patient(PatientModel.builder().id(new QualifiedId(PatientServiceTest.PATIENT_ID_1)).build())
                .status(CarePlanStatus.COMPLETED)
                .build();

        CarePlanModel carePlan2 =  CarePlanModel.builder()
                .id(new QualifiedId(CAREPLAN_ID_2))
                .patient(PatientModel.builder().id(new QualifiedId(PatientServiceTest.PATIENT_ID_1)).build())
                .build();

        PatientModel patient = buildPatient(PATIENT_ID_1, CPR_1);

        Mockito.when(fhirClient.getPatientsByStatus(CarePlanStatus.ACTIVE)).thenReturn(List.of(patient));
        Mockito.when(fhirClient.getPatientsByStatus(CarePlanStatus.COMPLETED)).thenReturn(List.of(patient));

        var pagedetails = new Pagination(1, 10);
        var includeActive = false;
        var includeCompleted = true;
        List<PatientModel> result = subject.getPatients(includeActive, includeCompleted, pagedetails);

        assertEquals(1, result.size());
    }

    @ParameterizedTest
    @MockitoSettings(strictness = Strictness.LENIENT)
    @MethodSource // arguments comes from a method that is name the same as the test
    public void getPatients_TwoResponses_ReturnAlphabetically(
            List<String> names,
            List<String> namesInExpectedOrder,
            int page,
            int pageSize
    ) throws ServiceException {

        CarePlanModel carePlan1 = CarePlanModel.builder()
                .id(new QualifiedId(CAREPLAN_ID_1))
                .status(CarePlanStatus.ACTIVE)
                .patient(PatientModel.builder()
                        .id(new QualifiedId(PatientServiceTest.PATIENT_ID_1))
                        .build())
                .build();

        for (var name : names) {
            PatientModel patient = buildPatient(name, name);
            HumanName patientName = new HumanName();
            patientName.setGiven(List.of(new StringType(name)));
//            patient.setName(List.of(patientName));
//            inactiveLookup.merge(FhirLookupResult.fromResources(patient));

            PatientModel patientmodel = PatientModel.builder().givenName(name).build();

            var orgId = "";
        }

//        Mockito.when(fhirClient.getPatientsByStatus(CarePlan.CarePlanStatus.ACTIVE)).thenReturn(inactiveLookup);
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);

        var pagedetails = new Pagination(page, pageSize);
        var includeActive = true;
        var includeCompleted = false;
        List<PatientModel> result = subject.getPatients(includeActive, includeCompleted, pagedetails);

        assertEquals(namesInExpectedOrder.size(), result.size());
        for (var i = 0; i < namesInExpectedOrder.size(); i++) {
            assertEquals(namesInExpectedOrder.get(i), result.get(i).givenName());
        }
    }

    @Test
    public void getPatients_includeActive_notIncludeCompleted_patientWithActiveAndCompletedCareplan() throws ServiceException {
        CarePlanModel carePlan1 = buildCarePlan(CAREPLAN_ID_1);
//        carePlan1.setStatus(CarePlan.CarePlanStatus.ACTIVE);
//        carePlan1.setStatus(CarePlan.CarePlanStatus.COMPLETED);

        PatientModel patient = buildPatient(PATIENT_ID_1, CPR_1);

//        FhirLookupResult activeLookup = FhirLookupResult.fromResources(carePlan1, patient);

//        Mockito.when(fhirClient.getPatientsByStatus(CarePlanStatus.ACTIVE)).thenReturn(List.of(carePlan1, patient));

        var pagination = new Pagination(1, 10);
        var includeActive = true;
        var includeCompleted = false;
        List<PatientModel> result = subject.getPatients(includeActive, includeCompleted, pagination);

        assertEquals(1, result.size());
    }


    @Test
    public void searchPatients_emptyResult() throws ServiceException {
        FhirLookupResult lookupResult = FhirLookupResult.fromResources();
//        Mockito.when(fhirClient.searchPatients(List.of(CPR_1), CarePlan.CarePlanStatus.ACTIVE)).thenReturn(lookupResult);
        List<PatientModel> result = subject.searchPatients(List.of(CPR_1));
        assertTrue(result.isEmpty());
    }

    @Test
    public void searchPatients_emptyResult_emptySearch() throws ServiceException {
        PatientModel patient = buildPatient(PATIENT_ID_1, CPR_1);

        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1);
//        carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);

        CarePlanModel carePlan2 = buildCarePlan(CAREPLAN_ID_2);
//        carePlan2.setStatus(CarePlan.CarePlanStatus.COMPLETED);

        CarePlanModel carePlan3 = buildCarePlan(CAREPLAN_ID_3);
//        carePlan3.setStatus(CarePlan.CarePlanStatus.ACTIVE);

//        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, carePlan2, carePlan3, patient);
        Mockito.when(fhirClient.searchPatients(new ArrayList<>(), CarePlanStatus.ACTIVE)).thenReturn(List.of(patient));

        PatientModel patientModel = PatientModel.builder().build();
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);

        List<PatientModel> result = subject.searchPatients(new ArrayList<>());

        assertEquals(1, result.size());
        assertEquals(patientModel, result.getFirst());
    }

    private CarePlanModel buildCarePlan(String carePlanId) {
        return CarePlanModel.builder()
                .id(new QualifiedId(carePlanId))
                .patient(PatientModel.builder()
                        .id(new QualifiedId(PatientServiceTest.PATIENT_ID_1))
                        .build())
                .build();
    }

    private PatientModel buildPatient(String patientId, String cpr) {
        return PatientModel.builder()
                .id(new QualifiedId(patientId))
                .cpr(cpr)
                .build();
    }
}
