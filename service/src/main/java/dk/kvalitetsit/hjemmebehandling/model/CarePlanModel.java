package dk.kvalitetsit.hjemmebehandling.model;

import java.util.List;

public class CarePlanModel {
    private String id;
    private String title;
    private String status;
    private PatientModel patient;
    private List<QuestionnaireWrapperModel> questionnaires;
    private List<PlanDefinitionModel> planDefinitions;

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

    public PatientModel getPatient() {
        return patient;
    }

    public void setPatient(PatientModel patient) {
        this.patient = patient;
    }

    public List<QuestionnaireWrapperModel> getQuestionnaires() {
        return questionnaires;
    }

    public void setQuestionnaires(List<QuestionnaireWrapperModel> questionnaires) {
        this.questionnaires = questionnaires;
    }

    public List<PlanDefinitionModel> getPlanDefinitions() {
        return planDefinitions;
    }

    public void setPlanDefinitions(List<PlanDefinitionModel> planDefinitions) {
        this.planDefinitions = planDefinitions;
    }
}
