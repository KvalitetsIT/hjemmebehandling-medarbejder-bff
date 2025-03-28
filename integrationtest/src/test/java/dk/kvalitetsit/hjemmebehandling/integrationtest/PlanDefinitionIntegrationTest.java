package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.PlanDefinitionApi;
import org.openapitools.client.model.CreatePlanDefinitionRequest;
import org.openapitools.client.model.PlanDefinitionDto;
import org.openapitools.client.model.QuestionnaireDto;
import org.openapitools.client.model.QuestionnaireWrapperDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlanDefinitionIntegrationTest extends AbstractIntegrationTest {
    private PlanDefinitionApi subject;

    @BeforeEach
    public void setup() {
        subject = new PlanDefinitionApi();
        subject.getApiClient().setBasePath(enhanceBasePath(subject.getApiClient().getBasePath()));
    }

    @Test
    public void getPlanDefinitions_success() throws Exception {
        ApiResponse<List<PlanDefinitionDto>> response = subject.getPlanDefinitionsWithHttpInfo(List.of());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void createPlanDefinition_success() throws Exception {
        QuestionnaireDto questionnaireDto = new QuestionnaireDto();
        questionnaireDto.setId("Questionnaire/questionnaire-1");

        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto()
                .status("ACTIVE")
                .questionnaires(List.of(new QuestionnaireWrapperDto()
                        .questionnaire(questionnaireDto)));

        planDefinitionDto.setId("PlanDefinition/plandefinition-1");

        CreatePlanDefinitionRequest request = new CreatePlanDefinitionRequest()
                .planDefinition(planDefinitionDto);

        ApiResponse<Void> response = subject.createPlanDefinitionWithHttpInfo(request);

        assertEquals(201, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey("location"));
    }


}
