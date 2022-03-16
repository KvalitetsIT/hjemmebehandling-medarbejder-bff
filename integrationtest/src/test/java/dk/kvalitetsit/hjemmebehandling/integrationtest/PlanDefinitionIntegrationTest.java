package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.PlanDefinitionApi;
import org.openapitools.client.model.*;

import java.util.List;
import java.util.Optional;

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
        // Arrange

        // Act

        GetPlanDefinitionRequest request = new GetPlanDefinitionRequest();
        request.setStatusesToInclude(List.of());
        ApiResponse<List<PlanDefinitionDto>> response = subject.getPlanDefinitionsWithHttpInfo(request);

        // Assert
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void createPlanDefinition_success() throws Exception {
        // Arrange
//        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();

        QuestionnaireDto questionnaireDto = new QuestionnaireDto();
        questionnaireDto.setId("Questionnaire/questionnaire-1");


        QuestionnaireWrapperDto wrapper = new QuestionnaireWrapperDto();
        wrapper.setQuestionnaire(questionnaireDto);
//        wrapper.setFrequency(frequencyDto);


        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();
        planDefinitionDto.setId("PlanDefinition/plandefinition-1");
        planDefinitionDto.setStatus("ACTIVE");
        planDefinitionDto.setQuestionnaires(List.of(wrapper));


        CreatePlanDefinitionRequest request = new CreatePlanDefinitionRequest()
            .planDefinition(planDefinitionDto);

        // Act
        ApiResponse<Void> response = subject.createPlanDefinitionWithHttpInfo(request);

        // Assert
        assertEquals(201, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey("location"));
    }
}
