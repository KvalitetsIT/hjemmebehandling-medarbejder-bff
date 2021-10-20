package dk.kvalitetsit.hjemmebehandling.api;

import java.util.List;

public class QuestionnaireDto {
    private String id;
    private String title;
    private String status;
    private List<QuestionDto> questions;

    // TODO: Think about thresholds and their relation to questions ...

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<QuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDto> questions) {
        this.questions = questions;
    }
}
