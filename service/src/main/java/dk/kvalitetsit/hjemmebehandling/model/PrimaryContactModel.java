package dk.kvalitetsit.hjemmebehandling.model;

public record PrimaryContactModel(
        ContactDetailsModel contactDetails,
        String name,
        String affiliation,
        String organisation
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ContactDetailsModel contactDetails;
        private String name;
        private String affiliation;
        private String organisation;

        public Builder contactDetails(ContactDetailsModel contactDetails) {
            this.contactDetails = contactDetails;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder affiliation(String affiliation) {
            this.affiliation = affiliation;
            return this;
        }

        public Builder organisation(String organisation) {
            this.organisation = organisation;
            return this;
        }

        public PrimaryContactModel build() {
            return new PrimaryContactModel(contactDetails, name, affiliation, organisation);
        }
    }
}
