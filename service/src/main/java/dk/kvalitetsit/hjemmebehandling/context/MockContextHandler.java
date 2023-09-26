package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;

import java.util.ArrayList;
import java.util.List;

public class MockContextHandler implements IUserContextHandler {
    private String orgId;
    private List<String> entitlements;
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
        context.setEntitlements(entitlements.toArray(new String[0]));
        context.setAutorisationsids(new String[]{"1234"} );

        return context;
	}

}
