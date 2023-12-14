package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.Choice;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.mapping.Dto;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;

import java.util.HashSet;
import java.util.stream.Collectors;

public class SingleChoice<T extends Answer> extends Choice<T> {

    private T answer;

    public SingleChoice(String text) {
        super(text);
    }

    @Override
    public void answer(T answer) {
        if (!this.getOptions().contains(answer)){
            throw INVALID_ANSWER_EXCEPTION;
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
    public BaseQuestion<?> toModel() {

            var model = new dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Choice.SingleChoice<>(this.getText());

            decorateModel(model);

            model.setOptions(new HashSet<>(this.getOptions().stream().map(Dto::toModel).collect(Collectors.toList())));

            return  model;

    }

}
