package dk.kvalitetsit.hjemmebehandling.api;

import java.util.List;

public class PatientDto {
    private String givenName;
    private String familyName;
    private String cpr;
    private ContactDetailsDto patientContactDetails;
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

    public ContactDetailsDto getPrimaryRelativeContactDetails() {
        return primaryRelativeContactDetails;
    }

    public void setPrimaryRelativeContactDetails(ContactDetailsDto primaryRelativeContactDetails) {
        this.primaryRelativeContactDetails = primaryRelativeContactDetails;
    }

    public List<ContactDetailsDto> getAdditionalRelativeContactDetails() {
        return additionalRelativeContactDetails;
    }

    public void setAdditionalRelativeContactDetails(List<ContactDetailsDto> additionalRelativeContactDetails) {
        this.additionalRelativeContactDetails = additionalRelativeContactDetails;
    }
}
