package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;

public class DIASUserContextHandler implements IUserContextHandler {

	private static String FULL_NAME = "FullName";
	private static String FIRST_NAME = "FirstName";
	private static String SUR_NAME = "SurName";
	private static String BSK_DIAS_ENTITLEMENTS = "bSKDIASEntitlements";
	private static String SOR_ID = "SORID";
	private static String AUTORISATIONS_IDS = "autorisationsids";
	private static String REGIONS_ID = "RegionsID";
	private static String EMAIL = "email";	
	private static String BSK_AUTORISATIONS_INFORMATION = "bSKAutorisationsInformation";

	
	public UserContext mapTokenToUser(DecodedJWT jwt) {
		var context = new UserContext();
		
        context.setFullName(jwt.getClaim(DIASUserContextHandler.FULL_NAME) !=null ? jwt.getClaim(DIASUserContextHandler.FULL_NAME).asString() : null );
        context.setFirstName(jwt.getClaim(DIASUserContextHandler.FIRST_NAME) !=null ? jwt.getClaim(DIASUserContextHandler.FIRST_NAME).asString() : null );
        context.setLastName(jwt.getClaim(DIASUserContextHandler.SUR_NAME) !=null ? jwt.getClaim(DIASUserContextHandler.SUR_NAME).asString() : null );
        context.setSorCode(jwt.getClaim(DIASUserContextHandler.SOR_ID) !=null ? jwt.getClaim(DIASUserContextHandler.SOR_ID).asString() : null );
        context.setUserID(jwt.getClaim(DIASUserContextHandler.REGIONS_ID) !=null ? jwt.getClaim(DIASUserContextHandler.REGIONS_ID).asString() : null );
        context.setEmail(jwt.getClaim(DIASUserContextHandler.EMAIL) !=null ? jwt.getClaim(DIASUserContextHandler.EMAIL).asString() : null );
        context.setEntitlements(jwt.getClaim(DIASUserContextHandler.BSK_DIAS_ENTITLEMENTS) !=null ? jwt.getClaim(DIASUserContextHandler.BSK_DIAS_ENTITLEMENTS).asArray(String.class) : null );
        context.setAutorisationsids(jwt.getClaim(DIASUserContextHandler.AUTORISATIONS_IDS) !=null ? jwt.getClaim(DIASUserContextHandler.AUTORISATIONS_IDS).asArray(String.class) : null );
		
        return context;
	}
	
}
