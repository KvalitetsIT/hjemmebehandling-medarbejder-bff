package dk.kvalitetsit.hjemmebehandling.model;

import java.time.Instant;
import java.util.List;

public record QuestionnaireWrapperModel(
        QuestionnaireModel questionnaire,
        FrequencyModel frequency,
        Instant satisfiedUntil,
        List<ThresholdModel> thresholds
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private QuestionnaireModel questionnaire;
        private FrequencyModel frequency;
        private Instant satisfiedUntil;
        private List<ThresholdModel> thresholds;

        public Builder questionnaire(QuestionnaireModel questionnaire) {
            this.questionnaire = questionnaire;
            return this;
        }

        public Builder frequency(FrequencyModel frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder satisfiedUntil(Instant satisfiedUntil) {
            this.satisfiedUntil = satisfiedUntil;
            return this;
        }

        public Builder thresholds(List<ThresholdModel> thresholds) {
            this.thresholds = thresholds;
            return this;
        }

        public QuestionnaireWrapperModel build() {
            return new QuestionnaireWrapperModel(questionnaire, frequency, satisfiedUntil, thresholds);
        }
    }
}
