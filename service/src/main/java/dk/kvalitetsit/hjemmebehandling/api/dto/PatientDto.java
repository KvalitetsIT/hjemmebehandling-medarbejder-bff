package dk.kvalitetsit.hjemmebehandling.api.dto;

import dk.kvalitetsit.hjemmebehandling.mapping.Dto;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;

import java.util.List;
import java.util.stream.Collectors;

public class PatientDto extends BaseDto  implements Dto<PatientModel> {

    private String givenName;
    private String familyName;
    private String cpr;
    private String customUserName;
    private ContactDetailsDto patientContactDetails;
    private String primaryRelativeName;
    private String primaryRelativeAffiliation;
    private ContactDetailsDto primaryRelativeContactDetails;
    private List<ContactDetailsDto> additionalRelativeContactDetails;

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getCpr() {
        return cpr;
    }

    public void setCpr(String cpr) {
        this.cpr = cpr;
    }

    public ContactDetailsDto getPatientContactDetails() {
        return patientContactDetails;
    }

    public void setPatientContactDetails(ContactDetailsDto patientContactDetails) {
        this.patientContactDetails = patientContactDetails;
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

    public ContactDetailsDto getPrimaryRelativeContactDetails() {
        return primaryRelativeContactDetails;
    }

    public void setPrimaryRelativeContactDetails( ContactDetailsDto primaryRelativeContactDetails) {
        this.primaryRelativeContactDetails = primaryRelativeContactDetails;
    }

    public List<ContactDetailsDto> getAdditionalRelativeContactDetails() {
        return additionalRelativeContactDetails;
    }

    public void setAdditionalRelativeContactDetails(List<ContactDetailsDto> additionalRelativeContactDetails) {
        this.additionalRelativeContactDetails = additionalRelativeContactDetails;
    }

	public String getCustomUserName() {
		return customUserName;
	}

	public void setCustomUserName(String customUserName) {
		this.customUserName = customUserName;
	}

    @Override
    public PatientModel toModel() {

        PatientModel patientModel = new PatientModel();

        patientModel.setCpr(this.getCpr());
        patientModel.setFamilyName(this.getFamilyName());
        patientModel.setGivenName(this.getGivenName());
        if(this.getPatientContactDetails() != null) {
            patientModel.setContactDetails(this.getPatientContactDetails().toModel());
        }
        patientModel.getPrimaryContact().setName(this.getPrimaryRelativeName());
        patientModel.getPrimaryContact().setAffiliation(this.getPrimaryRelativeAffiliation());
        if(this.getPrimaryRelativeContactDetails() != null) {
            patientModel.getPrimaryContact().setContactDetails(this.getPrimaryRelativeContactDetails().toModel());
        }
        if(this.getAdditionalRelativeContactDetails() != null) {
            patientModel.setAdditionalRelativeContactDetails(this.getAdditionalRelativeContactDetails().stream().map(ContactDetailsDto::toModel).collect(Collectors.toList()));
        }

        return patientModel;

    }
}
