package dk.kvalitetsit.hjemmebehandling.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class CarePlanDto {
    private String id;
    private String title;
    private String status;
    private PatientDto patientDto;
    private List<QuestionnaireWrapperDto> questionnaires;
    private List<PlanDefinitionDto> planDefinitions;

    @Schema(required = true, description = "Id of the CarePlan", example = "10")
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

    public List<QuestionnaireWrapperDto> getQuestionnaires() {
        return questionnaires;
    }

    public void setQuestionnaires(List<QuestionnaireWrapperDto> questionnaires) {
        this.questionnaires = questionnaires;
    }

    public List<PlanDefinitionDto> getPlanDefinitions() {
        return planDefinitions;
    }

    public void setPlanDefinitions(List<PlanDefinitionDto> planDefinitions) {
        this.planDefinitions = planDefinitions;
    }
}
