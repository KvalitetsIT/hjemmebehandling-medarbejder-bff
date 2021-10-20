package dk.kvalitetsit.hjemmebehandling.api;

public class QuestionnaireWrapperDto {
    private QuestionnaireDto questionnaire;
    private FrequencyDto frequency;

    public QuestionnaireDto getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(QuestionnaireDto questionnaire) {
        this.questionnaire = questionnaire;
    }

    public FrequencyDto getFrequency() {
        return frequency;
    }

    public void setFrequency(FrequencyDto frequency) {
        this.frequency = frequency;
    }
}
