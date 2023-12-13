package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.api.dto.PatientDto;
import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;

import java.util.List;
import java.util.stream.Collectors;

public class PatientModel implements ToDto<PatientDto> {
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

    @Override
    public PatientDto toDto() {
        PatientDto patientDto = new PatientDto();

        patientDto.setCpr(this.getCpr());
        patientDto.setFamilyName(this.getFamilyName());
        patientDto.setGivenName(this.getGivenName());
        patientDto.setCustomUserName(this.getCustomUserName());
        if(this.getContactDetails() != null) {
            patientDto.setPatientContactDetails(this.getContactDetails().toDto());
        }
        patientDto.setPrimaryRelativeName(this.getPrimaryContact().getName());
        patientDto.setPrimaryRelativeAffiliation(this.getPrimaryContact().getAffiliation());
        if(this.getPrimaryContact().getContactDetails() != null) {
            patientDto.setPrimaryRelativeContactDetails(this.getPrimaryContact().getContactDetails().toDto());
        }
        if(this.getAdditionalRelativeContactDetails() != null) {
            patientDto.setAdditionalRelativeContactDetails(this.getAdditionalRelativeContactDetails().stream().map(ContactDetailsModel::toDto).collect(Collectors.toList()));
        }

        return patientDto;
    }
}
