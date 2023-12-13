package dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers;

import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;

public abstract class Answer implements ToDto<dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer> {

    private String linkId;


    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public abstract int hashCode();

}
