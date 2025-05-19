package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;

public record ThresholdModel (
        String questionnaireItemLinkId, // Might be set to a "Qualified.QuestionId"
        ThresholdType type,
        Double valueQuantityLow,
        Double valueQuantityHigh,
        Boolean valueBoolean,
        String valueOption
) {

    public static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private String questionnaireItemLinkId;
        private ThresholdType type;
        private Double valueQuantityLow;
        private Double valueQuantityHigh;
        private Boolean valueBoolean;
        private String valueOption;

        public static Builder from(ThresholdModel threshold) {
            return new Builder(
                    threshold.questionnaireItemLinkId,
                    threshold.type,
                    threshold.valueQuantityLow,
                    threshold.valueQuantityHigh,
                    threshold.valueBoolean,
                    threshold.valueOption
            );
        }

        private Builder() {
        }

        private Builder(String questionnaireItemLinkId, ThresholdType type, Double valueQuantityLow, Double valueQuantityHigh, Boolean valueBoolean, String valueOption) {
            this.questionnaireItemLinkId = questionnaireItemLinkId;
            this.type = type;
            this.valueQuantityLow = valueQuantityLow;
            this.valueQuantityHigh = valueQuantityHigh;
            this.valueBoolean = valueBoolean;
            this.valueOption = valueOption;
        }

        public Builder setQuestionnaireItemLinkId(String questionnaireItemLinkId) {
            this.questionnaireItemLinkId = questionnaireItemLinkId;
            return this;
        }

        public Builder setType(ThresholdType type) {
            this.type = type;
            return this;
        }

        public Builder setValueQuantityLow(Double valueQuantityLow) {
            this.valueQuantityLow = valueQuantityLow;
            return this;
        }

        public Builder setValueQuantityHigh(Double valueQuantityHigh) {
            this.valueQuantityHigh = valueQuantityHigh;
            return this;
        }

        public Builder setValueBoolean(Boolean valueBoolean) {
            this.valueBoolean = valueBoolean;
            return this;
        }

        public Builder setValueOption(String valueOption) {
            this.valueOption = valueOption;
            return this;
        }

        public ThresholdModel build(){
            return new ThresholdModel(this.questionnaireItemLinkId, this.type, this.valueQuantityLow, this.valueQuantityHigh, this.valueBoolean, this.valueOption);
        }
    }


}
