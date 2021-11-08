package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.TriagingCategory;

import java.time.Instant;
import java.util.List;

public class QuestionnaireResponseModel {
    private String id;
    private String questionnaireId;
    private String questionnaireName;
    private List<QuestionAnswerPairModel> questionAnswerPairs;
    private Instant answered;
    private ExaminationStatus examinationStatus;
    private TriagingCategory triagingCategory;
    private PatientModel patient;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestionnaireId() {
        return questionnaireId;
    }

    public void setQuestionnaireId(String questionnaireId) {
        this.questionnaireId = questionnaireId;
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
}
