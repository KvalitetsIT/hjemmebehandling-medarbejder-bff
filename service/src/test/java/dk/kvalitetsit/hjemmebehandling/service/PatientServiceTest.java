package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class PatientServiceTest {
  @InjectMocks
  private PatientService subject;

  @Mock
  private FhirClient fhirClient;
  @Mock
  private FhirMapper fhirMapper;

  private static final String CPR_1 = "0101010101";

  private static final String CAREPLAN_ID_1 = "CarePlan/careplan-1";
  private static final String CAREPLAN_ID_2 = "CarePlan/careplan-2";
  private static final String CAREPLAN_ID_3 = "CarePlan/careplan-3";
  private static final String PATIENT_ID_1 = "Patient/patient-1";

  @Test
  public void searchPatients() {
    // Arrange
    CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
    Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

    FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient);
    Mockito.when(fhirClient.searchPatients(List.of(CPR_1), CarePlan.CarePlanStatus.ACTIVE)).thenReturn(lookupResult);

    PatientModel patientModel = new PatientModel();
    Mockito.when(fhirMapper.mapPatient(patient)).thenReturn(patientModel);

    // Act
    List<PatientModel> result = subject.searchPatients(List.of(CPR_1));

    // Assert
    assertEquals(1, result.size());
    assertEquals(patientModel, result.get(0));
  }

  @Test
  public void getPatients_includeCompleted_notIncludeActive_patientWithActiveAndCompletedCareplan() {
    // Arrange
    CarePlan carePlan1 = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
    carePlan1.setStatus(CarePlan.CarePlanStatus.ACTIVE);
    CarePlan carePlan2 = buildCarePlan(CAREPLAN_ID_2, PATIENT_ID_1);
    carePlan1.setStatus(CarePlan.CarePlanStatus.COMPLETED);

    Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

    FhirLookupResult activeLookup = FhirLookupResult.fromResources(carePlan1, patient);
    FhirLookupResult inactiveLookup = FhirLookupResult.fromResources(carePlan2, patient);

    Mockito.when(fhirClient.getPatientsByStatus(CarePlan.CarePlanStatus.ACTIVE)).thenReturn(activeLookup);
    Mockito.when(fhirClient.getPatientsByStatus(CarePlan.CarePlanStatus.COMPLETED)).thenReturn(inactiveLookup);

    // Act
    var pagedetails = new PageDetails(1,10);
    var includeActive = false;
    var includeCompleted = true;
    List<PatientModel> result = subject.getPatients(includeActive,includeCompleted,pagedetails);

    // Assert
    assertEquals(0,result.size());
  }

  @Test
  public void getPatients_includeCompleted_notIncludeActive_patientWithOnlyCompletedCareplan() {
    // Arrange
    CarePlan carePlan1 = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
    carePlan1.setStatus(CarePlan.CarePlanStatus.COMPLETED);
    CarePlan carePlan2 = buildCarePlan(CAREPLAN_ID_2, PATIENT_ID_1);
    carePlan1.setStatus(CarePlan.CarePlanStatus.COMPLETED);

    Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

    FhirLookupResult activeLookup = FhirLookupResult.fromResources();
    FhirLookupResult inactiveLookup = FhirLookupResult.fromResources(carePlan1,carePlan2, patient);

    Mockito.when(fhirClient.getPatientsByStatus(CarePlan.CarePlanStatus.ACTIVE)).thenReturn(activeLookup);
    Mockito.when(fhirClient.getPatientsByStatus(CarePlan.CarePlanStatus.COMPLETED)).thenReturn(inactiveLookup);

    // Act
    var pagedetails = new PageDetails(1,10);
    var includeActive = false;
    var includeCompleted = true;
    List<PatientModel> result = subject.getPatients(includeActive,includeCompleted,pagedetails);

    // Assert
    assertEquals(1,result.size());
  }

  @Test
  public void getPatients_includeActive_notIncludeCompleted_patientWithActiveAndCompletedCareplan() {
    // Arrange
    CarePlan carePlan1 = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
    carePlan1.setStatus(CarePlan.CarePlanStatus.ACTIVE);
    CarePlan carePlan2 = buildCarePlan(CAREPLAN_ID_2, PATIENT_ID_1);
    carePlan1.setStatus(CarePlan.CarePlanStatus.COMPLETED);

    Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

    FhirLookupResult activeLookup = FhirLookupResult.fromResources(carePlan1, patient);
    FhirLookupResult inactiveLookup = FhirLookupResult.fromResources(carePlan2, patient);

    Mockito.when(fhirClient.getPatientsByStatus(CarePlan.CarePlanStatus.ACTIVE)).thenReturn(activeLookup);

    // Act
    var pagedetails = new PageDetails(1,10);
    var includeActive = true;
    var includeCompleted = false;
    List<PatientModel> result = subject.getPatients(includeActive,includeCompleted,pagedetails);

    // Assert
    assertEquals(1,result.size());
  }



  @Test
  public void searchPatients_emptyResult() {
    // Arrange
    FhirLookupResult lookupResult = FhirLookupResult.fromResources();
    Mockito.when(fhirClient.searchPatients(List.of(CPR_1), CarePlan.CarePlanStatus.ACTIVE)).thenReturn(lookupResult);

    // Act
    List<PatientModel> result = subject.searchPatients(List.of(CPR_1));

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void searchPatients_emptyResult_emptySearch() {
    // Arrange
    Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

    CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
    carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);

    CarePlan carePlan2 = buildCarePlan(CAREPLAN_ID_2, PATIENT_ID_1);
    carePlan2.setStatus(CarePlan.CarePlanStatus.COMPLETED);

    CarePlan carePlan3 = buildCarePlan(CAREPLAN_ID_3, PATIENT_ID_1);
    carePlan3.setStatus(CarePlan.CarePlanStatus.ACTIVE);

    FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan,carePlan2,carePlan3, patient);
    Mockito.when(fhirClient.searchPatients(new ArrayList<>(), CarePlan.CarePlanStatus.ACTIVE)).thenReturn(lookupResult);

    PatientModel patientModel = new PatientModel();
    Mockito.when(fhirMapper.mapPatient(patient)).thenReturn(patientModel);

    // Act
    List<PatientModel> result = subject.searchPatients(new ArrayList<>());

    // Assert
    assertEquals(1, result.size());
    assertEquals(patientModel, result.get(0));
  }

  private CarePlan buildCarePlan(String carePlanId, String patientId) {
    CarePlan carePlan = new CarePlan();

    carePlan.setId(carePlanId);
    carePlan.setSubject(new Reference(patientId));

    return carePlan;
  }

  private Patient buildPatient(String patientId, String cpr) {
    Patient patient = new Patient();

    patient.setId(patientId);

    var identifier = new Identifier();
    identifier.setSystem(Systems.CPR);
    identifier.setValue(cpr);
    patient.setIdentifier(List.of(identifier));

    return patient;
  }
}
