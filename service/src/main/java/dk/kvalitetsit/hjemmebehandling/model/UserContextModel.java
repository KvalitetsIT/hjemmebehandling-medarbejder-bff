package dk.kvalitetsit.hjemmebehandling.model;

import org.openapitools.model.NameDto;

import java.util.List;
import java.util.Optional;

public record UserContextModel(
        Optional<QualifiedId.OrganizationId> orgId,
        Optional<NameDto> name,
        List<String> authorizationIds,
        Optional<String> userId,
        Optional<String> email,
        List<String> entitlements,
        Optional<String> orgName
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

        private QualifiedId.OrganizationId orgId;
        private NameDto name;
        private List<String> authorizationIds;
        private String userId;
        private String email;
        private List<String> entitlements;
        private String orgName;

        public static UserContextModel.Builder from(UserContextModel source) {
            return new Builder()
                    .orgId(source.orgId.orElse(null))
                    .name(source.name.orElse(null))
                    .userId(source.userId.orElse(null))
                    .email(source.email.orElse(null))
                    .orgName(source.orgName.orElse(null))
                    .authorizationIds(source.authorizationIds)
                    .entitlements(source.entitlements);
        }

        public UserContextModel.Builder orgId(QualifiedId.OrganizationId orgId) {
            this.orgId = orgId;
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

        public UserContextModel.Builder orgName(String orgName) {
            this.orgName = orgName;
            return this;
        }

        public UserContextModel build() {
            return new UserContextModel(Optional.ofNullable(orgId), Optional.ofNullable(name), authorizationIds, Optional.ofNullable(userId), Optional.ofNullable(email), entitlements, Optional.ofNullable(orgName));
        }
    }
}
