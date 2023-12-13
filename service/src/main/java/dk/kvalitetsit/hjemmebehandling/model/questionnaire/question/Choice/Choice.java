package dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Choice;

import dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer;

import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;

import java.util.HashSet;
import java.util.Set;

abstract class Choice<T extends Answer> extends BaseQuestion<T> {

    private HashSet<T> options = new HashSet<>();

    public Choice(String text) {
        super(text);
    }

    public Set<T> getOptions() {
        return options;
    }

    public void setOptions(HashSet<T> options) {
        this.options = options;
    }

    public void addOption(T option) {
        this.options.add(option);
    }

}


