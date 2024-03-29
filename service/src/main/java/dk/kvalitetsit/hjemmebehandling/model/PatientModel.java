package dk.kvalitetsit.hjemmebehandling.model;

import java.util.List;

public class PatientModel {
    private QualifiedId id;
    private String givenName;
    private String familyName;
    private String cpr;
    private ContactDetailsModel contactDetails;
    private PrimaryContactModel primaryContactModel;
    private List<ContactDetailsModel> additionalRelativeContactDetails;
    private String customUserId;
    private String customUserName;

    @Override
    public String toString() {
        return "PatientModel{" +
                "id=" + id +
                ", givenName='" + givenName + '\'' +
                ", familyName='" + familyName + '\'' +
                ", cpr='" + cpr + '\'' +
                ", contactDetails=" + contactDetails +
                ", primaryContactMdeol=" + primaryContactModel +
                ", additionalRelativeContactDetails=" + additionalRelativeContactDetails +
                ", customUserId='" + customUserId + '\'' +
                ", customUserName='" + customUserName + '\'' +
                '}';
    }

    public PatientModel() {
        this.primaryContactModel = new PrimaryContactModel();
    }

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

    public ContactDetailsModel getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(ContactDetailsModel contactDetails) {
        this.contactDetails = contactDetails;
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

    public PrimaryContactModel getPrimaryContact() {
        return primaryContactModel;
    }

    public void setPrimaryContact(PrimaryContactModel primaryContactModel) {
        this.primaryContactModel = primaryContactModel;
    }
}
