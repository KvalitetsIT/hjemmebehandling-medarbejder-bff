package dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Choice;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer;

import java.util.HashSet;

public class SingleChoice<T extends Answer> extends Choice<T> {

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
    public QuestionDto<?> toDto() {
        var dto = new dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.Choice.SingleChoice<>(this.getText());

        HashSet<?> x = this.getOptions();
        dto.setOptions(x);

        if (this.answer != null) dto.answer(this.answer.toDto());

        return dto;
    }
}
