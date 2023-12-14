package dk.kvalitetsit.hjemmebehandling.api.dto;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.QuestionnaireWrapperDto;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.mapping.Dto;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import org.hl7.fhir.r4.model.ResourceType;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static dk.kvalitetsit.hjemmebehandling.api.DtoMapper.mapBaseAttributesToModel;

public class CarePlanDto extends BaseDto implements Dto<CarePlanModel> {
    private String title;
    private String status;
    private Instant created;
    private Instant startDate;
    private Instant endDate;
    private PatientDto patientDto;
    private List<QuestionnaireWrapperDto> questionnaires;
    private List<PlanDefinitionDto> planDefinitions;
    private String departmentName;

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

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    @Override
    public CarePlanModel toModel() {
        CarePlanModel carePlanModel = new CarePlanModel();

        mapBaseAttributesToModel(carePlanModel, this, ResourceType.CarePlan);

        carePlanModel.setTitle(this.getTitle());
        if(this.getStatus() != null) {
            carePlanModel.setStatus(Enum.valueOf(CarePlanStatus.class, this.getStatus()));
        }
        carePlanModel.setCreated(this.getCreated());
        carePlanModel.setStartDate(this.getStartDate());
        carePlanModel.setEndDate(this.getEndDate());
        carePlanModel.setPatient(this.getPatientDto().toModel());
        carePlanModel.setQuestionnaires(List.of());
        if(this.getQuestionnaires() != null) {
            carePlanModel.setQuestionnaires(this.getQuestionnaires().stream().map(QuestionnaireWrapperDto::toModel).collect(Collectors.toList()));
        }
        carePlanModel.setPlanDefinitions(List.of());
        if(this.getPlanDefinitions() != null) {
            carePlanModel.setPlanDefinitions(this.getPlanDefinitions().stream().map(PlanDefinitionDto::toModel).collect(Collectors.toList()));
        }
        carePlanModel.setDepartmentName(this.getDepartmentName());

        return carePlanModel;
    }
}
