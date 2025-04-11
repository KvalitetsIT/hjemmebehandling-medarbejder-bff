package dk.kvalitetsit.hjemmebehandling.model;

public record ContactDetailsModel(
        String street,
        String postalCode,
        String country,
        String city,
        String primaryPhone,
        String secondaryPhone
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String street;
        private String postalCode;
        private String country;
        private String city;
        private String primaryPhone;
        private String secondaryPhone;

        public static Builder from(ContactDetailsModel model) {
            if (model == null) return builder();
            return new Builder()
                    .street(model.street)
                    .postalCode(model.postalCode)
                    .country(model.country)
                    .city(model.city)
                    .primaryPhone(model.primaryPhone)
                    .secondaryPhone(model.secondaryPhone);
        }

        public Builder street(String street) {
            this.street = street;
            return this;
        }

        public Builder postalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder primaryPhone(String primaryPhone) {
            this.primaryPhone = primaryPhone;
            return this;
        }

        public Builder secondaryPhone(String secondaryPhone) {
            this.secondaryPhone = secondaryPhone;
            return this;
        }

        public ContactDetailsModel build() {
            return new ContactDetailsModel(street, postalCode, country, city, primaryPhone, secondaryPhone);
        }
    }
}
