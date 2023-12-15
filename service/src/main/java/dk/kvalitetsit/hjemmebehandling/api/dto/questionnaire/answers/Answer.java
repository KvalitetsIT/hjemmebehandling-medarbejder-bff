package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers;

import dk.kvalitetsit.hjemmebehandling.mapping.Dto;

public abstract class Answer<T> implements Dto<dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer<T>> {

    private String linkId;

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public abstract int hashCode();

}
