package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.api.question.QuestionDto;

import java.util.List;

public class PatchQuestionnaireRequest {
    private String title;
    private String description;
    private String status;
    private List<QuestionDto> questions;
    private QuestionDto callToAction;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public QuestionDto getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(QuestionDto callToAction) {
        this.callToAction = callToAction;
    }
}
