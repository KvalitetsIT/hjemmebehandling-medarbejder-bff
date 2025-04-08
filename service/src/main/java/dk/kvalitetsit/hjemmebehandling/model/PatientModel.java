package dk.kvalitetsit.hjemmebehandling.model;

import java.util.List;

public record PatientModel(
        QualifiedId id,
        String givenName,
        String familyName,
        String cpr,
        ContactDetailsModel contactDetails,
        PrimaryContactModel primaryContact,
        List<ContactDetailsModel> additionalRelativeContactDetails,
        String customUserId,
        String customUserName
) {

    public static Builder builder() {
        return new PatientModel.Builder();
    }

    public static class Builder {
        private QualifiedId id;
        private String givenName;
        private String familyName;
        private String cpr;
        private ContactDetailsModel contactDetails;
        private PrimaryContactModel primaryContactModel;
        private List<ContactDetailsModel> additionalRelativeContactDetails;
        private String customUserId;
        private String customUserName;

        public static Builder from(PatientModel model) {
            return new Builder()
                    .id(model.id)
                    .givenName(model.givenName)
                    .familyName(model.familyName)
                    .cpr(model.cpr)
                    .contactDetails(model.contactDetails)
                    .customUserId(model.customUserId)
                    .customUserName(model.customUserName)
                    .primaryContact(model.primaryContact)
                    .additionalRelativeContactDetails(model.additionalRelativeContactDetails);
        }

        public Builder id(QualifiedId id) {
            this.id = id;
            return this;
        }

        public Builder givenName(String givenName) {
            this.givenName = givenName;
            return this;

        }

        public Builder familyName(String familyName) {
            this.familyName = familyName;
            return this;
        }

        public Builder cpr(String cpr) {
            this.cpr = cpr;
            return this;
        }

        public Builder contactDetails(ContactDetailsModel contactDetails) {
            this.contactDetails = contactDetails;
            return this;
        }

        public Builder primaryContact(PrimaryContactModel primaryContactModel) {
            this.primaryContactModel = primaryContactModel;
            return this;
        }

        public Builder additionalRelativeContactDetails(List<ContactDetailsModel> additionalRelativeContactDetails) {
            this.additionalRelativeContactDetails = additionalRelativeContactDetails;
            return this;
        }

        public Builder customUserId(String customUserId) {
            this.customUserId = customUserId;
            return this;
        }

        public Builder customUserName(String customUserName) {
            this.customUserName = customUserName;
            return this;
        }

        public PatientModel build() {
            return new PatientModel(this.id, this.givenName, this.familyName, this.cpr, this.contactDetails, this.primaryContactModel, this.additionalRelativeContactDetails, this.customUserId, this.customUserName);
        }
    }
}

