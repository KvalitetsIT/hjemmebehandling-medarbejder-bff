package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.Choice;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Question;

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
    public BaseQuestion<?> toModel() {

            var model = new dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Choice.SingleChoice<>(this.getText());

            model.setOptions((HashSet<dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer>) this.getOptions().stream().map(x -> x.toModel()).collect(Collectors.toList()));

            // Todo: Implement the rest of the conversion

            return  model;

    }
}
