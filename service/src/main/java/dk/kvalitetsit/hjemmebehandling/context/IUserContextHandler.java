package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;
import dk.kvalitetsit.hjemmebehandling.fhir.ConcreteFhirClient;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.openapitools.model.UserContext;

public interface IUserContextHandler {

    UserContext mapTokenToUser(ConcreteFhirClient client, DecodedJWT jwt) throws ServiceException;
}
