package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.OrganizationModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.UserContextModel;
import org.openapitools.model.NameDto;

import java.util.List;

public class MockContextHandler implements IUserContextHandler {
    private final QualifiedId.OrganizationId orgId;
    private final List<String> entitlements;

    public MockContextHandler(QualifiedId.OrganizationId orgId, List<String> entitlements) {
        this.orgId = orgId;
        this.entitlements = entitlements;
    }

    @Override
    public UserContextModel mapTokenToUserContext(FhirClient client, DecodedJWT jwt) {

        var organization = new OrganizationModel(orgId, null);

        return UserContextModel.builder()
                .name(new NameDto().given("Test").family("Testsen"))
                .organization(organization)
                .userId("TesTes")
                .email("test@rm.dk")
                .entitlements(entitlements)
                .authorizationIds(List.of("1234"))
                .build();
    }
}
