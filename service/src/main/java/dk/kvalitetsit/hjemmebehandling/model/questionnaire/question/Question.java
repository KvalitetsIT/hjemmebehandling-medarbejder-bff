package dk.kvalitetsit.hjemmebehandling.model.questionnaire.question;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.BaseQuestionDto;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer;

public class Question<T extends Answer<?>> extends BaseQuestion<T> {

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
    public BaseQuestionDto<? extends dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer> toDto() {

        var dto = new QuestionDto<>(this.getText());

        decorateDto(dto);

        if (this.answer != null) dto.answer(this.answer.toDto());

        return dto;
    }
}
