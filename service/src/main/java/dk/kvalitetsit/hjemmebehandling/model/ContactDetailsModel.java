package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.api.dto.ContactDetailsDto;
import dk.kvalitetsit.hjemmebehandling.mapping.ToDto;

public class ContactDetailsModel implements ToDto<ContactDetailsDto> {
    private String street;
    private String postalCode;
    private String country;
    private String city;
    private String primaryPhone;
    private String secondaryPhone;

    @Override
    public String toString() {
        return "ContactDetailsModel{" +
                "street='" + street + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", primaryPhone='" + primaryPhone + '\'' +
                ", secondaryPhone='" + secondaryPhone + '\'' +
                '}';
    }

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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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

    @Override
    public ContactDetailsDto toDto() {
        ContactDetailsDto contactDetailsDto = new ContactDetailsDto();

        contactDetailsDto.setCountry(this.getCountry());
        contactDetailsDto.setCity(this.getCity());
        contactDetailsDto.setPrimaryPhone(this.getPrimaryPhone());
        contactDetailsDto.setSecondaryPhone(this.getSecondaryPhone());
        contactDetailsDto.setPostalCode(this.getPostalCode());
        contactDetailsDto.setStreet(this.getStreet());

        return contactDetailsDto;
    }
}
