package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import org.openapitools.model.UserContext;

import java.util.List;

public class MockContextHandler implements IUserContextHandler {
    private final String orgId;
    private final List<String> entitlements;
    public MockContextHandler(String orgId, List<String> entitlements) {
        this.orgId = orgId;
        this.entitlements = entitlements;
    }

    @Override
	public UserContext mapTokenToUser(FhirClient client, DecodedJWT jwt) {
		var context = new UserContext();

        context.setFullName("Test Testsen");
        context.setFirstName("Test");
        context.setLastName("Testsen");
        context.setOrgId(orgId);
        context.setUserId("TesTes");
        context.setEmail("test@rm.dk");
        context.setEntitlements(entitlements);
        context.setAuthorizationIds(List.of("1234") );

        return context;
	}

}
