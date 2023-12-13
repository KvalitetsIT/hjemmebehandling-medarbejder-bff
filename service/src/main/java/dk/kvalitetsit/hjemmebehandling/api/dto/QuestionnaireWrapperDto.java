package dk.kvalitetsit.hjemmebehandling.api.dto;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.QuestionnaireDto;
import dk.kvalitetsit.hjemmebehandling.mapping.ToModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireWrapperModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionnaireWrapperDto implements ToModel<QuestionnaireWrapperModel> {
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

    @Override
    public QuestionnaireWrapperModel toModel() {
        QuestionnaireWrapperModel questionnaireWrapperModel = new QuestionnaireWrapperModel();

        questionnaireWrapperModel.setQuestionnaire(this.getQuestionnaire().toModel());
        if (this.getFrequency() != null) {
            questionnaireWrapperModel.setFrequency(this.getFrequency().toModel());
        }
        questionnaireWrapperModel.setThresholds( this.getThresholds().stream().map(ThresholdDto::toModel).collect(Collectors.toList()) );

        return questionnaireWrapperModel;
    }
}
