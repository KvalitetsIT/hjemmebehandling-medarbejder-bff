package dk.kvalitetsit.hjemmebehandling.api;

import java.util.List;

public class PatientDto {
    private String givenName;
    private String familyName;
    private String cpr;
    private ContactDetailDto patientContactDetails;
    private ContactDetailDto primaryRelativeContactDetails;
    private List<ContactDetailDto> additionalRelativeContactDetails;

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

    public ContactDetailDto getPatientContactDetails() {
        return patientContactDetails;
    }

    public void setPatientContactDetails(ContactDetailDto patientContactDetails) {
        this.patientContactDetails = patientContactDetails;
    }

    public ContactDetailDto getPrimaryRelativeContactDetails() {
        return primaryRelativeContactDetails;
    }

    public void setPrimaryRelativeContactDetails(ContactDetailDto primaryRelativeContactDetails) {
        this.primaryRelativeContactDetails = primaryRelativeContactDetails;
    }

    public List<ContactDetailDto> getAdditionalRelativeContactDetails() {
        return additionalRelativeContactDetails;
    }

    public void setAdditionalRelativeContactDetails(List<ContactDetailDto> additionalRelativeContactDetails) {
        this.additionalRelativeContactDetails = additionalRelativeContactDetails;
    }
}
