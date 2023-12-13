package dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers;

public abstract class Answer {

    private String linkId;


    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public abstract int hashCode();

}
