package dk.kvalitetsit.hjemmebehandling.api.request;

import dk.kvalitetsit.hjemmebehandling.api.question.QuestionDto;

import java.util.List;

public class UpdateQuestionnaireRequest {
    private String title;
    private String description;
    private String status;
    private List<QuestionDto> questions;
    private List<QuestionDto> callToActions;

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

    public List<QuestionDto> getCallToActions() {
        return callToActions;
    }

    public void setCallToActions(List<QuestionDto> callToActions) {
        this.callToActions = callToActions;
    }
}
