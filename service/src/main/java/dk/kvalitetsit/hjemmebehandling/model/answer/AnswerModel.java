package dk.kvalitetsit.hjemmebehandling.model.answer;

import dk.kvalitetsit.hjemmebehandling.api.answer.AnswerDto;
import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;
import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;

public class AnswerModel implements ToDto<AnswerDto> {
    private String linkId;
    private String value;
    private AnswerType answerType;

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

    @Override
    public AnswerDto toDto() {
        AnswerDto answerDto = new AnswerDto();
        answerDto.setLinkId(this.getLinkId());
        answerDto.setValue(this.getValue());
        answerDto.setAnswerType(this.getAnswerType());
        return answerDto;
    }
}
