package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class FhirClientTest {
    private FhirClient subject;

    @Mock
    private FhirContext context;

    private String endpoint = "http://foo";

    @BeforeEach
    public void setup() {
        subject = new FhirClient(context, endpoint);
    }

    @Test
    public void lookupCarePlanById_carePlanPresent_success() {
        // Arrange
        String carePlanId = "careplan-1";
        CarePlan carePlan = new CarePlan();
        setupReadCarePlanClient(carePlanId, carePlan);

        // Act
        Optional<CarePlan> result = subject.lookupCarePlanById(carePlanId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(carePlan, result.get());
    }

    @Test
    public void lookupCarePlanById_carePlanMissing_empty() {
        // Arrange
        String carePlanId = "careplan-1";
        setupReadCarePlanClient(carePlanId, null);

        // Act
        Optional<CarePlan> result = subject.lookupCarePlanById(carePlanId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void lookupCarePlanByPatientId_carePlanPresent_success() {
        // Arrange
        String patientId = "patient-1";
        CarePlan carePlan = new CarePlan();
        setupSearchCarePlanClient(carePlan);

        // Act
        List<CarePlan> result = subject.lookupCarePlansByPatientId(patientId);

        // Assert
        assertEquals(1, result.size());
        assertEquals(carePlan, result.get(0));
    }

    @Test
    public void lookupCarePlanByPatientId_carePlanMissing_empty() {
        // Arrange
        String patientId = "patient-1";
        setupSearchCarePlanClient();

        // Act
        List<CarePlan> result = subject.lookupCarePlansByPatientId(patientId);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void lookupPatientByCpr_patientPresent_success() {
        // Arrange
        String cpr = "0101010101";
        Patient patient = new Patient();
        setupSearchPatientClient(patient);

        // Act
        Optional<Patient> result = subject.lookupPatientByCpr(cpr);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(patient, result.get());
    }

    @Test
    public void lookupPatientByCpr_patientMissing_empty() {
        // Arrange
        String cpr = "0101010101";
        setupSearchPatientClient();

        // Act
        Optional<Patient> result = subject.lookupPatientByCpr(cpr);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void lookupPatientById_patientPresent_success() {
        // Arrange
        String id = "patient-1";
        Patient patient = new Patient();
        setupReadPatientClient(id, patient);

        // Act
        Optional<Patient> result = subject.lookupPatientById(id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(patient, result.get());
    }

    @Test
    public void lookupPatientById_patientMissing_empty() {
        // Arrange
        String id = "patient-1";
        setupReadPatientClient(id, null);

        // Act
        Optional<Patient> result = subject.lookupPatientById(id);

        // Assert
        assertFalse(result.isPresent());
    }



    private void setupReadCarePlanClient(String carePlanId, CarePlan carePlan) {
        setupReadClient(carePlanId, carePlan, CarePlan.class);
    }

    private void setupReadPatientClient(String patientId, Patient patient) {
        setupReadClient(patientId, patient, Patient.class);
    }

    private void setupReadClient(String id, Resource resource, Class<? extends Resource> resourceClass) {
        IGenericClient client = Mockito.mock(IGenericClient.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(client
            .read()
            .resource(resourceClass)
            .withId(Mockito.anyString())
            .execute())
            .then((a) -> {
                if(resource == null) {
                    throw new ResourceNotFoundException("error");
                }
                return resource;
            });

        Mockito.when(context.newRestfulGenericClient(endpoint)).thenReturn(client);
    }

    private void setupSearchCarePlanClient(CarePlan... carePlans) {
        setupSearchClient(CarePlan.class, carePlans);
    }

    private void setupSearchPatientClient(Patient... patients) {
        setupSearchClient(Patient.class, patients);
    }

    private void setupSearchClient(Class<? extends Resource> resourceClass, Resource... resources) {
        IGenericClient client = Mockito.mock(IGenericClient.class, Mockito.RETURNS_DEEP_STUBS);

        Bundle bundle = new Bundle();

        for(Resource resource : resources) {
            Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
            component.setResource(resource);
            bundle.addEntry(component);
        }
        bundle.setTotal(resources.length);

        Mockito.when(client
            .search()
            .forResource(resourceClass)
            .where(Mockito.any(ICriterion.class))
            .execute())
            .thenReturn(bundle);

        Mockito.when(context.newRestfulGenericClient(endpoint)).thenReturn(client);
    }
}