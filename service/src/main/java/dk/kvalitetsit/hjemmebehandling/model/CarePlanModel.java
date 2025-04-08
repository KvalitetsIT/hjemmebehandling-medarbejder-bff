package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record CarePlanModel(
        QualifiedId id,
        String organizationId,
        String title,
        CarePlanStatus status,
        Instant created,
        Instant startDate,
        Instant endDate,
        PatientModel patient,
        List<QuestionnaireWrapperModel> questionnaires,
        List<PlanDefinitionModel> planDefinitions,
        String departmentName,
        Instant satisfiedUntil
) implements BaseModel {
    public CarePlanModel {
        // Ensure lists are never null
        questionnaires = (questionnaires != null) ? List.copyOf(questionnaires) : List.of();
        planDefinitions = (planDefinitions != null) ? List.copyOf(planDefinitions) : List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private QualifiedId id;
        private String organizationId;
        private String title;
        private CarePlanStatus status;
        private Instant created;
        private Instant startDate;
        private Instant endDate;
        private PatientModel patient;
        private List<QuestionnaireWrapperModel> questionnaires = new ArrayList<>();
        private List<PlanDefinitionModel> planDefinitions = new ArrayList<>();
        private String departmentName;
        private Instant satisfiedUntil;

        public static Builder from(CarePlanModel model) {
            return new Builder()
                    .id(model.id)
                    .organizationId(model.organizationId)
                    .title(model.title)
                    .status(model.status)
                    .created(model.created)
                    .startDate(model.startDate)
                    .endDate(model.endDate)
                    .patient(model.patient)
                    .questionnaires(model.questionnaires)
                    .planDefinitions(model.planDefinitions)
                    .departmentName(model.departmentName)
                    .satisfiedUntil(model.satisfiedUntil);

        }

        public Builder id(QualifiedId id) {
            this.id = id;
            return this;
        }

        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(CarePlanStatus status) {
            this.status = status;
            return this;
        }

        public Builder created(Instant created) {
            this.created = created;
            return this;
        }

        public Builder startDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(Instant endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder patient(PatientModel patient) {
            this.patient = patient;
            return this;
        }

        public Builder questionnaires(List<QuestionnaireWrapperModel> questionnaires) {
            this.questionnaires = (questionnaires != null) ? new ArrayList<>(questionnaires) : new ArrayList<>();
            return this;
        }

        public Builder planDefinitions(List<PlanDefinitionModel> planDefinitions) {
            this.planDefinitions = (planDefinitions != null) ? new ArrayList<>(planDefinitions) : new ArrayList<>();
            return this;
        }

        public Builder departmentName(String departmentName) {
            this.departmentName = departmentName;
            return this;
        }

        public Builder satisfiedUntil(Instant satisfiedUntil) {
            this.satisfiedUntil = satisfiedUntil;
            return this;
        }

        public CarePlanModel build() {
            return new CarePlanModel(id, organizationId, title, status, created, startDate, endDate, patient,
                    questionnaires, planDefinitions, departmentName, satisfiedUntil);
        }
    }
}
