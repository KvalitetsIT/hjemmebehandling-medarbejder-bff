package dk.kvalitetsit.hjemmebehandling.api.request;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.QuestionnaireDto;
import io.swagger.v3.oas.annotations.media.Schema;

public class CreateQuestionnaireRequest {
    private QuestionnaireDto questionnaire;

    @Schema(required = true, description = "The questionnaire to create.")
    public QuestionnaireDto getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(QuestionnaireDto questionnaire) {
        this.questionnaire = questionnaire;
    }
}
