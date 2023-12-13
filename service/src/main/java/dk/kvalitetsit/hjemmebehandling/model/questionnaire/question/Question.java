package dk.kvalitetsit.hjemmebehandling.model.questionnaire.question;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer;

public class Question<T extends Answer> extends BaseQuestion<T> {

    private T answer;

    public Question(String text) {
        super(text);
    }

    @Override
    public void answer(T answer) {
        this.answer = answer;
    }

    public T getAnswer() {
        return answer;
    }

    @Override
    public QuestionDto<?> toDto() {

        //TODO: must be mapped into the correct QuestionDto
        return new QuestionDto<>(this.getText());

    }
}
