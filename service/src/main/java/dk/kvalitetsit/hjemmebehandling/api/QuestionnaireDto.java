package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.api.question.QuestionDto;

import java.util.Date;
import java.util.List;

public class QuestionnaireDto extends BaseDto {
    private String title;
    private String status;
    private List<QuestionDto> questions;
    private QuestionDto callToAction;
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

    public QuestionDto getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(QuestionDto callToAction) {
        this.callToAction = callToAction;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }
}
