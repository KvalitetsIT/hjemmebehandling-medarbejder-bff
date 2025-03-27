package dk.kvalitetsit.hjemmebehandling.api;

public class CustomUserRequestDto {

    private String fullName;
    private String firstName;
    private String lastName;
    private String tempPassword;
    private CustomUserRequestAttributesDto attributes;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getTempPassword() {
        return tempPassword;
    }

    public void setTempPassword(String tempPassword) {
        this.tempPassword = tempPassword;
    }

    public CustomUserRequestAttributesDto getAttributes() {
        return attributes;
    }

    public void setAttributes(CustomUserRequestAttributesDto attributes) {
        this.attributes = attributes;
    }


}
