package dk.kvalitetsit.hjemmebehandling.api.request;

import dk.kvalitetsit.hjemmebehandling.api.dto.ThresholdDto;
import dk.kvalitetsit.hjemmebehandling.constants.PlanDefinitionStatus;

import java.util.List;

public class PatchPlanDefinitionRequest {
    private String name;
    private List<String> questionnaireIds;
    private List<ThresholdDto> thresholds;

    private PlanDefinitionStatus status;

    public PlanDefinitionStatus getStatus() {
        return status;
    }

    public void setStatus(PlanDefinitionStatus status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
