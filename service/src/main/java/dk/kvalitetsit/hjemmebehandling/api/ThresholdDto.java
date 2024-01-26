package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;


public class ThresholdDto {
    private String questionId;
    private ThresholdType type;
    private Boolean valueBoolean;
    private Double valueQuantityLow;
    private Double valueQuantityHigh;
    private String ValueOption;

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

    public String getValueOption() {
        return ValueOption;
    }

    public void setValueOption(String valueOption) {
        ValueOption = valueOption;
    }
}