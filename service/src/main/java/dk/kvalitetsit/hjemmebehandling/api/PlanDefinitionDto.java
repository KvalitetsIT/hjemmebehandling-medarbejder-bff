package dk.kvalitetsit.hjemmebehandling.api;

import org.hl7.fhir.r4.model.Base;

import java.util.List;

public class PlanDefinitionDto extends BaseDto {
    private String name;
    private String title;
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

    public List<QuestionnaireWrapperDto> getQuestionnaires() {
        return questionnaires;
    }

    public void setQuestionnaires(List<QuestionnaireWrapperDto> questionnaires) {
        this.questionnaires = questionnaires;
    }
}
