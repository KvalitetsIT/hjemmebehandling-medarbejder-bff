package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.model.constants.TriagingCategory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record QuestionnaireResponseModel(
        QualifiedId.QuestionnaireResponseId id,
        QualifiedId.OrganizationId organizationId,
        QualifiedId.QuestionnaireId questionnaireId,
        QualifiedId.CarePlanId carePlanId,
        QualifiedId.PractitionerId authorId,
        QualifiedId.QuestionnaireId sourceId,
        String questionnaireName,
        List<QuestionAnswerPairModel> questionAnswerPairs,
        Instant answered,
        ExaminationStatus examinationStatus,
        PractitionerModel examinationAuthor,
        TriagingCategory triagingCategory,
        PatientModel patient,
        String planDefinitionTitle

) implements BaseModel<QuestionnaireResponseModel> {

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public QualifiedId.OrganizationId organizationId() {
        return organizationId;
    }

    @Override
    public QuestionnaireResponseModel substitute(QuestionnaireResponseModel other) {
        return new QuestionnaireResponseModel(
                Optional.ofNullable(other.id).orElse(id),
                Optional.ofNullable(other.organizationId).orElse(organizationId),
                Optional.ofNullable(other.questionnaireId).orElse(questionnaireId),
                Optional.ofNullable(other.carePlanId).orElse(carePlanId),
                Optional.ofNullable(other.authorId).orElse(authorId),
                Optional.ofNullable(other.sourceId).orElse(sourceId),
                Optional.ofNullable(other.questionnaireName).orElse(questionnaireName),
                Optional.ofNullable(other.questionAnswerPairs).orElse(questionAnswerPairs),
                Optional.ofNullable(other.answered).orElse(answered),
                Optional.ofNullable(other.examinationStatus).orElse(examinationStatus),
                Optional.ofNullable(other.examinationAuthor).orElse(examinationAuthor),
                Optional.ofNullable(other.triagingCategory).orElse(triagingCategory),
                Optional.ofNullable(other.patient).orElse(patient),
                Optional.ofNullable(other.planDefinitionTitle).orElse(planDefinitionTitle)
        );
    }


    public static class Builder {
        private QualifiedId.QuestionnaireResponseId id;
        private QualifiedId.QuestionnaireId questionnaireId;
        private QualifiedId.CarePlanId carePlanId;
        private QualifiedId.PractitionerId authorId;
        private QualifiedId.QuestionnaireId sourceId;
        private String questionnaireName;
        private List<QuestionAnswerPairModel> questionAnswerPairs;
        private Instant answered;
        private ExaminationStatus examinationStatus;
        private PractitionerModel examinationAuthor;
        private TriagingCategory triagingCategory;
        private PatientModel patient;
        private String planDefinitionTitle;
        private QualifiedId.OrganizationId organizationId;

        public static Builder from(QuestionnaireResponseModel source) {
            return new Builder()
                    .answered(source.answered)
                    .authorId(source.authorId)
                    .id(source.id)
                    .carePlanId(source.carePlanId)
                    .sourceId(source.sourceId)
                    .triagingCategory(source.triagingCategory)
                    .examinationAuthor(source.examinationAuthor)
                    .organizationId(source.organizationId())
                    .planDefinitionTitle(source.planDefinitionTitle())
                    .questionnaireId(source.questionnaireId)
                    .questionnaireName(source.questionnaireName())
                    .examinationStatus(source.examinationStatus)
                    .questionAnswerPairs(source.questionAnswerPairs)
                    .authorId(source.authorId)
                    .patient(source.patient);
        }

        public Builder organizationId(QualifiedId.OrganizationId organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Builder questionnaireId(QualifiedId.QuestionnaireId questionnaireId) {
            this.questionnaireId = questionnaireId;
            return this;
        }

        public Builder carePlanId(QualifiedId.CarePlanId carePlanId) {
            this.carePlanId = carePlanId;
            return this;
        }

        public Builder authorId(QualifiedId.PractitionerId authorId) {
            this.authorId = authorId;
            return this;
        }

        public Builder sourceId(QualifiedId.QuestionnaireId sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder questionnaireName(String questionnaireName) {
            this.questionnaireName = questionnaireName;
            return this;
        }

        public Builder questionAnswerPairs(List<QuestionAnswerPairModel> questionAnswerPairs) {
            this.questionAnswerPairs = questionAnswerPairs;
            return this;
        }

        public Builder answered(Instant answered) {
            this.answered = answered;
            return this;
        }

        public Builder examinationStatus(ExaminationStatus examinationStatus) {
            this.examinationStatus = examinationStatus;
            return this;
        }

        public Builder examinationAuthor(PractitionerModel examinationAuthor) {
            this.examinationAuthor = examinationAuthor;
            return this;
        }

        public Builder triagingCategory(TriagingCategory triagingCategory) {
            this.triagingCategory = triagingCategory;
            return this;
        }

        public Builder patient(PatientModel patient) {
            this.patient = patient;
            return this;
        }

        public Builder planDefinitionTitle(String planDefinitionTitle) {
            this.planDefinitionTitle = planDefinitionTitle;
            return this;
        }

        public Builder id(QualifiedId.QuestionnaireResponseId id) {
            this.id = id;
            return this;
        }

        public QuestionnaireResponseModel build() {
            return new QuestionnaireResponseModel(
                    id,
                    organizationId,
                    questionnaireId,
                    carePlanId,
                    authorId,
                    sourceId,
                    questionnaireName,
                    questionAnswerPairs,
                    answered,
                    examinationStatus,
                    examinationAuthor,
                    triagingCategory,
                    patient,
                    planDefinitionTitle
            );
        }


    }
}
