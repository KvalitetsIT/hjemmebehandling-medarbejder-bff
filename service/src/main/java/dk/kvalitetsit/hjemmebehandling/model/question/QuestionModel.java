package dk.kvalitetsit.hjemmebehandling.model.question;

import dk.kvalitetsit.hjemmebehandling.constants.EnableWhenOperator;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;

import java.util.List;

public class QuestionModel {
    private String linkId;
    private String text;
    private String abbreviation;
    private String helperText;
    private boolean required;
    private QuestionType questionType;
    private MeasurementTypeModel measurementType;
    private List<String> options;
    private List<EnableWhen> enableWhens;
    private List<ThresholdModel> thresholds;

    private List<QuestionModel> subQuestions;
    private boolean deprecated;

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getHelperText() {
        return helperText;
    }

    public void setHelperText(String helperText) {
        this.helperText = helperText;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public MeasurementTypeModel getMeasurementType() {
        return measurementType;
    }

    public void setMeasurementType(MeasurementTypeModel measurementType) {
        this.measurementType = measurementType;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<EnableWhen> getEnableWhens() {
        return enableWhens;
    }

    public void setEnableWhens(List<EnableWhen> enableWhens) {
        this.enableWhens = enableWhens;
    }

    public List<ThresholdModel> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<ThresholdModel> thresholds) {
        this.thresholds = thresholds;
    }

    public List<QuestionModel> getSubQuestions() {
        return subQuestions;
    }

    public void setSubQuestions(List<QuestionModel> subQuestions) {
        this.subQuestions = subQuestions;
    }

    public static class EnableWhen {
        private AnswerModel answer; // contains linkId for another question and desired answer[type,value]
        private EnableWhenOperator operator;

        public AnswerModel getAnswer() {
            return answer;
        }

        public void setAnswer(AnswerModel answer) {
            this.answer = answer;
        }

        public EnableWhenOperator getOperator() {
            return operator;
        }

        public void setOperator(EnableWhenOperator operator) {
            this.operator = operator;
        }
    }
}
