package dk.kvalitetsit.hjemmebehandling.model;

public class QuestionnaireWrapperModel {
    private QuestionnaireModel questionnaire;
    private FrequencyModel frequency;

    public QuestionnaireModel getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(QuestionnaireModel questionnaire) {
        this.questionnaire = questionnaire;
    }

    public FrequencyModel getFrequency() {
        return frequency;
    }

    public void setFrequency(FrequencyModel frequency) {
        this.frequency = frequency;
    }
}
