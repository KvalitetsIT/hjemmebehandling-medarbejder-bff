package dk.kvalitetsit.hjemmebehandling.model;

public record PersonModel(
        String resourceType,
        PersonIdentifierModel identifier,
        Boolean active,
        PersonNameModel name,
        String gender,
        String birthDate,
        Boolean deceasedBoolean,
        PersonAddressModel address
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String resourceType;
        private PersonIdentifierModel identifier;
        private Boolean active;
        private PersonNameModel name;
        private String gender;
        private String birthDate;
        private Boolean deceasedBoolean;
        private PersonAddressModel address;

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder identifier(PersonIdentifierModel identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        public Builder name(PersonNameModel name) {
            this.name = name;
            return this;
        }

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder birthDate(String birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Builder deceasedBoolean(Boolean deceasedBoolean) {
            this.deceasedBoolean = deceasedBoolean;
            return this;
        }

        public Builder address(PersonAddressModel address) {
            this.address = address;
            return this;
        }

        public PersonModel build() {
            return new PersonModel(resourceType, identifier, active, name, gender, birthDate, deceasedBoolean, address);
        }
    }
}
