package dk.kvalitetsit.hjemmebehandling.api.request;

import dk.kvalitetsit.hjemmebehandling.api.dto.QuestionnaireFrequencyPairDto;

import java.util.List;

public class UpdateCareplanRequest {
    private List<String> planDefinitionIds;
    private List<QuestionnaireFrequencyPairDto> questionnaires;
    private String patientPrimaryPhone;
    private String patientSecondaryPhone;
    private String primaryRelativeName;
    private String primaryRelativeAffiliation;
    private String primaryRelativePrimaryPhone;
    private String primaryRelativeSecondaryPhone;

    public List<String> getPlanDefinitionIds() {
        return planDefinitionIds;
    }

    public void setPlanDefinitionIds(List<String> planDefinitionIds) {
        this.planDefinitionIds = planDefinitionIds;
    }

    public List<QuestionnaireFrequencyPairDto> getQuestionnaires() {
        return questionnaires;
    }

    public void setQuestionnaires(List<QuestionnaireFrequencyPairDto> questionnaires) {
        this.questionnaires = questionnaires;
    }

    public String getPatientPrimaryPhone() {
        return patientPrimaryPhone;
    }

    public void setPatientPrimaryPhone(String patientPrimaryPhone) {
        this.patientPrimaryPhone = patientPrimaryPhone;
    }

    public String getPatientSecondaryPhone() {
        return patientSecondaryPhone;
    }

    public void setPatientSecondaryPhone(String patientSecondaryPhone) {
        this.patientSecondaryPhone = patientSecondaryPhone;
    }

    public String getPrimaryRelativeName() {
        return primaryRelativeName;
    }

    public void setPrimaryRelativeName(String primaryRelativeName) {
        this.primaryRelativeName = primaryRelativeName;
    }

    public String getPrimaryRelativeAffiliation() {
        return primaryRelativeAffiliation;
    }

    public void setPrimaryRelativeAffiliation(String primaryRelativeAffiliation) {
        this.primaryRelativeAffiliation = primaryRelativeAffiliation;
    }

    public String getPrimaryRelativePrimaryPhone() {
        return primaryRelativePrimaryPhone;
    }

    public void setPrimaryRelativePrimaryPhone(String primaryRelativePrimaryPhone) {
        this.primaryRelativePrimaryPhone = primaryRelativePrimaryPhone;
    }

    public String getPrimaryRelativeSecondaryPhone() {
        return primaryRelativeSecondaryPhone;
    }

    public void setPrimaryRelativeSecondaryPhone(String primaryRelativeSecondaryPhone) {
        this.primaryRelativeSecondaryPhone = primaryRelativeSecondaryPhone;
    }
}
