package dk.kvalitetsit.hjemmebehandling.model.question;

import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;

import java.util.List;

public class QuestionModel {
    private String text;
    private boolean required;
    private QuestionType questionType;
    private List<String> options;

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

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
