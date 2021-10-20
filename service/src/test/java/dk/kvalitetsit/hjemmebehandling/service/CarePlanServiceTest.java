package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CarePlanServiceTest {
    @InjectMocks
    private CarePlanService subject;

    @Mock
    private FhirClient fhirClient;

    @Mock
    private FhirMapper fhirMapper;

    @Mock
    private FhirObjectBuilder fhirObjectBuilder;

    @Test
    public void getCarePlan_carePlanPresent_returnsCarePlan() {
        // Arrange
        String carePlanId = "careplan-1";
        String patientId = "Patient/patient-1";

        CarePlanModel carePlanModel = setupCarePlan(carePlanId, patientId);
        PatientModel patientModel = setupPatient(patientId);

        // Act
        Optional<CarePlanModel> result = subject.getCarePlan(carePlanId);

        // Assert
        assertEquals(carePlanModel, result.get());
        assertEquals(patientModel, result.get().getPatient());
    }

    @Test
    public void getCarePlan_patientMissing_throwsException() {
        // Arrange
        String carePlanId = "careplan-1";
        String patientId = "Patient/patient-1";

        CarePlanModel carePlanModel = setupCarePlan(carePlanId, patientId);
        Mockito.when(fhirClient.lookupPatientById(patientId)).thenReturn(Optional.empty());

        // Act

        // Assert
        assertThrows(IllegalStateException.class, () -> subject.getCarePlan(carePlanId));
    }

    @Test
    public void getCarePlan_carePlanMissing_returnsEmpty() {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.when(fhirClient.lookupCarePlan(carePlanId)).thenReturn(Optional.empty());

        // Act
        Optional<CarePlanModel> result = subject.getCarePlan(carePlanId);

        // Assert
        assertFalse(result.isPresent());
    }

    private CarePlanModel setupCarePlan(String carePlanId, String patientId) {
        CarePlan carePlan = buildCarePlan(carePlanId, patientId);
        Mockito.when(fhirClient.lookupCarePlan(carePlanId)).thenReturn(Optional.of(carePlan));

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(fhirMapper.mapCarePlan(carePlan)).thenReturn(carePlanModel);

        return carePlanModel;
    }

    private PatientModel setupPatient(String patientId) {
        Patient patient = new Patient();
        Mockito.when(fhirClient.lookupPatientById(patientId)).thenReturn(Optional.of(patient));

        PatientModel patientModel = new PatientModel();
        Mockito.when(fhirMapper.mapPatient(patient)).thenReturn(patientModel);

        return patientModel;
    }

    private CarePlan buildCarePlan(String carePlanId, String patientId) {
        CarePlan carePlan = new CarePlan();

        carePlan.setId(carePlanId);
        carePlan.setSubject(new Reference(patientId));

        return carePlan;
    }
}