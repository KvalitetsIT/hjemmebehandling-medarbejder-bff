package dk.kvalitetsit.hjemmebehandling.model;

public record Option(
         String option,
         String comment
) {
    public static Option.Builder builder() {
        return new Option.Builder();
    }

    public static class Builder {
        private String option;
        private String comment;

        public Builder option(String option) {
            this.option = option;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Option build(){
            return new Option(this.option, this.comment);
        }
    }
}
