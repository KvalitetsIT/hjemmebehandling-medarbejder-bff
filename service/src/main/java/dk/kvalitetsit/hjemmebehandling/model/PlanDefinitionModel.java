package dk.kvalitetsit.hjemmebehandling.model;


import dk.kvalitetsit.hjemmebehandling.model.constants.Status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record PlanDefinitionModel(
        QualifiedId.PlanDefinitionId id,
        QualifiedId.OrganizationId organizationId,
        String name,
        String title,
        Status status,
        Instant created,
        Instant lastUpdated,
        List<QuestionnaireWrapperModel> questionnaires

) implements BaseModel<PlanDefinitionModel> {

    public PlanDefinitionModel {
        // Ensure lists are never null
        questionnaires = questionnaires != null ? questionnaires : List.of();
    }


    @Override
    public QualifiedId.PlanDefinitionId id() {
        return id;
    }

    @Override
    public QualifiedId.OrganizationId organizationId() {
        return organizationId;
    }

    @Override
    public PlanDefinitionModel substitute(PlanDefinitionModel other) {
        return new PlanDefinitionModel(
                Optional.ofNullable(other.id()).orElse(this.id()),
                Optional.ofNullable(other.organizationId()).orElse(this.organizationId()),
                Optional.ofNullable(other.name()).orElse(this.name()),
                Optional.ofNullable(other.title()).orElse(this.title()),
                Optional.ofNullable(other.status()).orElse(this.status()),
                Optional.ofNullable(other.created()).orElse(this.created()),
                Optional.ofNullable(other.lastUpdated()).orElse(this.lastUpdated()),
                Optional.ofNullable(other.questionnaires()).orElse(this.questionnaires())
        );
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder  {

        private QualifiedId.PlanDefinitionId id;
        private String name;
        private String title;
        private Status status;
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

        public Builder status(Status status) {
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
