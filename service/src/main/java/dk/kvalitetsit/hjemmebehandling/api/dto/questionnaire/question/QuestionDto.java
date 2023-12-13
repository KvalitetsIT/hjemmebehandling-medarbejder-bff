package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Question;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;

public class QuestionDto<T extends Answer> extends BaseQuestionDto<T> {

    private T answer;

    public QuestionDto(String text) {
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
    public BaseQuestion<?> toModel() {


        Question<?> questionModel = new Question<>(this.getText());


        questionModel.answer(this.getAnswer());

        return questionModel;
    }
}
