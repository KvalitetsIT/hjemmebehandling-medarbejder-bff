package dk.kvalitetsit.hjemmebehandling.model;

public record QuestionAnswerPairModel(
        QuestionModel question,
        AnswerModel answer
) {
    public static QuestionAnswerPairModel.Builder builder() {
        return new QuestionAnswerPairModel.Builder();
    }

    public static class Builder {
        private QuestionModel question;
        private AnswerModel answer;

        public Builder question(QuestionModel question) {
            this.question = question;
            return this;
        }

        public Builder answer(AnswerModel answer) {
            this.answer = answer;
            return this;
        }


        public QuestionAnswerPairModel build() {
            return new QuestionAnswerPairModel(this.question, this.answer);
        }
    }
}
