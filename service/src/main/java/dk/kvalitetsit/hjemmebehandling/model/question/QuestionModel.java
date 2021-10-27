package dk.kvalitetsit.hjemmebehandling.model.question;

public abstract class QuestionModel {
    private String text;
    private boolean required;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
