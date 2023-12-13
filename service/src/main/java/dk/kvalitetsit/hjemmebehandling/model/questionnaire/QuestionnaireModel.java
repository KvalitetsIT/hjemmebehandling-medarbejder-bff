package dk.kvalitetsit.hjemmebehandling.model.questionnaire;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.QuestionnaireDto;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.model.BaseModel;
import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionnaireModel extends BaseModel implements ToDto<QuestionnaireDto> {
    private String title;
    private String description;
    private QuestionnaireStatus status;
    private List<BaseQuestion<Answer>> questions;
    private List<BaseQuestion<Answer>> callToActions;
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


    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public List<BaseQuestion<Answer>> getQuestions() {
        return questions;
    }

    public void setQuestions(List<BaseQuestion<Answer>> questions) {
        this.questions = questions;
    }

    public List<BaseQuestion<Answer>> getCallToActions() {
        return callToActions;
    }

    public void setCallToActions(List<BaseQuestion<Answer>> callToActions) {
        this.callToActions = callToActions;
    }

    @Override
    public QuestionnaireDto toDto() {
        QuestionnaireDto questionnaireDto = new QuestionnaireDto();

        questionnaireDto.setId(this.getId().toString());
        questionnaireDto.setTitle(this.getTitle());
        questionnaireDto.setStatus(this.getStatus().toString());
        questionnaireDto.setVersion(this.getVersion());
        questionnaireDto.setLastUpdated(this.getLastUpdated());

        if(this.getQuestions() != null) {
            questionnaireDto.setQuestions(this.getQuestions().stream().map(ToDto::toDto).collect(Collectors.toList()));
        }
        if(this.getCallToActions() != null) {
            questionnaireDto.setCallToActions(this.getCallToActions().stream().map(ToDto::toDto).collect(Collectors.toList()));
        }

        return questionnaireDto;
    }
}
