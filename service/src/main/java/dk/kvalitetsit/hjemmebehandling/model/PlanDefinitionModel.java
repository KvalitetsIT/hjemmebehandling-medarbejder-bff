package dk.kvalitetsit.hjemmebehandling.model;


import dk.kvalitetsit.hjemmebehandling.api.dto.PlanDefinitionDto;
import dk.kvalitetsit.hjemmebehandling.constants.PlanDefinitionStatus;
import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlanDefinitionModel extends BaseModel implements ToDto<PlanDefinitionDto> {
    private String name;
    private String title;
    private PlanDefinitionStatus status;
    private Instant created;

    private Instant lastUpdated;

    private List<QuestionnaireWrapperModel> questionnaires;

    public PlanDefinitionModel() {
        questionnaires = new ArrayList<>();
    }


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

    public PlanDefinitionStatus getStatus() {
        return status;
    }

    public void setStatus(PlanDefinitionStatus status) {
        this.status = status;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public List<QuestionnaireWrapperModel> getQuestionnaires() {
        return questionnaires;
    }

    public void setQuestionnaires(List<QuestionnaireWrapperModel> questionnaires) {
        this.questionnaires = questionnaires;
    }

    @Override
    public PlanDefinitionDto toDto() {
        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();

        planDefinitionDto.setId(this.getId().toString());
        planDefinitionDto.setName(this.getName());
        planDefinitionDto.setTitle(this.getTitle());
        planDefinitionDto.setStatus(this.getStatus().toString());
        planDefinitionDto.setCreated(this.getCreated());
        planDefinitionDto.setLastUpdated(this.getLastUpdated());
        // TODO - planDefinitionModel.getQuestionnaires() should never return null - but it can for now.
        if(this.getQuestionnaires() != null) {
            planDefinitionDto.setQuestionnaires(this.getQuestionnaires().stream().map(QuestionnaireWrapperModel::toDto).collect(Collectors.toList()));
        }

        return planDefinitionDto;
    }
}
