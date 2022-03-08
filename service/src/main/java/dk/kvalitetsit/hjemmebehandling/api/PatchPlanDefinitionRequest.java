package dk.kvalitetsit.hjemmebehandling.api;

import java.util.List;

public class PatchPlanDefinitionRequest {
    private List<String> questionnaireIds;
    private List<ThresholdDto> thresholds;

    public List<String> getQuestionnaireIds() {
        return questionnaireIds;
    }

    public void setQuestionnaireIds(List<String> questionnaireIds) {
        this.questionnaireIds = questionnaireIds;
    }

    public List<ThresholdDto> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<ThresholdDto> thresholds) {
        this.thresholds = thresholds;
    }
}
