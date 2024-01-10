package dk.kvalitetsit.hjemmebehandling.api.answer;

import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;

import java.util.List;

public class AnswerDto {
    private String linkId;
    private String value;
    private AnswerType answerType;
    private List<AnswerDto> subAnswers;

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

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

    public List<AnswerDto> getSubAnswers() {
        return subAnswers;
    }

    public void setSubAnswers(List<AnswerDto> subAnswers) {
        this.subAnswers = subAnswers;
    }
}
