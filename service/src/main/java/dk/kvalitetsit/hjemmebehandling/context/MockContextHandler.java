package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;
import dk.kvalitetsit.hjemmebehandling.fhir.ConcreteFhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.openapitools.model.UserContext;

import java.util.List;
import java.util.Optional;

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

        context.setFullName(Optional.of("Test Testsen"));
        context.setFirstName(Optional.of("Test"));
        context.setLastName(Optional.of("Testsen"));
        context.setOrgId(Optional.ofNullable(orgId));
        context.setUserId(Optional.of("TesTes"));
        context.setEmail(Optional.of("test@rm.dk"));
        context.setEntitlements(entitlements);
        context.setAuthorizationIds(List.of("1234"));

        return context;
    }
}
