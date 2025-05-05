package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.UserContextModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.openapitools.model.UserContext;

public interface IUserContextHandler {

    UserContextModel mapTokenToUserContext(FhirClient client, DecodedJWT jwt) throws ServiceException;
}
