package dk.kvalitetsit.hjemmebehandling.model;

public class Option {
    private String option;
    private String comment;

    public Option() {
    }

    public Option(String option, String comment) {
        this.option = option;
        this.comment = comment;
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

}
