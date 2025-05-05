package dk.kvalitetsit.hjemmebehandling.model;


import java.time.Instant;
import java.util.List;

public record PlanDefinitionModel(
        QualifiedId.PlanDefinitionId id,
        QualifiedId.OrganizationId organizationId,
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
    public QualifiedId.PlanDefinitionId id() {
        return id;
    }

    @Override
    public QualifiedId.OrganizationId organizationId() {
        return organizationId;
    }

    public static class Builder {

        private QualifiedId.PlanDefinitionId id;
        private String name;
        private String title;
        private PlanDefinitionStatus status;
        private Instant created;
        private Instant lastUpdated;
        private List<QuestionnaireWrapperModel> questionnaires;
        private QualifiedId.OrganizationId organizationId;

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


        public Builder id(QualifiedId.PlanDefinitionId id) {
            this.id = id;
            return this;
        }

        public Builder organizationId(QualifiedId.OrganizationId organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public PlanDefinitionModel build() {
            return new PlanDefinitionModel(id, organizationId, name, title, status, created, lastUpdated, questionnaires);
        }

    }

}
