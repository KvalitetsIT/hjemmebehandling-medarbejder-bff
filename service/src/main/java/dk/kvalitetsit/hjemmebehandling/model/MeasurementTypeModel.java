package dk.kvalitetsit.hjemmebehandling.model;

public record MeasurementTypeModel(
        String system,
        String code,
        String display
        ) {
    public static MeasurementTypeModel.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String system;
        private String code;
        private String display;

        public Builder system(String system) {
            this.system = system;
            return this;
        }
        public Builder code(String code) {
            this.code = code;return this;
        }
        public Builder display(String display) {
            this.display = display;return this;
        }

        public MeasurementTypeModel build() {
            return new MeasurementTypeModel(this.system, this.code, this.display);
        }
    }
}
