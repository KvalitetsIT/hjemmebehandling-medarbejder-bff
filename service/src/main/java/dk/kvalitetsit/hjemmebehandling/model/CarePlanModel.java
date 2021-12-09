package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;

import java.time.Instant;
import java.util.List;

public class CarePlanModel extends BaseModel {
    private String title;
    private CarePlanStatus status;
    private Instant created;
    private Instant startDate;
    private Instant endDate;
    private PatientModel patient;
    private List<QuestionnaireWrapperModel> questionnaires;
    private List<PlanDefinitionModel> planDefinitions;
    private String departmentName;
    private Instant satisfiedUntil;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public CarePlanStatus getStatus() {
        return status;
    }

    public void setStatus(CarePlanStatus status) {
        this.status = status;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
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

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Instant getSatisfiedUntil() {
        return satisfiedUntil;
    }

    public void setSatisfiedUntil(Instant satisfiedUntil) {
        this.satisfiedUntil = satisfiedUntil;
    }
}
