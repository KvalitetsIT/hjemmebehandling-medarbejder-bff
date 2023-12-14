package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.Choice;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer;

import java.util.List;
import java.util.Set;

public class MultipleChoice<T extends Answer> extends SingleChoice<T> {

    private List<T> answers;

    public MultipleChoice(String text) {
        super(text);
    }

    public void answer(List<T> answers) {
        Set<T> options = this.getOptions();
        if(!options.containsAll(answers) ) {
            throw INVALID_ANSWER_EXCEPTION;
        }
        this.answers = answers;
    }


    public List<T> getAnswers() {
        return answers;
    }

    public void setAnswers(List<T> answers) {
        this.answers = answers;
    }
}
