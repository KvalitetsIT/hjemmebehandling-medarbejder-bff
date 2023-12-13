package dk.kvalitetsit.hjemmebehandling.model.questionnaire.question;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.constants.EnableWhenOperator;
import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer;
import org.hl7.fhir.r4.model.Questionnaire;

import java.util.List;

import static dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper.getValue;

public abstract class BaseQuestion<T extends Answer> implements ToDto<QuestionDto<?>> {
    private String linkId;
    private String text;
    private String abbreviation;
    private String helperText;
    private boolean required;
    //private QuestionType questionType;

    private List<EnableWhen> enableWhens;

    private boolean deprecated;


    public BaseQuestion(String text) {
        this.text = text;
    }

    public abstract void answer(T answer);
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




    public static class EnableWhen implements ToDto<Questionnaire.QuestionnaireItemEnableWhenComponent> {
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


        @Override
        public Questionnaire.QuestionnaireItemEnableWhenComponent toDto() {
            Questionnaire.QuestionnaireItemEnableWhenComponent enableWhenComponent = new Questionnaire.QuestionnaireItemEnableWhenComponent();

            enableWhenComponent.setOperator( this.getOperator().toDto() );
            enableWhenComponent.setQuestion(this.getAnswer().getLinkId());
            enableWhenComponent.setAnswer(getValue(this.getAnswer()));
            enableWhenComponent.setOperator(this.getOperator().toDto());


            // TODO: Determine which question this is

            return enableWhenComponent;
        }

    }
}
