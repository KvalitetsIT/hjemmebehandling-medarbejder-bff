package dk.kvalitetsit.hjemmebehandling.api;

import java.util.List;

public class UpdateCareplanRequest {
    private List<String> planDefinitionIds;
    private List<QuestionnaireFrequencyPairDto> questionnaires;
    private String patientPrimaryPhone;
    private String patientSecondaryPhone;
    private ContactDetailsDto patientPrimaryRelativeContactDetails;

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

    public ContactDetailsDto getPatientPrimaryRelativeContactDetails() {
        return patientPrimaryRelativeContactDetails;
    }

    public void setPatientPrimaryRelativeContactDetails(ContactDetailsDto patientPrimaryRelativeContactDetails) {
        this.patientPrimaryRelativeContactDetails = patientPrimaryRelativeContactDetails;
    }
}
