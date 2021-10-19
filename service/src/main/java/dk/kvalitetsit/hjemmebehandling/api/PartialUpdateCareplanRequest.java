package dk.kvalitetsit.hjemmebehandling.api;

import java.util.List;
import java.util.Map;

public class PartialUpdateCareplanRequest {
    private List<String> questionnaireIds;
    private Map<String, FrequencyDto> questionnaireFrequencies;

    public List<String> getQuestionnaireIds() {
        return questionnaireIds;
    }

    public void setQuestionnaireIds(List<String> questionnaireIds) {
        this.questionnaireIds = questionnaireIds;
    }

    public Map<String, FrequencyDto> getQuestionnaireFrequencies() {
        return questionnaireFrequencies;
    }

    public void setQuestionnaireFrequencies(Map<String, FrequencyDto> questionnaireFrequencies) {
        this.questionnaireFrequencies = questionnaireFrequencies;
    }
}
