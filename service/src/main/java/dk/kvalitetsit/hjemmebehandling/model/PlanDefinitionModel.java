package dk.kvalitetsit.hjemmebehandling.model;

import java.util.List;

public class PlanDefinitionModel extends BaseModel {
    private String name;
    private String title;
    private List<QuestionnaireWrapperModel> questionnaires;

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

    public List<QuestionnaireWrapperModel> getQuestionnaires() {
        return questionnaires;
    }

    public void setQuestionnaires(List<QuestionnaireWrapperModel> questionnaires) {
        this.questionnaires = questionnaires;
    }
}
