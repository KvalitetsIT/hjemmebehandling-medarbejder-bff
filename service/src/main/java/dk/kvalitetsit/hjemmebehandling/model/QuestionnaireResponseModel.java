package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.constants.TriagingCategory;

import java.time.Instant;
import java.util.List;

public record QuestionnaireResponseModel(
        QualifiedId id,
        QualifiedId questionnaireId,
        QualifiedId carePlanId,
        QualifiedId authorId,
        QualifiedId sourceId,
        String questionnaireName,
        List<QuestionAnswerPairModel> questionAnswerPairs,
        Instant answered,
        ExaminationStatus examinationStatus,
        PractitionerModel examinationAuthor,
        TriagingCategory triagingCategory,
        PatientModel patient,
        String planDefinitionTitle

) implements BaseModel {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private QualifiedId id;


        private QualifiedId questionnaireId;
        private QualifiedId carePlanId;
        private QualifiedId authorId;
        private QualifiedId sourceId;
        private String questionnaireName;
        private List<QuestionAnswerPairModel> questionAnswerPairs;
        private Instant answered;
        private ExaminationStatus examinationStatus;
        private PractitionerModel examinationAuthor;
        private TriagingCategory triagingCategory;
        private PatientModel patient;
        private String planDefinitionTitle;

        public Builder questionnaireId(QualifiedId questionnaireId) {
            this.questionnaireId = questionnaireId;
            return this;
        }

        public Builder carePlanId(QualifiedId carePlanId) {
            this.carePlanId = carePlanId;
            return this;
        }

        public Builder authorId(QualifiedId authorId) {
            this.authorId = authorId;
            return this;
        }

        public Builder sourceId(QualifiedId sourceId) {
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

        public Builder id(QualifiedId id) {
            this.id = id;
            return this;
        }

        public QuestionnaireResponseModel build() {
            return new QuestionnaireResponseModel(
                    id,
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

    @Override
    public QualifiedId id() {
        return null;
    }

    @Override
    public String organizationId() {
        return "";
    }
}
