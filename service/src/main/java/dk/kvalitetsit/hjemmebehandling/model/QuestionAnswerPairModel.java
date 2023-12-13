package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.api.dto.QuestionAnswerPairDto;
import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;


public class QuestionAnswerPairModel implements ToDto<QuestionAnswerPairDto> {
    private BaseQuestion<?> question;
    private AnswerModel answer;



    public QuestionAnswerPairModel(BaseQuestion<?> question, AnswerModel answer) {
        this.question = question;
        this.answer = answer;
    }

    public BaseQuestion<?> getQuestion() {
        return question;
    }

    public void setQuestion(BaseQuestion<?> question) {
        this.question = question;
    }

    public AnswerModel getAnswer() {
        return answer;
    }

    public void setAnswer(AnswerModel answer) {
        this.answer = answer;
    }


    @Override
    public QuestionAnswerPairDto toDto() {
        return null;
    }
}
