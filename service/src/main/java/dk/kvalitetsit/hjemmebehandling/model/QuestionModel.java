package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.model.constants.EnableWhenOperator;
import dk.kvalitetsit.hjemmebehandling.model.constants.QuestionType;

import java.util.ArrayList;
import java.util.List;

public record QuestionModel(
        String linkId,
        String text,
        String abbreviation,
        String helperText,
        boolean required,
        QuestionType questionType,
        MeasurementTypeModel measurementType,
        List<Option> options,
        List<EnableWhen> enableWhens,
        List<ThresholdModel> thresholds,
        List<QuestionModel> subQuestions,
        boolean deprecated
) {

    public static Builder builder() {
        return new Builder();
    }

    public QuestionModel asDeprecated() {
        return new QuestionModel(
                linkId,
                text,
                abbreviation,
                helperText,
                required,
                questionType,
                measurementType,
                options,
                enableWhens,
                thresholds,
                subQuestions,
                true // Mark as deprecated
        );
    }

    public static class Builder {
        private String linkId;
        private String text;
        private String abbreviation;
        private String helperText;
        private boolean required;
        private QuestionType questionType;
        private MeasurementTypeModel measurementType;
        private List<Option> options = new ArrayList<>();
        private List<EnableWhen> enableWhens = new ArrayList<>();
        private List<ThresholdModel> thresholds = new ArrayList<>();
        private List<QuestionModel> subQuestions = new ArrayList<>();
        private boolean deprecated;

        public static Builder from(QuestionModel question) {
            return new Builder()
                    .abbreviation(question.abbreviation)
                    .deprecated(question.deprecated)
                    .questionType(question.questionType)
                    .enableWhens(question.enableWhens)
                    .helperText(question.helperText)
                    .linkId(question.linkId)
                    .thresholds(question.thresholds)
                    .measurementType(question.measurementType)
                    .options(question.options)
                    .required(question.required)
                    .text(question.text)
                    .subQuestions(question.subQuestions);
        }

        public Builder linkId(String linkId) {
            this.linkId = linkId;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder abbreviation(String abbreviation) {
            this.abbreviation = abbreviation;
            return this;
        }

        public Builder helperText(String helperText) {
            this.helperText = helperText;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder questionType(QuestionType questionType) {
            this.questionType = questionType;
            return this;
        }

        public Builder measurementType(MeasurementTypeModel measurementType) {
            this.measurementType = measurementType;
            return this;
        }

        public Builder options(List<Option> options) {
            this.options = options != null ? new ArrayList<>(options) : new ArrayList<>();
            return this;
        }

        public Builder enableWhens(List<EnableWhen> enableWhens) {
            this.enableWhens = enableWhens != null ? new ArrayList<>(enableWhens) : new ArrayList<>();
            return this;
        }

        public Builder thresholds(List<ThresholdModel> thresholds) {
            this.thresholds = thresholds != null ? new ArrayList<>(thresholds) : new ArrayList<>();
            return this;
        }

        public Builder subQuestions(List<QuestionModel> subQuestions) {
            this.subQuestions = subQuestions != null ? new ArrayList<>(subQuestions) : new ArrayList<>();
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public QuestionModel build() {
            return new QuestionModel(
                    linkId, text, abbreviation, helperText, required, questionType, measurementType,
                    options, enableWhens, thresholds, subQuestions, deprecated
            );
        }
    }

    public record EnableWhen(
            AnswerModel answer,
            EnableWhenOperator operator
    ) {
    }
}
