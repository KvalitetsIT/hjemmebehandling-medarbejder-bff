package dk.kvalitetsit.hjemmebehandling.model;

public class PatientModel {
    private String givenName;
    private String familyName;
    private String cpr;
    private ContactDetailsModel patientContactDetails;

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
}
