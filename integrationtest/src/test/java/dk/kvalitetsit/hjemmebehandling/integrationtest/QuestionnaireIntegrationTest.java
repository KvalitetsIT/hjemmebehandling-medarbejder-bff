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
    public void getQuestionnaire_byId_success() throws Exception {
        var questionnaire = questionnaireApi.getQuestionnaireById("questionnaire-1");
        assertNotNull(questionnaire);
    }

    @Test
    public void createQuestionnaire_success() throws Exception {
        // Arrange
        QuestionnaireDto questionnaireDto = new QuestionnaireDto();

        QuestionDto question1 = new QuestionDto();
        question1.setLinkId("1");
        question1.setText("Har du sovet godt? (æøå)");
        question1.setQuestionType(QuestionDto.QuestionTypeEnum.BOOLEAN);



        QuestionDto question2 = new QuestionDto();
        question2.setLinkId("2");
        question2.setText("Sov du heller ikke godt i går?");
        question2.setQuestionType(QuestionDto.QuestionTypeEnum.BOOLEAN);

        AnswerModel answer = new AnswerModel();
        answer.setLinkId(question1.getLinkId());
        answer.setAnswerType(AnswerModel.AnswerTypeEnum.BOOLEAN);
        answer.setValue(Boolean.FALSE.toString());

        EnableWhen enableWhen = new EnableWhen();
        enableWhen.setAnswer(answer);
        enableWhen.setOperator(EnableWhen.OperatorEnum.EQUAL);
        question2.addEnableWhenItem(enableWhen);

        questionnaireDto.setQuestions( List.of(question1, question2) );
        questionnaireDto.setStatus("DRAFT");

        CreateQuestionnaireRequest request = new CreateQuestionnaireRequest();
        request.setQuestionnaire(questionnaireDto);

        // Act
        ApiResponse<Void> response = questionnaireApi.createQuestionnaireWithHttpInfo(request);

        // Assert
        assertEquals(201, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey("location"));
    }

    @Test
    public void patchQuestionnaire_success() throws Exception {
        // Arrange
        String id = "questionnaire-1";
        QuestionnaireDto questionnaire = questionnaireApi.getQuestionnaireById(id);
        questionnaire.setTitle("Ny forbedret titel");

        PatchQuestionnaireRequest request = new PatchQuestionnaireRequest();
        request.setTitle("Ny forbedret titel");
        request.status(questionnaire.getStatus());
        request.setDescription(request.getDescription());
        request.setQuestions(questionnaire.getQuestions());
        request.setCallToActions(questionnaire.getCallToActions());


        // Act
        ApiResponse<Void> response = questionnaireApi.patchQuestionnaireWithHttpInfo(id, request);

        // Assert
        assertEquals(200, response.getStatusCode());
    }
}
