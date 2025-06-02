package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.model.constants.AnswerType;

import java.util.List;

public record AnswerModel(
        String linkId,
        String value,
        AnswerType answerType,
        List<AnswerModel> subAnswers) {

    public static AnswerModel.Builder builder() {
        return new AnswerModel.Builder();
    }

    public static class Builder {

        private String linkId;
        private String value;
        private AnswerType answerType;
        private List<AnswerModel> subAnswers;

        public void linkId(String linkId) {
            this.linkId = linkId;
        }

        public void value(String value) {
            this.value = value;
        }

        public void answerType(AnswerType answerType) {
            this.answerType = answerType;
        }

        public void subAnswers(List<AnswerModel> subAnswers) {
            this.subAnswers = subAnswers;
        }

        public AnswerModel build() {
            return new AnswerModel(this.linkId, this.value, this.answerType, this.subAnswers);
        }
    }

}


