package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire;

import dk.kvalitetsit.hjemmebehandling.api.dto.FrequencyDto;

public class QuestionnaireFrequencyPairDto{
    String id;
    FrequencyDto frequency;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FrequencyDto getFrequency() {
        return frequency;
    }

    public void setFrequency(FrequencyDto frequency) {
        this.frequency = frequency;
    }
}
