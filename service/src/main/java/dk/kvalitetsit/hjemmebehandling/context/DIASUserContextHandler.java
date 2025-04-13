package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import dk.kvalitetsit.hjemmebehandling.fhir.ConcreteFhirClient;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Organization;
import org.openapitools.model.UserContext;

import java.util.List;
import java.util.Optional;

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


    @Override
    public UserContext mapTokenToUser(ConcreteFhirClient client, DecodedJWT jwt) throws ServiceException {
        var context = new UserContext();
        if (jwt == null) {
            return context;
        }
        context.setFullName(Optional.ofNullable(jwt.getClaim(DIASUserContextHandler.FULL_NAME)).map(Object::toString));
        context.setFirstName(Optional.ofNullable(jwt.getClaim(DIASUserContextHandler.FIRST_NAME)).map(Object::toString));
        context.setLastName(Optional.ofNullable(jwt.getClaim(DIASUserContextHandler.SUR_NAME)).map(Object::toString));

        // set sorid and lookup name
        if (jwt.getClaim(DIASUserContextHandler.SOR_ID) != null) {
            String sorid = jwt.getClaim(DIASUserContextHandler.SOR_ID).asString();
            context.setOrgId(Optional.ofNullable(sorid));
            Optional<Organization> organization = client.lookupOrganizationBySorCode(sorid);
            organization.ifPresent(value -> context.setOrgName(Optional.ofNullable(value.getName())));
        }

        context.setUserId(Optional.of(jwt.getClaim(DIASUserContextHandler.REGIONS_ID)).map(Claim::asString));
        context.setEmail(Optional.of(jwt.getClaim(DIASUserContextHandler.EMAIL)).map(Claim::asString));

        context.setEntitlements(jwt.getClaim(DIASUserContextHandler.BSK_DIAS_ENTITLEMENTS) != null ? List.of(jwt.getClaim(DIASUserContextHandler.BSK_DIAS_ENTITLEMENTS).toString()) : null);
        context.setAuthorizationIds(jwt.getClaim(DIASUserContextHandler.AUTORISATIONS_IDS) != null ? List.of(jwt.getClaim(DIASUserContextHandler.AUTORISATIONS_IDS).toString()) : null);


        return context;
    }

}
