package dk.kvalitetsit.hjemmebehandling.api.question;

public class Option {
    private String option;
    private String comment;
    private String triage;

    public Option() {
    }

    public Option(String option, String comment, String triage) {
        this.option = option;
        this.comment = comment;
        this.triage = triage;
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTriage() {
        return triage;
    }

    public void setTriage(String triage) {
        this.triage = triage;
    }
}
