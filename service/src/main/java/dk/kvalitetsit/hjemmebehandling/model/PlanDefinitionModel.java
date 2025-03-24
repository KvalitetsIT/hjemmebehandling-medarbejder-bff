package dk.kvalitetsit.hjemmebehandling.model;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PlanDefinitionModel extends BaseModel {
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
}
