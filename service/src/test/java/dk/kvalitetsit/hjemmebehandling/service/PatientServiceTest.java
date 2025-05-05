package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.repository.PatientRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcretePatientService;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
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
    private static final QualifiedId.OrganizationId ORGANISATION_ID_1 = new QualifiedId.OrganizationId("");
    private static final CPR CPR_1 = new CPR("0101010101");
    private static final QualifiedId.CarePlanId CAREPLAN_ID_1 = new QualifiedId.CarePlanId("careplan-1");
    private static final QualifiedId.CarePlanId CAREPLAN_ID_2 = new QualifiedId.CarePlanId("careplan-2");
    private static final QualifiedId.CarePlanId CAREPLAN_ID_3 = new QualifiedId.CarePlanId("careplan-3");
    private static final QualifiedId.PatientId PATIENT_ID_1 = new QualifiedId.PatientId("patient-1");

    @InjectMocks
    private ConcretePatientService subject;

    @Mock
    private PatientRepository<PatientModel, CarePlanStatus> patientRepository;

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

        Mockito.when(patientRepository.searchPatients(List.of(CPR_1), CarePlanStatus.ACTIVE)).thenReturn(List.of(patient));

        PatientModel patientModel = PatientModel.builder().build();

        List<PatientModel> result = subject.searchPatients(List.of(CPR_1));


        assertEquals(1, result.size());
        assertEquals(patientModel, result.getFirst());
    }

    @Test
    public void getPatients_includeCompleted_notIncludeActive_patientWithActiveAndCompletedCareplan() throws ServiceException {
        PatientModel patient = buildPatient(PATIENT_ID_1, CPR_1);

        Mockito.when(patientRepository.fetchByStatus(CarePlanStatus.ACTIVE)).thenReturn(List.of(patient));
        Mockito.when(patientRepository.fetchByStatus(CarePlanStatus.COMPLETED)).thenReturn(List.of(patient));

        var pagedetails = new Pagination(1, 10);
        var includeActive = false;
        var includeCompleted = true;
        List<PatientModel> result = subject.getPatients(includeActive, includeCompleted, pagedetails);

        assertEquals(0, result.size());
    }

    @Test
    public void getPatients_includeCompleted_notIncludeActive_patientWithOnlyCompletedCareplan() throws ServiceException {
        PatientModel patient = buildPatient(PATIENT_ID_1, CPR_1);

        Mockito.when(patientRepository.fetchByStatus(CarePlanStatus.ACTIVE)).thenReturn(List.of(patient));
        Mockito.when(patientRepository.fetchByStatus(CarePlanStatus.COMPLETED)).thenReturn(List.of(patient));

        var pagedetails = new Pagination(1, 10);
        var includeActive = false;
        var includeCompleted = true;
        List<PatientModel> result = subject.getPatients(includeActive, includeCompleted, pagedetails);

        assertEquals(1, result.size());
    }

    @ParameterizedTest
    @MockitoSettings(strictness = Strictness.LENIENT)
    @MethodSource // arguments comes from a method that is name the same as the test
    public void getPatients_TwoResponses_ReturnAlphabetically(List<String> names, List<String> namesInExpectedOrder, int page, int pageSize) throws ServiceException {
        List<PatientModel> patients = names.stream().map(name -> PatientModel.builder()
                .name(PersonNameModel.builder()
                        .given(List.of(name))
                        .build()
                ).build()).toList();

        Mockito.when(patientRepository.fetchByStatus(CarePlanStatus.ACTIVE)).thenReturn(patients);

        var pagedetails = new Pagination(page, pageSize);
        var includeActive = true;
        var includeCompleted = false;
        List<PatientModel> result = subject.getPatients(includeActive, includeCompleted, pagedetails);

        assertEquals(namesInExpectedOrder.size(), result.size());
        for (var i = 0; i < namesInExpectedOrder.size(); i++) {
            assertEquals(namesInExpectedOrder.get(i), result.get(i).name().given().getFirst());
        }
    }

    @Test
    public void getPatients_includeActive_notIncludeCompleted_patientWithActiveAndCompletedCareplan() throws ServiceException {
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
        PatientModel patient = PatientModel.builder()
                .id(PatientServiceTest.PATIENT_ID_1)
                .build();

        CarePlanModel carePlan1 = CarePlanModel.builder()
                .id(CAREPLAN_ID_1)
                .status(CarePlanStatus.ACTIVE)
                .patient(patient)
                .build();

        CarePlanModel carePlan2 = CarePlanModel.builder()
                .id(CAREPLAN_ID_2)
                .status(CarePlanStatus.COMPLETED)
                .patient(patient)
                .build();

        CarePlanModel carePlan3 = CarePlanModel.builder()
                .id(CAREPLAN_ID_3)
                .status(CarePlanStatus.ACTIVE)
                .patient(patient)
                .build();

        Mockito.when(patientRepository.searchPatients(new ArrayList<>(), CarePlanStatus.ACTIVE)).thenReturn(List.of(patient));
        PatientModel patientModel = PatientModel.builder().build();

        List<PatientModel> result = subject.searchPatients(new ArrayList<>());

        assertEquals(1, result.size());
        assertEquals(patientModel, result.getFirst());
    }

    private CarePlanModel buildCarePlan(QualifiedId.CarePlanId carePlanId) {
        return CarePlanModel.builder()
                .id(carePlanId)
                .patient(PatientModel.builder()
                        .id(PatientServiceTest.PATIENT_ID_1)
                        .build())
                .build();
    }

    private PatientModel buildPatient(QualifiedId.PatientId patientId, CPR cpr) {
        return PatientModel.builder()
                .id(patientId)
                .cpr(cpr)
                .build();
    }
}
