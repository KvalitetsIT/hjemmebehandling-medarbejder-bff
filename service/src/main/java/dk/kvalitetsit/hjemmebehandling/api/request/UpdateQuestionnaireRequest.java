package dk.kvalitetsit.hjemmebehandling.api.request;



import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.QuestionDto;

import java.util.List;

public class UpdateQuestionnaireRequest {
    private String title;
    private String description;
    private String status;
    private List<QuestionDto<? extends Answer>> questions;
    private List<QuestionDto<? extends Answer>> callToActions;

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

    public List<QuestionDto<? extends Answer>> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDto<? extends Answer>> questions) {
        this.questions = questions;
    }

    public List<QuestionDto<? extends Answer>> getCallToActions() {
        return callToActions;
    }

    public void setCallToActions(List<QuestionDto<? extends Answer>> callToActions) {
        this.callToActions = callToActions;
    }
}
