package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers;

import dk.kvalitetsit.hjemmebehandling.mapping.ToModel;

public abstract class Answer implements ToModel<dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer> {

    private String linkId;

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public abstract int hashCode();

}
