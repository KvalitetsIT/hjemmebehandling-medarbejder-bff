package dk.kvalitetsit.hjemmebehandling.model;

import java.time.Instant;
import java.util.List;

public class QuestionnaireResponseModel {
    private String id;
    private String questionnaireId;
    private List<QuestionAnswerPairModel> questionAnswerPairs;
    private Instant answered;
    //private QuestionnaireResponseStatus status; // TODO - figure out how this should work.
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

    public PatientModel getPatient() {
        return patient;
    }

    public void setPatient(PatientModel patient) {
        this.patient = patient;
    }
}
