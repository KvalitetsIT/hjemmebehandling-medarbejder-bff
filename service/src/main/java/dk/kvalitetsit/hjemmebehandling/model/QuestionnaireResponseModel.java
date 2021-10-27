package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;

import java.time.Instant;
import java.util.Map;

public class QuestionnaireResponseModel {
    private String id;
    private Map<QuestionModel, AnswerModel> answers;
    private Instant answered;
    //private QuestionnaireResponseStatus status; // TODO - figure out how this should work.
    private PatientModel patient;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<QuestionModel, AnswerModel> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<QuestionModel, AnswerModel> answers) {
        this.answers = answers;
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
