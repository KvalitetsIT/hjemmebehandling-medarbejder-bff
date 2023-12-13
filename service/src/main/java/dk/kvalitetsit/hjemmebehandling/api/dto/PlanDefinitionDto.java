package dk.kvalitetsit.hjemmebehandling.api.dto;

import dk.kvalitetsit.hjemmebehandling.constants.PlanDefinitionStatus;
import dk.kvalitetsit.hjemmebehandling.mapping.ToModel;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import org.hl7.fhir.r4.model.ResourceType;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static dk.kvalitetsit.hjemmebehandling.api.DtoMapper.mapBaseAttributesToModel;

public class PlanDefinitionDto extends BaseDto implements ToModel<PlanDefinitionModel> {
    private String name;
    private String title;
    private String status;
    private Instant created;
    private Instant lastUpdated;

    private List<QuestionnaireWrapperDto> questionnaires;

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public List<QuestionnaireWrapperDto> getQuestionnaires() {
        return questionnaires;
    }

    public void setQuestionnaires(List<QuestionnaireWrapperDto> questionnaires) {
        this.questionnaires = questionnaires;
    }

    @Override
    public PlanDefinitionModel toModel() {

        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        mapBaseAttributesToModel(planDefinitionModel, this, ResourceType.PlanDefinition);

        planDefinitionModel.setName(this.getName());
        planDefinitionModel.setTitle(this.getTitle());
        if(this.getStatus() != null) {
            planDefinitionModel.setStatus(Enum.valueOf(PlanDefinitionStatus.class, this.getStatus()));
        }
        planDefinitionModel.setCreated(this.getCreated());
        // TODO - planDefinitionModel.getQuestionnaires() should never return null - but it can for now.
        if(this.getQuestionnaires() != null) {
            planDefinitionModel.setQuestionnaires(this.getQuestionnaires().stream().map(QuestionnaireWrapperDto::toModel).collect(Collectors.toList()));
        }

        return planDefinitionModel;

    }


}
