package dk.kvalitetsit.hjemmebehandling.api;

import java.util.List;
import java.util.Map;

public class UpdateCareplanRequest {
    private List<String> planDefinitionIds;
    private List<String> questionnaireIds;
    private Map<String, FrequencyDto> questionnaireFrequencies;
    private String patientPrimaryPhone;
    private String patientSecondaryPhone;
    private ContactDetailsDto patientPrimaryRelativeContactDetails;

    public List<String> getPlanDefinitionIds() {
        return planDefinitionIds;
    }

    public void setPlanDefinitionIds(List<String> planDefinitionIds) {
        this.planDefinitionIds = planDefinitionIds;
    }

    public List<String> getQuestionnaireIds() {
        return questionnaireIds;
    }

    public void setQuestionnaireIds(List<String> questionnaireIds) {
        this.questionnaireIds = questionnaireIds;
    }

    public Map<String, FrequencyDto> getQuestionnaireFrequencies() {
        return questionnaireFrequencies;
    }

    public void setQuestionnaireFrequencies(Map<String, FrequencyDto> questionnaireFrequencies) {
        this.questionnaireFrequencies = questionnaireFrequencies;
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
