package dk.kvalitetsit.hjemmebehandling.api;

import java.time.Instant;
import java.util.List;

public class QuestionnaireResponseDto {
    private String id;
    private String questionnaireId;
    private List<QuestionAnswerPairDto> questionAnswerPairs;
    private Instant answered;
    private PatientDto patient;

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

    public List<QuestionAnswerPairDto> getQuestionAnswerPairs() {
        return questionAnswerPairs;
    }

    public void setQuestionAnswerPairs(List<QuestionAnswerPairDto> questionAnswerPairs) {
        this.questionAnswerPairs = questionAnswerPairs;
    }

    public Instant getAnswered() {
        return answered;
    }

    public void setAnswered(Instant answered) {
        this.answered = answered;
    }

    public PatientDto getPatient() {
        return patient;
    }

    public void setPatient(PatientDto patient) {
        this.patient = patient;
    }
}
