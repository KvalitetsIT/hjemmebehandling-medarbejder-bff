package dk.kvalitetsit.hjemmebehandling.model.question;

import dk.kvalitetsit.hjemmebehandling.constants.EnableWhenOperator;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;

import java.util.List;

public class QuestionModel {
    private String linkId;
    private String text;
    private boolean required;
    private QuestionType questionType;
    private MeasurementTypeModel code;
    private List<String> options;
    private List<EnableWhen> enableWhens;

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

    public MeasurementTypeModel getCode() {
        return code;
    }

    public void setCode(MeasurementTypeModel code) {
        this.code = code;
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
