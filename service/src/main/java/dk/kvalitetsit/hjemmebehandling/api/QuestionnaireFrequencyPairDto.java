package dk.kvalitetsit.hjemmebehandling.api;

public class QuestionnaireFrequencyPairDto {
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
