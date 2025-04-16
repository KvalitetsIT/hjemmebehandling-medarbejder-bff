package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;
import dk.kvalitetsit.hjemmebehandling.fhir.client.FhirClient;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.openapitools.model.UserContext;

public interface IUserContextHandler {

    UserContext mapTokenToUser(FhirClient client, DecodedJWT jwt) throws ServiceException;
}
