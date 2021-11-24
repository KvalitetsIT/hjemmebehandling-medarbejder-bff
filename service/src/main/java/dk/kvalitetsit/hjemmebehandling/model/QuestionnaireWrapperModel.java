package dk.kvalitetsit.hjemmebehandling.model;

import java.time.Instant;

public class QuestionnaireWrapperModel {
    private QuestionnaireModel questionnaire;
    private FrequencyModel frequency;
    private Instant satisfiedUntil;

    public QuestionnaireWrapperModel() {

    }

    public QuestionnaireWrapperModel(QuestionnaireModel questionnaire, FrequencyModel frequency, Instant satisfiedUntil) {
        this.questionnaire = questionnaire;
        this.frequency = frequency;
        this.satisfiedUntil = satisfiedUntil;
    }

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

    public Instant getSatisfiedUntil() {
        return satisfiedUntil;
    }

    public void setSatisfiedUntil(Instant satisfiedUntil) {
        this.satisfiedUntil = satisfiedUntil;
    }
}
