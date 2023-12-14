package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.api.dto.CarePlanDto;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.mapping.Model;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.QuestionnaireWrapperModel;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class CarePlanModel extends BaseModel implements Model<CarePlanDto> {
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

    @Override
    public String toString() {
        return "CarePlanModel{" +
                "title='" + title + '\'' +
                ", status=" + status +
                ", created=" + created +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", patient=" + patient +
                ", questionnaires=" + questionnaires +
                ", planDefinitions=" + planDefinitions +
                ", departmentName='" + departmentName + '\'' +
                ", satisfiedUntil=" + satisfiedUntil +
                '}';
    }

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

    @Override
    public CarePlanDto toDto() {
        CarePlanDto carePlanDto = new CarePlanDto();

        carePlanDto.setId(this.getId().toString());
        carePlanDto.setTitle(this.getTitle());
        carePlanDto.setStatus(this.getStatus().toString());
        carePlanDto.setCreated(this.getCreated());
        carePlanDto.setStartDate(this.getStartDate());
        carePlanDto.setEndDate(this.getEndDate());
        carePlanDto.setPatientDto(this.getPatient().toDto());
        carePlanDto.setQuestionnaires(this.getQuestionnaires().stream().map(QuestionnaireWrapperModel::toDto).collect(Collectors.toList()));
        carePlanDto.setPlanDefinitions(this.getPlanDefinitions().stream().map(PlanDefinitionModel::toDto).collect(Collectors.toList()));
        carePlanDto.setDepartmentName(this.getDepartmentName());

        return carePlanDto;
    }
}
