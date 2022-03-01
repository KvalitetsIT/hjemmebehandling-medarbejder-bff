package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.api.question.QuestionDto;

import java.util.Date;
import java.util.List;

public class QuestionnaireDto extends BaseDto {
    private String title;
    private String status;
    private List<QuestionDto> questions;
    private List<QuestionDto> callToActions;
    private String version;
    private Date lastUpdated;

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

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public List<QuestionDto> getCallToActions() {
        return callToActions;
    }

    public void setCallToActions(List<QuestionDto> callToActions) {
        this.callToActions = callToActions;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }
}
