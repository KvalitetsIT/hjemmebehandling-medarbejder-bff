package dk.kvalitetsit.hjemmebehandling.model;

public record PersonIdentifierModel(
        String id,
        String system
) {

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder{
        private String id;
        private String system;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder system(String system) {
            this.system = system;
            return this;
        }

        public PersonIdentifierModel build(){
            return new PersonIdentifierModel(id, system);
        }
    }
}
