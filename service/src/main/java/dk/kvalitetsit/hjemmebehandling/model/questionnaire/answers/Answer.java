package dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers;

import dk.kvalitetsit.hjemmebehandling.mapping.Model;

public abstract class Answer<T> implements Model<dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer> {

    private String linkId;

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public abstract int hashCode();

    public abstract T getValue();


}
