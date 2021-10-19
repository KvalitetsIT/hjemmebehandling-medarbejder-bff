package dk.kvalitetsit.hjemmebehandling.api;

import java.util.List;
import java.util.Map;

public class CarePlanDto {
    private String id;
    private String title;
    private String status;
    private PatientDto patientDto;
    private List<QuestionnaireDto> questionnaires;
    private Map<String, FrequencyDto> questionnaireFrequencies;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PatientDto getPatientDto() {
        return patientDto;
    }

    public void setPatientDto(PatientDto patientDto) {
        this.patientDto = patientDto;
    }

    public List<QuestionnaireDto> getQuestionnaires() {
        return questionnaires;
    }

    public void setQuestionnaires(List<QuestionnaireDto> questionnaires) {
        this.questionnaires = questionnaires;
    }

    public Map<String, FrequencyDto> getQuestionnaireFrequencies() {
        return questionnaireFrequencies;
    }

    public void setQuestionnaireFrequencies(Map<String, FrequencyDto> questionnaireFrequencies) {
        this.questionnaireFrequencies = questionnaireFrequencies;
    }
}
