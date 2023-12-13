package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.Choice;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer;

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
}
