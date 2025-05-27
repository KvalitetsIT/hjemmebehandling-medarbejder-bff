package dk.kvalitetsit.hjemmebehandling.model;


import java.util.Optional;

public record PractitionerModel(
        QualifiedId.PractitionerId id,
        String givenName,
        String familyName
) implements BaseModel<PractitionerModel> {
    public static PractitionerModel.Builder builder() {
        return new Builder();
    }

    @Override
    public QualifiedId.OrganizationId organizationId() {
        return null;
    }

    @Override
    public PractitionerModel substitute(PractitionerModel other) {
        return new PractitionerModel(
                Optional.ofNullable(other.id).orElse(id),
                Optional.ofNullable(other.givenName).orElse(givenName),
                Optional.ofNullable(other.familyName).orElse(familyName)
        );
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
