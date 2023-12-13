//package dk.kvalitetsit.hjemmebehandling.api.dto;
//
//import dk.kvalitetsit.hjemmebehandling.api.answer.AnswerDto;
//import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer;
//import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.QuestionDto;
//
//
//public class QuestionAnswerPairDto implements ToModel<QuestionAnswerPairModel> {
//    private QuestionDto<? extends Answer> question;
//    private AnswerDto answer;
//
//
//    public QuestionDto<? extends Answer> getQuestion() {
//        return question;
//    }
//
//    public void setQuestion(QuestionDto<? extends Answer> question) {
//        this.question = question;
//    }
//
//    public AnswerDto getAnswer() {
//        return answer;
//    }
//
//    public void setAnswer(AnswerDto answer) {
//        this.answer = answer;
//    }
//
//
//    @Override
//    public QuestionAnswerPairModel toModel() {
//        return null;
//    }
//}
