package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

public interface IUserContextHandler {

	public UserContext mapTokenToUser(FhirClient client, DecodedJWT jwt) throws ServiceException;
}
