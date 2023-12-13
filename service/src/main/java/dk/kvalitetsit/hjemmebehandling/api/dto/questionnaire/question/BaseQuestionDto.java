package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question;

import dk.kvalitetsit.hjemmebehandling.mapping.ToModel;
import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.constants.EnableWhenOperator;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Number;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Question;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;

import java.util.List;

public abstract class BaseQuestionDto<T extends Answer> implements ToModel<BaseQuestion<?>> {
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

    /*
    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }
    */

    public List<EnableWhen> getEnableWhens() {
        return enableWhens;
    }

    public void setEnableWhens(List<EnableWhen> enableWhens) {
        this.enableWhens = enableWhens;
    }


    public static class EnableWhen {
        private AnswerModel answer; // contains linkId for another question and desired answer[type,value]
        private EnableWhenOperator operator;

        public AnswerModel getAnswer() {
            return answer;
        }

        public void setAnswer(AnswerModel answer) {
            this.answer = answer;
        }

        public EnableWhenOperator getOperator() {
            return operator;
        }

        public void setOperator(EnableWhenOperator operator) {
            this.operator = operator;
        }
    }
}
