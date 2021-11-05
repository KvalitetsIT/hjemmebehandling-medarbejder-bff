package dk.kvalitetsit.hjemmebehandling.api;

public class PersonDto {
	private String cpr;
    private String givenName;
    private String familyName;
    private String gender;
    private String birthDate;
    private boolean deceasedBoolean;
    private ContactDetailsDto patientContactDetails;

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

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(String birthDate) {
		this.birthDate = birthDate;
	}

	public boolean isDeceasedBoolean() {
		return deceasedBoolean;
	}

	public void setDeceasedBoolean(boolean deceasedBoolean) {
		this.deceasedBoolean = deceasedBoolean;
	}

}
