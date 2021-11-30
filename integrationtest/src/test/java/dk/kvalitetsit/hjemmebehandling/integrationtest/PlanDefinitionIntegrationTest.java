package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.PlanDefinitionApi;
import org.openapitools.client.model.PlanDefinitionDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlanDefinitionIntegrationTest extends AbstractIntegrationTest {
    private PlanDefinitionApi subject;

    @BeforeEach
    public void setup() {
        subject = new PlanDefinitionApi();

        subject.getApiClient().setBasePath(enhanceBasePath(subject.getApiClient().getBasePath()));
    }

    @Test
    public void getPlanDefinitions_success() throws Exception {
        // Arrange

        // Act
        ApiResponse<List<PlanDefinitionDto>> response = subject.getPlanDefinitionsWithHttpInfo();

        // Assert
        assertEquals(201, response.getStatusCode());
    }
}
