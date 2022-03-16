package dk.kvalitetsit.hjemmebehandling.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class QuestionnaireWrapperDto {
    private QuestionnaireDto questionnaire;
    private FrequencyDto frequency;
    private Instant satisfiedUntil;
    private List<ThresholdDto> thresholds;

    public QuestionnaireWrapperDto() {
        thresholds = new ArrayList<>();
    }

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

    public Instant getSatisfiedUntil() {
        return satisfiedUntil;
    }

    public void setSatisfiedUntil(Instant satisfiedUntil) {
        this.satisfiedUntil = satisfiedUntil;
    }

    public List<ThresholdDto> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<ThresholdDto> thresholds) {
        this.thresholds = thresholds;
    }
}
