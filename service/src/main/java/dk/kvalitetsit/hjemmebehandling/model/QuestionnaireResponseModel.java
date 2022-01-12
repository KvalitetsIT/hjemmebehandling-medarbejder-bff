package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.TriagingCategory;

import java.time.Instant;
import java.util.List;

public class QuestionnaireResponseModel extends BaseModel {
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

    public QualifiedId getQuestionnaireId() {
        return questionnaireId;
    }

    public void setQuestionnaireId(QualifiedId questionnaireId) {
        this.questionnaireId = questionnaireId;
    }

    public QualifiedId getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(QualifiedId carePlanId) {
        this.carePlanId = carePlanId;
    }

    public QualifiedId getAuthorId() {
        return authorId;
    }

    public void setAuthorId(QualifiedId authorId) {
        this.authorId = authorId;
    }

    public QualifiedId getSourceId() {
        return sourceId;
    }

    public void setSourceId(QualifiedId sourceId) {
        this.sourceId = sourceId;
    }

    public String getQuestionnaireName() {
        return questionnaireName;
    }

    public void setQuestionnaireName(String questionnaireName) {
        this.questionnaireName = questionnaireName;
    }

    public List<QuestionAnswerPairModel> getQuestionAnswerPairs() {
        return questionAnswerPairs;
    }

    public void setQuestionAnswerPairs(List<QuestionAnswerPairModel> questionAnswerPairs) {
        this.questionAnswerPairs = questionAnswerPairs;
    }

    public Instant getAnswered() {
        return answered;
    }

    public void setAnswered(Instant answered) {
        this.answered = answered;
    }

    public ExaminationStatus getExaminationStatus() {
        return examinationStatus;
    }

    public void setExaminationStatus(ExaminationStatus examinationStatus) {
        this.examinationStatus = examinationStatus;
    }

    public PractitionerModel getExaminationAuthor() {
        return examinationAuthor;
    }

    public void setExaminationAuthor(PractitionerModel examinationAuthor) {
        this.examinationAuthor = examinationAuthor;
    }

    public TriagingCategory getTriagingCategory() {
        return triagingCategory;
    }

    public void setTriagingCategory(TriagingCategory triagingCategory) {
        this.triagingCategory = triagingCategory;
    }

    public PatientModel getPatient() {
        return patient;
    }

    public void setPatient(PatientModel patient) {
        this.patient = patient;
    }

    public String getPlanDefinitionTitle() {
        return planDefinitionTitle;
    }

    public void setPlanDefinitionTitle(String planDefinitionTitle) {
        this.planDefinitionTitle = planDefinitionTitle;
    }
}
