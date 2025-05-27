package dk.kvalitetsit.hjemmebehandling.model;

import java.util.Arrays;
import java.util.List;

public record PersonNameModel(
        String family,
        List<String> given
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String family;
        private List<String> given;

        public Builder family(String family) {
            this.family = family;
            return this;
        }

        public Builder given(String... given) {
            this.given = Arrays.stream(given).toList();
            return this;
        }

        public PersonNameModel build() {
            return new PersonNameModel(family, given);
        }
    }
}
