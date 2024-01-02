package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import org.hl7.fhir.r4.model.Base;

import java.util.Date;
import java.util.List;

public class QuestionnaireModel extends BaseModel {
    private String title;
    private String description;
    private QuestionnaireStatus status;
    private List<QuestionModel> questions;
    private QuestionModel callToAction;
    private String version;
    private Date lastUpdated;

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

    public QuestionnaireStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionnaireStatus status) {
        this.status = status;
    }

    public List<QuestionModel> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionModel> questions) {
        this.questions = questions;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public QuestionModel getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(QuestionModel callToAction) {
        this.callToAction = callToAction;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }
}
