package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiException;
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
    public void getQuestionnaireById_NotFound() throws Exception {
        try{
            var questionnaire = questionnaireApi.getQuestionnaireById("questionnaire/doesNotExist");
            fail("Method should throw 404");
        } catch(ApiException exception) {
            assertEquals(404, exception.getCode());
        }
    }

}
