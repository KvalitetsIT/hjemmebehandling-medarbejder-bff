package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.api.dto.ContactDetailsDto;
import dk.kvalitetsit.hjemmebehandling.api.dto.PersonDto;
import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;

public class PersonModel implements ToDto<PersonDto> {
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


	@Override
	public PersonDto toDto() {
		PersonDto personDto = new PersonDto();

		personDto.setCpr(this.getIdentifier().getId());
		personDto.setFamilyName(this.getName().getFamily());
		personDto.setGivenName(String.join(" ", this.getName().getGiven()));
		personDto.setBirthDate(this.getBirthDate());
		personDto.setDeceasedBoolean(this.isDeceasedBoolean());
		personDto.setGender(this.getGender());

		personDto.setPatientContactDetails(new ContactDetailsDto());
		personDto.getPatientContactDetails().setCountry(this.getAddress().getCountry());
		personDto.getPatientContactDetails().setPostalCode(this.getAddress().getPostalCode());
		personDto.getPatientContactDetails().setStreet(this.getAddress().getLine());
		personDto.getPatientContactDetails().setCity(this.getAddress().getCity());

		return personDto;
	}
}
