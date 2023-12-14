package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.Choice.MultipleChoice;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.Choice.SingleChoice;
import dk.kvalitetsit.hjemmebehandling.mapping.Dto;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.constants.EnableWhenOperator;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.stream.Collectors;


@Schema(oneOf = {QuestionDto.class, MultipleChoice.class, SingleChoice.class})
public abstract class BaseQuestionDto<T extends Answer> implements Dto<BaseQuestion<?>> {
    private String linkId;
    private String text;
    private String abbreviation;
    private String helperText;
    private boolean required;
    //private QuestionType questionType;

    private List<EnableWhen> enableWhens;

    private boolean deprecated;


    public BaseQuestionDto(String text) {
        this.text = text;
    }

    public abstract void answer(T answer);


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
     * Since this abstract class cannot have the toModel method this is intended as a workaround.
     * It mutates the model with the base fields
     * @param model
     */
    protected void decorateModel(dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion<dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer<?>> model) {
        model.setAbbreviation(this.abbreviation);
        model.setHelperText(this.getHelperText());
        model.setRequired(isRequired());
        model.setDeprecated(isDeprecated());

        // TODO: Consider throwing an error if any of the subsequent fields is null
        if(this.linkId != null) model.setLinkId(this.linkId);
        if(this.enableWhens != null) model.setEnableWhens(this.enableWhens.stream().map(EnableWhen::toModel).collect(Collectors.toList()));

    }



    public static class EnableWhen implements Dto<BaseQuestion.EnableWhen> {
        private Answer answer; // contains linkId for another question and desired answer[type,value]
        private EnableWhenOperator operator;

        public Answer getAnswer() {
            return answer;
        }

        public void setAnswer(Answer answer) {
            this.answer = answer;
        }

        public EnableWhenOperator getOperator() {
            return operator;
        }

        public void setOperator(EnableWhenOperator operator) {
            this.operator = operator;
        }


        @Override
        public BaseQuestion.EnableWhen toModel() {
            var model = new BaseQuestion.EnableWhen();
            model.setAnswer(this.answer.toModel());
            model.setOperator(this.operator);

            return model;
        }
    }
}
