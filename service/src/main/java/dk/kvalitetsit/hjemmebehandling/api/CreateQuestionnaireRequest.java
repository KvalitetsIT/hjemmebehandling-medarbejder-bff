package dk.kvalitetsit.hjemmebehandling.api;

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
