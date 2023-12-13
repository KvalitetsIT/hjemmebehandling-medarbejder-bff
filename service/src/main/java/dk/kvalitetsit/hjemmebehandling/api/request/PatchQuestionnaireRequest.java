package dk.kvalitetsit.hjemmebehandling.api.request;



import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.BaseQuestionDto;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.QuestionDto;

import java.util.List;

public class PatchQuestionnaireRequest {
    private String title;
    private String description;
    private String status;
    private List<BaseQuestionDto<?>> questions;
    private List<BaseQuestionDto<?>> callToActions;

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


    public List<BaseQuestionDto<?>> getQuestions() {
        return questions;
    }

    public void setQuestions(List<BaseQuestionDto<?>> questions) {
        this.questions = questions;
    }

    public List<BaseQuestionDto<?>> getCallToActions() {
        return callToActions;
    }

    public void setCallToActions(List<BaseQuestionDto<?>> callToActions) {
        this.callToActions = callToActions;
    }
}
