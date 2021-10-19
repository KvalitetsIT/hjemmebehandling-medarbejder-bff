package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.checkerframework.checker.units.qual.C;
import org.hl7.fhir.r4.model.CarePlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

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
    public void lookupCarePlan_carePlanPresent_success() {
        // Arrange
        String carePlanId = "careplan-1";
        CarePlan carePlan = new CarePlan();
        setupClient(carePlanId, carePlan);

        // Act
        Optional<CarePlan> result = subject.lookupCarePlan(carePlanId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(carePlan, result.get());
    }

    @Test
    public void lookupCarePlan_carePlanMissing_empty() {
        // Arrange
        String carePlanId = "careplan-1";
        setupClient(carePlanId, null);

        // Act
        Optional<CarePlan> result = subject.lookupCarePlan(carePlanId);

        // Assert
        assertFalse(result.isPresent());
    }

    private void setupClient(String carePlanId, CarePlan carePlan) {
        IGenericClient client = Mockito.mock(IGenericClient.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(client
                .read()
                .resource(CarePlan.class)
                .withId(carePlanId)
                .execute())
                .then((a) -> {
                    if(carePlan == null) {
                        throw new ResourceNotFoundException("error");
                    }
                    return carePlan;
                });

        Mockito.when(context.newRestfulGenericClient(endpoint)).thenReturn(client);
    }
}