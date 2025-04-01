package dk.kvalitetsit.hjemmebehandling.model;

public record PatientDetails(
        String patientPrimaryPhone,
        String patientSecondaryPhone,
        String primaryRelativeName,
        String primaryRelativeAffiliation,
        String primaryRelativePrimaryPhone,
        String primaryRelativeSecondaryPhone
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String patientPrimaryPhone;
        private String patientSecondaryPhone;
        private String primaryRelativeName;
        private String primaryRelativeAffiliation;
        private String primaryRelativePrimaryPhone;
        private String primaryRelativeSecondaryPhone;

        public Builder patientPrimaryPhone(String patientPrimaryPhone) {
            this.patientPrimaryPhone = patientPrimaryPhone;
            return this;
        }

        public Builder patientSecondaryPhone(String patientSecondaryPhone) {
            this.patientSecondaryPhone = patientSecondaryPhone;
            return this;
        }

        public Builder primaryRelativeName(String primaryRelativeName) {
            this.primaryRelativeName = primaryRelativeName;
            return this;
        }

        public Builder primaryRelativeAffiliation(String primaryRelativeAffiliation) {
            this.primaryRelativeAffiliation = primaryRelativeAffiliation;
            return this;
        }

        public Builder primaryRelativePrimaryPhone(String primaryRelativePrimaryPhone) {
            this.primaryRelativePrimaryPhone = primaryRelativePrimaryPhone;
            return this;
        }

        public Builder primaryRelativeSecondaryPhone(String primaryRelativeSecondaryPhone) {
            this.primaryRelativeSecondaryPhone = primaryRelativeSecondaryPhone;
            return this;
        }

        public PatientDetails build() {
            return new PatientDetails(
                    patientPrimaryPhone, patientSecondaryPhone, primaryRelativeName,
                    primaryRelativeAffiliation, primaryRelativePrimaryPhone, primaryRelativeSecondaryPhone
            );
        }
    }
}
