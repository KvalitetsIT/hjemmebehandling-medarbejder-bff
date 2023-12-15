package dk.kvalitetsit.hjemmebehandling.model.questionnaire.question;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.BaseQuestionDto;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.MeasurementDto;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Number;

public class Measurement<T extends Answer<?>> extends BaseQuestion<T> {

    private String system;
    private String code;
    private String display;
    private T answer;

    public Measurement(String text) {
        super(text);
    }


    @Override
    public BaseQuestionDto<?> toDto() {
        MeasurementDto<?> dto = new MeasurementDto<T>(this.getText());
        dto.setSystem(this.system);
        dto.setCode(this.system);
        dto.setDisplay(this.display);
        dto.answer(this.getAnswer().toDto());

        decorateDto(dto);
        return dto;
    }

    @Override
    public void answer(T answer) {

    }

    @Override
    public T getAnswer() {
        return this.answer;
    }
}
