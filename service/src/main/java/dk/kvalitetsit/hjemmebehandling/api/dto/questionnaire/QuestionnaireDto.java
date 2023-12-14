package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire;

import dk.kvalitetsit.hjemmebehandling.api.dto.BaseDto;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.Choice.MultipleChoice;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.Choice.SingleChoice;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.mapping.Dto;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.BaseQuestionDto;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.QuestionnaireModel;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hl7.fhir.r4.model.ResourceType;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static dk.kvalitetsit.hjemmebehandling.api.DtoMapper.mapBaseAttributesToModel;

public class QuestionnaireDto extends BaseDto implements Dto<QuestionnaireModel> {
    private String title;
    private String status;

    private List<BaseQuestionDto<?>> questions;


    private List<BaseQuestionDto<?>> callToActions;
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

    public List<BaseQuestionDto<?>> getQuestions() {
        return questions;
    }

    public void setQuestions(List<BaseQuestionDto<?>> questions) {
        this.questions = questions;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public List<BaseQuestionDto<?>> getCallToActions() {
        return callToActions;
    }

    public void setCallToActions(List<BaseQuestionDto<?>> callToActions) {
        this.callToActions = callToActions;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public QuestionnaireModel toModel() {

        QuestionnaireModel questionnaireModel = new QuestionnaireModel();

        mapBaseAttributesToModel(questionnaireModel, this, ResourceType.Questionnaire);

        questionnaireModel.setTitle(this.getTitle());
        if (this.getStatus() != null) {
            questionnaireModel.setStatus(QuestionnaireStatus.valueOf(this.getStatus()));
        }
        if(this.getQuestions() != null) {
            questionnaireModel.setQuestions(
                    this.getQuestions()
                            .stream()
                            .map(Dto::toModel)
                            .collect(Collectors.toList()));
        }

        return questionnaireModel;
    }
}
