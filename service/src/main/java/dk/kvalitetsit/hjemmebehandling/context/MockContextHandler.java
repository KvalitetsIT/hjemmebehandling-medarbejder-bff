package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;

public class MockContextHandler implements IUserContextHandler {

	@Override
	public UserContext mapTokenToUser(DecodedJWT jwt) {
		var context = new UserContext();
		
        context.setFullName("Test Testsen");
        context.setFirstName("Test");
        context.setLastName("Testsen");
        context.setOrgId("453071000016001");
        context.setUserId("TesTes");
        context.setEmail("test@rm.dk");
        context.setEntitlements(new String[]{"DIAS_HJEMMEBEHANDLING_Sygeplejerske"} );
        context.setAutorisationsids(new String[]{"1234"} );
		
        return context;
	}

}
