package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.CarePlanApi;
import org.openapitools.client.model.CarePlanDto;
import org.openapitools.client.model.CreateCarePlanRequest;

import static org.junit.jupiter.api.Assertions.*;

public class CarePlanIntegrationTest {
    private CarePlanApi subject;

    @BeforeEach
    public void setup() {
        subject = new CarePlanApi();

        String basePath = subject.getApiClient().getBasePath().replace("8586", "8080");
        subject.getApiClient().setBasePath(basePath);
    }

    @Test
    public void createCarePlan_success() throws Exception {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest()
                .cpr("0101010101")
                .planDefinitionId("2");

        // Act
        ApiResponse<Void> response = subject.createCarePlanWithHttpInfo(request);

        // Assert
        assertEquals(201, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey("Location"));
    }

    @Test
    public void getCarePlan_success() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        // Act
        ApiResponse<CarePlanDto> response = subject.getCarePlanWithHttpInfo(carePlanId);

        // Assert
        assertEquals(200, response.getStatusCode());
    }
}
