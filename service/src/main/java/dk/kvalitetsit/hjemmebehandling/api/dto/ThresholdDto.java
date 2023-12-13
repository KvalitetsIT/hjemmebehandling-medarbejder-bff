package dk.kvalitetsit.hjemmebehandling.api.dto;

import dk.kvalitetsit.hjemmebehandling.mapping.ToModel;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;


public class ThresholdDto implements ToModel<ThresholdModel> {
    private String questionId;
    private ThresholdType type;
    private Boolean valueBoolean;
    private Double valueQuantityLow;
    private Double valueQuantityHigh;

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public ThresholdType getType() {
        return type;
    }

    public void setType(ThresholdType type) {
        this.type = type;
    }

    public Boolean getValueBoolean() {
        return valueBoolean;
    }

    public void setValueBoolean(Boolean valueBoolean) {
        this.valueBoolean = valueBoolean;
    }

    public Double getValueQuantityLow() {
        return valueQuantityLow;
    }

    public void setValueQuantityLow(Double valueQuantityLow) {
        this.valueQuantityLow = valueQuantityLow;
    }

    public Double getValueQuantityHigh() {
        return valueQuantityHigh;
    }

    public void setValueQuantityHigh(Double valueQuantityHigh) {
        this.valueQuantityHigh = valueQuantityHigh;
    }

    @Override
    public ThresholdModel toModel() {
        ThresholdModel thresholdModel = new ThresholdModel();

        thresholdModel.setQuestionnaireItemLinkId(this.getQuestionId());
        thresholdModel.setType(this.getType());
        thresholdModel.setValueBoolean(this.getValueBoolean());
        thresholdModel.setValueQuantityLow(this.getValueQuantityLow());
        thresholdModel.setValueQuantityHigh(this.getValueQuantityHigh());

        return thresholdModel;
    }
}