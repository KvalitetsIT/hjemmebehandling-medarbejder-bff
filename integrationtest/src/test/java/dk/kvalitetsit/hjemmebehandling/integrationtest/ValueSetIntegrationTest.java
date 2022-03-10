package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.ValueSetApi;
import org.openapitools.client.model.MeasurementTypeDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValueSetIntegrationTest extends AbstractIntegrationTest {
    private ValueSetApi subject;

    @BeforeEach
    public void setup() {
        subject = new ValueSetApi();

        subject.getApiClient().setBasePath(enhanceBasePath(subject.getApiClient().getBasePath()));
    }

    @Test
    public void getMeasurementTypes_success() throws Exception {
        // Arrange

        // Act
        ApiResponse<List<MeasurementTypeDto>> response = subject.getMeasurementTypesWithHttpInfo();

        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(!response.getData().isEmpty());
    }
}
