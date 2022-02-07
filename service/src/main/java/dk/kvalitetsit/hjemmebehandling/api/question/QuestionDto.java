package dk.kvalitetsit.hjemmebehandling.api.question;

import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;

import java.util.List;

public class QuestionDto {
    private String linkId;
    private String text;
    private boolean required;
    private QuestionType questionType;
    private List<String> options;
    private List<QuestionModel.EnableWhen> enableWhen;

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String id) {
        this.linkId = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean getRequired() {
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

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<QuestionModel.EnableWhen> getEnableWhen() {
        return enableWhen;
    }

    public void setEnableWhen(List<QuestionModel.EnableWhen> enableWhen) {
        this.enableWhen = enableWhen;
    }
}
