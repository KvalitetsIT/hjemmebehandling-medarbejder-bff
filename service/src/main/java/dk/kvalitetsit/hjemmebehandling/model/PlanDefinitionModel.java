package dk.kvalitetsit.hjemmebehandling.model;


import java.time.Instant;
import java.util.List;

public record PlanDefinitionModel(
        String name,
        String title,
        PlanDefinitionStatus status,
        Instant created,
        Instant lastUpdated,
        List<QuestionnaireWrapperModel> questionnaires

) implements BaseModel {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private QualifiedId id;
        private String name;
        private String title;
        private PlanDefinitionStatus status;
        private Instant created;
        private Instant lastUpdated;
        private List<QuestionnaireWrapperModel> questionnaires;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(PlanDefinitionStatus status) {
            this.status = status;
            return this;
        }

        public Builder created(Instant created) {
            this.created = created;
            return this;
        }

        public Builder lastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder questionnaires(List<QuestionnaireWrapperModel> questionnaires) {
            this.questionnaires = questionnaires;
            return this;
        }


        public Builder id(QualifiedId id) {
            this.id = id;
            return this;
        }

        public PlanDefinitionModel build() {
            return new PlanDefinitionModel(name, title, status, created, lastUpdated, questionnaires);
        }
    }


    @Override
    public QualifiedId id() {
        return null;
    }

    @Override
    public String organizationId() {
        return "";
    }
}
