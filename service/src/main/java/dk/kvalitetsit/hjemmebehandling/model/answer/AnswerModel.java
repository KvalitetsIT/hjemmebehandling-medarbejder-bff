package dk.kvalitetsit.hjemmebehandling.model.answer;

import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;

public class AnswerModel {
    private String value;
    private AnswerType answerType;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AnswerType getAnswerType() {
        return answerType;
    }

    public void setAnswerType(AnswerType answerType) {
        this.answerType = answerType;
    }
}
