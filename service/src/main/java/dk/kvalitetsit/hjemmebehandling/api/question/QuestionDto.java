package dk.kvalitetsit.hjemmebehandling.api.question;

import dk.kvalitetsit.hjemmebehandling.api.ThresholdDto;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;

import java.util.List;

public class QuestionDto {
    private String linkId;
    private String text;
    private String abbreviation;
    private String helperText;
    private boolean required;
    private QuestionType questionType;
    private List<String> options;
    private List<QuestionModel.EnableWhen> enableWhen;
    private List<ThresholdDto> thresholds;

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String id) {
        this.linkId = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getHelperText() {
        return helperText;
    }

    public void setHelperText(String helperText) {
        this.helperText = helperText;
    }

    public boolean getRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<QuestionModel.EnableWhen> getEnableWhen() {
        return enableWhen;
    }

    public void setEnableWhen(List<QuestionModel.EnableWhen> enableWhen) {
        this.enableWhen = enableWhen;
    }

    public List<ThresholdDto> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<ThresholdDto> thresholds) {
        this.thresholds = thresholds;
    }
}
