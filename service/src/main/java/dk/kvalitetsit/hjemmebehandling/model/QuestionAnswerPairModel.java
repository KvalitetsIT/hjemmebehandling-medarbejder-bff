package dk.kvalitetsit.hjemmebehandling.model;

public class QuestionAnswerPairModel {
    private QuestionModel question;
    private AnswerModel answer;


    public QuestionAnswerPairModel(QuestionModel question, AnswerModel answer) {
        this.question = question;
        this.answer = answer;
    }

    public QuestionModel getQuestion() {
        return question;
    }

    public void setQuestion(QuestionModel question) {
        this.question = question;
    }

    public AnswerModel getAnswer() {
        return answer;
    }

    public void setAnswer(AnswerModel answer) {
        this.answer = answer;
    }


}
