package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;

public class MeasurementDto<T extends Answer<?>> extends BaseQuestionDto<T> {

    private String code;
    private String system;
    private String display;

    private T answer;

    public MeasurementDto(String text) {
        super(text);
    }

    @Override
    public void answer(T answer) {
        this.answer = answer;
    }


    @Override
    public BaseQuestion<?> toModel() {
        return null;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getCode() {
        return code;
    }

    public String getSystem() {
        return system;
    }

    public String getDisplay() {
        return display;
    }
}
