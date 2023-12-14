package dk.kvalitetsit.hjemmebehandling.model.questionnaire.question;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.BaseQuestionDto;
import dk.kvalitetsit.hjemmebehandling.constants.EnableWhenOperator;
import dk.kvalitetsit.hjemmebehandling.mapping.Model;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseQuestion<T extends Answer<?>> implements Model<BaseQuestionDto<?>> {
    private String linkId;
    private String text;
    private String abbreviation;
    private String helperText;
    private boolean required;
    private List<EnableWhen> enableWhens;
    private boolean deprecated;
    public BaseQuestion(String text) {
        this.text = text;
    }

    public abstract void answer(Answer<?> answer);
    public abstract T getAnswer();

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
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

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<EnableWhen> getEnableWhens() {
        return enableWhens;
    }

    public void setEnableWhens(List<EnableWhen> enableWhens) {
        this.enableWhens = enableWhens;
    }

    /**
     * Since this abstract class cannot have the toDTO method this is intended as a workaround
     * It mutates the dto with the base fields
     * @param dto
     */
    protected void decorateDto(BaseQuestionDto<?> dto){

        dto.setHelperText(this.getHelperText());
        dto.setAbbreviation(this.getAbbreviation());
        dto.setDeprecated(this.isDeprecated());

        if (this.getEnableWhens() != null) dto.setEnableWhens(this.getEnableWhens().stream().map(EnableWhen::toDto).collect(Collectors.toList()));

        dto.setRequired(this.isRequired());
        dto.setLinkId(this.getLinkId());

    }



    public static class EnableWhen implements Model<BaseQuestionDto.EnableWhen> {
        private Answer<?> answer; // contains linkId for another question and desired answer[type,value]
        private EnableWhenOperator operator;

        public Answer<?> getAnswer() {
            return answer;
        }

        public void setAnswer(Answer<?> answer) {
            this.answer = answer;
        }

        public EnableWhenOperator getOperator() {
            return operator;
        }

        public void setOperator(EnableWhenOperator operator) {
            this.operator = operator;
        }


        @Override
        public BaseQuestionDto.EnableWhen toDto() {
            BaseQuestionDto.EnableWhen enableWhenComponent = new BaseQuestionDto.EnableWhen();

            enableWhenComponent.setAnswer(this.answer.toDto());
            enableWhenComponent.setOperator(this.operator);

            return enableWhenComponent;
        }

    }
}
