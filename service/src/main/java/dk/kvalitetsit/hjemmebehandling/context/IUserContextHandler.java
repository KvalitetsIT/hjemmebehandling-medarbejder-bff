package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;

public interface IUserContextHandler {

	public UserContext mapTokenToUser(DecodedJWT jwt);
}
