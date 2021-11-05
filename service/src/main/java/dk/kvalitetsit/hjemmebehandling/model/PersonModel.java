package dk.kvalitetsit.hjemmebehandling.model;

public class PersonModel {
    private String resourceType;
    private PersonIdentifierModel identifier;
    private boolean active;
    private PersonNameModel name;
    private String gender;
    private String birthDate;
    private boolean deceasedBoolean;
    private PersonAddressModel address;
    
	public String getResourceType() {
		return resourceType;
	}
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}
	public PersonIdentifierModel getIdentifier() {
		return identifier;
	}
	public void setIdentifier(PersonIdentifierModel identifier) {
		this.identifier = identifier;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public PersonNameModel getName() {
		return name;
	}
	public void setName(PersonNameModel name) {
		this.name = name;
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
	public PersonAddressModel getAddress() {
		return address;
	}
	public void setAddress(PersonAddressModel address) {
		this.address = address;
	}

   
}
