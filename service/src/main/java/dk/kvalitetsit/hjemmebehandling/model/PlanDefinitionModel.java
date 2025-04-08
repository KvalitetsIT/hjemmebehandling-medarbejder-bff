package dk.kvalitetsit.hjemmebehandling.model;


import java.time.Instant;
import java.util.List;

public record PlanDefinitionModel(
        QualifiedId id,
        String organizationId,
        String name,
        String title,
        PlanDefinitionStatus status,
        Instant created,
        Instant lastUpdated,
        List<QuestionnaireWrapperModel> questionnaires

) implements BaseModel {

    public PlanDefinitionModel {
        // Ensure lists are never null
        questionnaires = questionnaires != null ? questionnaires : List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public QualifiedId id() {
        return id;
    }

    @Override
    public String organizationId() {
        return organizationId;
    }

    public static class Builder {

        private QualifiedId id;
        private String name;
        private String title;
        private PlanDefinitionStatus status;
        private Instant created;
        private Instant lastUpdated;
        private List<QuestionnaireWrapperModel> questionnaires;
        private String organizationId;

        public static Builder from(PlanDefinitionModel model) {
            return new Builder()
                    .name(model.name)
                    .title(model.title)
                    .status(model.status)
                    .created(model.created)
                    .lastUpdated(model.lastUpdated)
                    .questionnaires(model.questionnaires)
                    .id(model.id)
                    .organizationId(model.organizationId);
        }

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

        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public PlanDefinitionModel build() {
            return new PlanDefinitionModel(id, organizationId, name, title, status, created, lastUpdated, questionnaires);
        }

    }

}
