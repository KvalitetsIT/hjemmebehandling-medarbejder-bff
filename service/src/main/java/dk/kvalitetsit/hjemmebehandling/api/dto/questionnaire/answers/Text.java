package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers;

import java.util.Objects;

public class Text extends Answer {
    private String value;

    public Text(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Text text = (Text) o;
        return Objects.equals(value, text.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer toModel() {
        return new dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Text(this.value);
    }
}
