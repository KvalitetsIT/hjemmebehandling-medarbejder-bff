package dk.kvalitetsit.hjemmebehandling.api.question;

public abstract class QuestionDto {
    private String text;
    private boolean required;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
