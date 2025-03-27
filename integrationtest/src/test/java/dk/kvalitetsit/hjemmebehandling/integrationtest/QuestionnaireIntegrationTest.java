package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.QuestionnaireApi;
import org.openapitools.client.model.*;

import java.util.List;
import java.util.Objects;

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
        QuestionDto question1 = new QuestionDto()
                .linkId("1")
                .text("Har du sovet godt? (æøå)")
                .questionType(QuestionDto.QuestionTypeEnum.BOOLEAN);

        QuestionDto question2 = new QuestionDto()
                .linkId("2")
                .text("Sov du heller ikke godt i går?")
                .questionType(QuestionDto.QuestionTypeEnum.BOOLEAN);

        question2.addEnableWhenItem(new EnableWhen()
                .answer(new AnswerDto()
                        .linkId(question1.getLinkId())
                        .answerType(AnswerDto.AnswerTypeEnum.BOOLEAN)
                        .value(Boolean.FALSE.toString())
                ).operator(EnableWhen.OperatorEnum.EQUAL));

        QuestionnaireDto questionnaireDto = new QuestionnaireDto()
                .questions(List.of(question1, question2))
                .status("DRAFT");

        CreateQuestionnaireRequest request = new CreateQuestionnaireRequest()
                .questionnaire(questionnaireDto);

        ApiResponse<Void> response = questionnaireApi.createQuestionnaireWithHttpInfo(request);

        assertEquals(201, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey("location"));
    }

    @Test
    public void patchQuestionnaire_success() throws Exception {
        String id = "questionnaire-1";

        QuestionnaireDto questionnaire = questionnaireApi.getQuestionnaireById(id)
                .title("Ny forbedret titel");

        PatchQuestionnaireRequest request = new PatchQuestionnaireRequest()
                .title("Ny forbedret titel")
                .status(Objects.requireNonNull(questionnaire.getStatus()))
                .description("ny forbedret description")
                .questions(questionnaire.getQuestions())
                .callToAction(Objects.requireNonNull(questionnaire.getCallToAction()));

        ApiResponse<Void> response = questionnaireApi.patchQuestionnaireWithHttpInfo(id, request);

        assertEquals(200, response.getStatusCode());
    }
}
