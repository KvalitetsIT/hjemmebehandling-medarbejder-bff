package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.CarePlanApi;
import org.openapitools.client.api.QuestionnaireApi;
import org.openapitools.client.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QuestionnaireIntegrationTest extends AbstractIntegrationTest {
    private QuestionnaireApi questionnaireApi;

    @BeforeEach
    public void setup() {
        questionnaireApi = new QuestionnaireApi();
        questionnaireApi.getApiClient().setBasePath(enhanceBasePath(questionnaireApi.getApiClient().getBasePath()));
    }

    @Test
    public void getQuestionnaire_byId_success() throws Exception {
        var questionnaire = questionnaireApi.getQuestionnaireById("questionnaire-1");
        assertNotNull(questionnaire);
    }

}
