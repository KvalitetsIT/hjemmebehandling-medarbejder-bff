package dk.kvalitetsit.hjemmebehandling.model;

import org.openapitools.model.NameDto;

import java.util.List;
import java.util.Optional;

public record UserContextModel(
        Optional<OrganizationModel> organization,
        Optional<NameDto> name,
        List<String> authorizationIds,
        Optional<String> userId,
        Optional<String> email,
        List<String> entitlements
) {
    public UserContextModel {
        // Ensure lists are never null
        authorizationIds = Optional.ofNullable(authorizationIds).orElse(List.of());
        entitlements = Optional.ofNullable(entitlements).orElse(List.of());
    }

    public static UserContextModel.Builder builder() {
        return new UserContextModel.Builder();
    }

    public static class Builder {

        private OrganizationModel organization;
        private NameDto name;
        private List<String> authorizationIds;
        private String userId;
        private String email;
        private List<String> entitlements;


        public static UserContextModel.Builder from(UserContextModel source) {
            return new Builder()
                    .organization(source.organization.orElse(null))
                    .name(source.name.orElse(null))
                    .userId(source.userId.orElse(null))
                    .email(source.email.orElse(null))
                    .authorizationIds(source.authorizationIds)
                    .entitlements(source.entitlements);
        }

        public UserContextModel.Builder organization(OrganizationModel organization) {
            this.organization = organization;
            return this;
        }

        public UserContextModel.Builder name(NameDto name) {
            this.name = name;
            return this;
        }

        public UserContextModel.Builder authorizationIds(List<String> authorizationIds) {
            this.authorizationIds = authorizationIds;
            return this;
        }

        public UserContextModel.Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public UserContextModel.Builder email(String email) {
            this.email = email;
            return this;
        }

        public UserContextModel.Builder entitlements(List<String> entitlements) {
            this.entitlements = entitlements;
            return this;
        }




        public UserContextModel build() {
            return new UserContextModel(Optional.ofNullable(organization), Optional.ofNullable(name), authorizationIds, Optional.ofNullable(userId), Optional.ofNullable(email), entitlements);
        }
    }
}
