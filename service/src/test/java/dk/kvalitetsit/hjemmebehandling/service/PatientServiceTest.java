package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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
  private static final String PATIENT_ID_1 = "Patient/patient-1";

  @Test
  public void searchPatients() {
    // Arrange
    CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
    Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

    FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient);
    Mockito.when(fhirClient.searchPatientsWithActiveCarePlan(List.of(CPR_1))).thenReturn(lookupResult);

    PatientModel patientModel = new PatientModel();
    Mockito.when(fhirMapper.mapPatient(patient)).thenReturn(patientModel);

    // Act
    List<PatientModel> result = subject.searchPatients(List.of(CPR_1));

    // Assert
    assertEquals(1, result.size());
    assertEquals(patientModel, result.get(0));
  }

  @Test
  public void searchPatients_emptyResult() {
    // Arrange
    FhirLookupResult lookupResult = FhirLookupResult.fromResources();
    Mockito.when(fhirClient.searchPatientsWithActiveCarePlan(List.of(CPR_1))).thenReturn(lookupResult);

    // Act
    List<PatientModel> result = subject.searchPatients(List.of(CPR_1));

    // Assert
    assertTrue(result.isEmpty());
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
