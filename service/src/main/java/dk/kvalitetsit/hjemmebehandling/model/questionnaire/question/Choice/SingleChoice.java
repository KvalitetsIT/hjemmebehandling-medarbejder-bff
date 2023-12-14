package dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Choice;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.BaseQuestionDto;
import dk.kvalitetsit.hjemmebehandling.mapping.Model;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer;

import java.util.HashSet;

public class SingleChoice<T extends Answer<?>> extends Choice<T> {

    private T answer;

    public SingleChoice(String text) {
        super(text);
    }

    public void answer(T answer) {
        if (!this.getOptions().contains(answer)){
            throw new IllegalArgumentException("The answer is invalid. It does not match the given options");
        }
        this.answer = answer;
    }

    public T getAnswer() {
        return answer;
    }

    public void setAnswer(T answer) {
        this.answer = answer;
    }

    @Override
    public BaseQuestionDto<?> toDto() {

        var dto = new dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.Choice.SingleChoice<>(this.getText());

        decorateDto(dto);

        var options = this.getOptions().stream().map(Model::toDto).toList();

        dto.setOptions(new HashSet<>(options));

        if (this.answer != null) dto.answer(this.answer.toDto());

        return dto;
    }
}
