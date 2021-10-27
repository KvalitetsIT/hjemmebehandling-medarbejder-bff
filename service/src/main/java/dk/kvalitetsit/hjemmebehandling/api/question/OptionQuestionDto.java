package dk.kvalitetsit.hjemmebehandling.api.question;

import java.util.List;

public class OptionQuestionDto extends QuestionDto {
    private List<String> options;

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
