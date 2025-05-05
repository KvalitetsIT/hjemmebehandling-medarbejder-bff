package dk.kvalitetsit.hjemmebehandling.model;

public record PersonAddressModel(
        String line,
        String city,
        String postalCode,
        String country
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String line;
        private String city;
        private String postalCode;
        private String country;

        public Builder line(String line) {
            this.line = line;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
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

        public PersonAddressModel build() {
            return new PersonAddressModel(line, city, postalCode, country);
        }
    }
}
