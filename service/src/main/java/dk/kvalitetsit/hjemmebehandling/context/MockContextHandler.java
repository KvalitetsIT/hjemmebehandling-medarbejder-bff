package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;

public class MockContextHandler implements IUserContextHandler {
    private String orgId;

    public MockContextHandler(String orgId) {
        this.orgId = orgId;
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
        context.setEntitlements(new String[]{"DIAS_KoMo_Laege","DIAS_KoMo_Sygeplejerske","DIAS_KoMo_Administrator"} );
        context.setAutorisationsids(new String[]{"1234"} );
		
        return context;
	}

}
