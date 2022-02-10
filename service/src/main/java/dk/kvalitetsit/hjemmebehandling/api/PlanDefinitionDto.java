package dk.kvalitetsit.hjemmebehandling.api;

import java.time.Instant;
import java.util.List;

public class PlanDefinitionDto extends BaseDto {
    private String name;
    private String title;
    private String status;
    private Instant created;
    private List<QuestionnaireWrapperDto> questionnaires;

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
}
