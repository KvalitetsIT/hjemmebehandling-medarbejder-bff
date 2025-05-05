package dk.kvalitetsit.hjemmebehandling.model;


public record PractitionerModel(
        QualifiedId.PractitionerId id,
        String givenName,
        String familyName
) {
    public static PractitionerModel.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private QualifiedId.PractitionerId id;
        private String givenName;
        private String familyName;

        public void setId(QualifiedId.PractitionerId id) {
            this.id = id;
        }

        public void givenName(String givenName) {
            this.givenName = givenName;
        }

        public void familyName(String familyName) {
            this.familyName = familyName;
        }

        public PractitionerModel build() {
            return new PractitionerModel(this.id, this.givenName, this.familyName);
        }

    }
}
