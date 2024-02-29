package dk.kvalitetsit.hjemmebehandling.context;

import java.util.Optional;

import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Organization;

import com.auth0.jwt.interfaces.DecodedJWT;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;

public class DIASUserContextHandler implements IUserContextHandler {

	private static final String FULL_NAME = "FullName";
	private static final String FIRST_NAME = "FirstName";
	private static final String SUR_NAME = "SurName";
	private static final String BSK_DIAS_ENTITLEMENTS = "bSKDIASEntitlements";
	private static final String SOR_ID = "SORID";
	private static final String AUTORISATIONS_IDS = "autorisationsids";
	private static final String REGIONS_ID = "RegionsID";
	private static final String EMAIL = "email";
	private static final String BSK_AUTORISATIONS_INFORMATION = "bSKAutorisationsInformation";

	
	public UserContext mapTokenToUser(FhirClient client, DecodedJWT jwt) throws ServiceException {
		var context = new UserContext();
		if(jwt==null) {
			return context;
		}
        context.setFullName(jwt.getClaim(DIASUserContextHandler.FULL_NAME) !=null ? jwt.getClaim(DIASUserContextHandler.FULL_NAME).asString() : null );
        context.setFirstName(jwt.getClaim(DIASUserContextHandler.FIRST_NAME) !=null ? jwt.getClaim(DIASUserContextHandler.FIRST_NAME).asString() : null );
        context.setLastName(jwt.getClaim(DIASUserContextHandler.SUR_NAME) !=null ? jwt.getClaim(DIASUserContextHandler.SUR_NAME).asString() : null );
        
        // set sorid and lookup name
        if (jwt.getClaim(DIASUserContextHandler.SOR_ID) !=null) {
        	String sorid = jwt.getClaim(DIASUserContextHandler.SOR_ID).asString();
        	context.setOrgId(sorid);
        	Optional<Organization> organization = client.lookupOrganizationBySorCode(sorid);
            organization.ifPresent(value -> context.setOrgName(value.getName()));
        }
        
        context.setUserId(jwt.getClaim(DIASUserContextHandler.REGIONS_ID) !=null ? jwt.getClaim(DIASUserContextHandler.REGIONS_ID).asString() : null );
        context.setEmail(jwt.getClaim(DIASUserContextHandler.EMAIL) !=null ? jwt.getClaim(DIASUserContextHandler.EMAIL).asString() : null );
        context.setEntitlements(jwt.getClaim(DIASUserContextHandler.BSK_DIAS_ENTITLEMENTS) !=null ? jwt.getClaim(DIASUserContextHandler.BSK_DIAS_ENTITLEMENTS).asArray(String.class) : null );
        context.setAuthorizationIds(jwt.getClaim(DIASUserContextHandler.AUTORISATIONS_IDS) !=null ? jwt.getClaim(DIASUserContextHandler.AUTORISATIONS_IDS).asArray(String.class) : null );
		
        
        
        return context;
	}
	
}
