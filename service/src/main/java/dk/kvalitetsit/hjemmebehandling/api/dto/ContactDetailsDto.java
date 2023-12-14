package dk.kvalitetsit.hjemmebehandling.api.dto;

import dk.kvalitetsit.hjemmebehandling.mapping.Dto;
import dk.kvalitetsit.hjemmebehandling.model.ContactDetailsModel;

public class ContactDetailsDto implements Dto<ContactDetailsModel> {
    private String street;
    private String postalCode;
    private String country;
    private String city;
    private String primaryPhone;
    private String secondaryPhone;

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPrimaryPhone() {
        return primaryPhone;
    }

    public void setPrimaryPhone(String primaryPhone) {
        this.primaryPhone = primaryPhone;
    }

    public String getSecondaryPhone() {
        return secondaryPhone;
    }

    public void setSecondaryPhone(String secondaryPhone) {
        this.secondaryPhone = secondaryPhone;
    }

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

    @Override
    public ContactDetailsModel toModel() {
        ContactDetailsModel contactDetailsModel = new ContactDetailsModel();

        contactDetailsModel.setCountry(this.getCountry());
        contactDetailsModel.setCity(this.getCity());
        contactDetailsModel.setPrimaryPhone(this.getPrimaryPhone());
        contactDetailsModel.setSecondaryPhone(this.getSecondaryPhone());
        contactDetailsModel.setPostalCode(this.getPostalCode());
        contactDetailsModel.setStreet(this.getStreet());

        return contactDetailsModel;
    }
}
