package dk.kvalitetsit.hjemmebehandling.model.question;

import java.util.List;

public class OptionQuestionModel extends QuestionModel {
    private List<String> options;

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
