package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.api.ContactDetailsDto;

import java.util.List;

public class PatientModel {
    private QualifiedId id;
    private String givenName;
    private String familyName;
    private String cpr;
    private ContactDetailsModel patientContactDetails;
    private String primaryRelativeName;
    private String primaryRelativeAffiliation;
    private ContactDetailsModel primaryRelativeContactDetails;
    private List<ContactDetailsModel> additionalRelativeContactDetails;
    private String customUserId;
    private String customUserName;
    
    
    public QualifiedId getId() {
        return id;
    }

    public void setId(QualifiedId id) {
        this.id = id;
    }

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

    public ContactDetailsModel getPatientContactDetails() {
        return patientContactDetails;
    }

    public void setPatientContactDetails(ContactDetailsModel patientContactDetails) {
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

    public ContactDetailsModel getPrimaryRelativeContactDetails() {
        return primaryRelativeContactDetails;
    }

    public void setPrimaryRelativeContactDetails(ContactDetailsModel primaryRelativeContactDetails) {
        this.primaryRelativeContactDetails = primaryRelativeContactDetails;
    }

    public List<ContactDetailsModel> getAdditionalRelativeContactDetails() {
        return additionalRelativeContactDetails;
    }

    public void setAdditionalRelativeContactDetails(List<ContactDetailsModel> additionalRelativeContactDetails) {
        this.additionalRelativeContactDetails = additionalRelativeContactDetails;
    }

	public String getCustomUserId() {
		return customUserId;
	}

	public void setCustomUserId(String customUserId) {
		this.customUserId = customUserId;
	}

	public String getCustomUserName() {
		return customUserName;
	}

	public void setCustomUserName(String customUserName) {
		this.customUserName = customUserName;
	}
}
