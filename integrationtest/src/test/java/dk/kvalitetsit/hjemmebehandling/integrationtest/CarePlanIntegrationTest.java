package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.CarePlanApi;
import org.openapitools.client.model.CarePlanDto;
import org.openapitools.client.model.CreateCarePlanRequest;

import static org.junit.jupiter.api.Assertions.*;

public class CarePlanIntegrationTest extends AbstractIntegrationTest {
    private CarePlanApi subject;

    @BeforeEach
    public void setup() {
        subject = new CarePlanApi();

        subject.getApiClient().setBasePath(enhanceBasePath(subject.getApiClient().getBasePath()));
    }

    @Test
    public void createCarePlan_success() throws Exception {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest()
                .cpr("0101010101")
                .planDefinitionId("plandefinition-1");

        // Act
        ApiResponse<Void> response = subject.createCarePlanWithHttpInfo(request);

        // Assert
        assertEquals(201, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey("location"));
    }

    @Test
    public void getCarePlan_success() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        // Act
        ApiResponse<CarePlanDto> response = subject.getCarePlanByIdWithHttpInfo(carePlanId);

        // Assert
        assertEquals(200, response.getStatusCode());
    }
}
