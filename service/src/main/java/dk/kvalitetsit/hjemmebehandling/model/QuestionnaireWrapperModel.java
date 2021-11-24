package dk.kvalitetsit.hjemmebehandling.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class QuestionnaireWrapperModel {
    private QuestionnaireModel questionnaire;
    private FrequencyModel frequency;
    private Instant satisfiedUntil;
    private List<ThresholdModel> thresholds;

    public QuestionnaireWrapperModel() {
        thresholds = new ArrayList<>();
    }

    public QuestionnaireWrapperModel(QuestionnaireModel questionnaire, FrequencyModel frequency, Instant satisfiedUntil, List<ThresholdModel> thresholds) {
        this();
        this.questionnaire = questionnaire;
        this.frequency = frequency;
        this.satisfiedUntil = satisfiedUntil;
        this.questionnaire = questionnaire;
        this.frequency = frequency;
        this.thresholds = thresholds;
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

    public List<ThresholdModel> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<ThresholdModel> thresholds) {
        this.thresholds = thresholds;
    }
}
