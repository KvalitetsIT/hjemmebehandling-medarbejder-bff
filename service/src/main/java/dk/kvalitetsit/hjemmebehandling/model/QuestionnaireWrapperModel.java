package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.api.dto.QuestionnaireWrapperDto;
import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.QuestionnaireModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionnaireWrapperModel implements ToDto<QuestionnaireWrapperDto> {
    private QuestionnaireModel questionnaire;
    private FrequencyModel frequency;
    private Instant satisfiedUntil;
    private List<ThresholdModel> thresholds;

    public QuestionnaireWrapperModel() {
        thresholds = new ArrayList<>();
    }

    public QuestionnaireWrapperModel(QuestionnaireModel questionnaire, FrequencyModel frequency, Instant satisfiedUntil, List<ThresholdModel> thresholds) {
        this();
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

    @Override
    public QuestionnaireWrapperDto toDto() {
        QuestionnaireWrapperDto questionnaireWrapperDto = new QuestionnaireWrapperDto();

        questionnaireWrapperDto.setQuestionnaire(this.getQuestionnaire().toDto());
        if (this.getFrequency() != null) {
            questionnaireWrapperDto.setFrequency(this.getFrequency().toDto());
        }

        questionnaireWrapperDto.setSatisfiedUntil(this.getSatisfiedUntil());
        questionnaireWrapperDto.setThresholds( this.getThresholds().stream().map(ThresholdModel::toDto).collect(Collectors.toList()) );

        return questionnaireWrapperDto;
    }
}
