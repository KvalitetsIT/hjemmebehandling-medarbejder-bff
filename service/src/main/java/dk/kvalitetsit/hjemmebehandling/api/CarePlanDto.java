package dk.kvalitetsit.hjemmebehandling.api;

import java.util.List;
import java.util.Map;

public class CarePlanDto {
    private String id;
    private String title;
    private String status;
    private List<QuestionnaireDto> questionnaires;
    private Map<String, FrequencyDto> questionnaireFrequencies;
}
