package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public record QuestionnaireModel(
        QualifiedId id,
        String title,
        String description,
        QuestionnaireStatus status,
        List<QuestionModel> questions,
        QuestionModel callToAction,
        String version,
        Date lastUpdated
) {
    public QuestionnaireModel {
        // Ensure lists are never null
        questions = questions != null ? List.copyOf(questions) : List.of();
    }

    public static Builder from(QuestionnaireModel questionnaire) {
        return new Builder(
                questionnaire.id,
                questionnaire.title,
                questionnaire.description,
                questionnaire.status,
                questionnaire.questions,
                questionnaire.callToAction,
                questionnaire.version,
                questionnaire.lastUpdated
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private QualifiedId id;
        private String title;
        private String description;
        private QuestionnaireStatus status;
        private List<QuestionModel> questions = new ArrayList<>();
        private QuestionModel callToAction;
        private String version;
        private Date lastUpdated;

        public Builder(QualifiedId id, String title, String description, QuestionnaireStatus status, List<QuestionModel> questions, QuestionModel callToAction, String version, Date lastUpdated) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.status = status;
            this.questions = questions;
            this.callToAction = callToAction;
            this.version = version;
            this.lastUpdated = lastUpdated;
        }

        public Builder() {
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder status(QuestionnaireStatus status) {
            this.status = status;
            return this;
        }

        public Builder questions(List<QuestionModel> questions) {
            this.questions = questions != null ? new ArrayList<>(questions) : new ArrayList<>();
            return this;
        }

        public Builder callToAction(QuestionModel callToAction) {
            this.callToAction = callToAction;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder lastUpdated(Date lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }


        public Builder id(QualifiedId id) {
            this.id = id;
            return this;
        }

        public QuestionnaireModel build() {
            return new QuestionnaireModel(id, title, description, status, questions, callToAction, version, lastUpdated);
        }
    }
}
