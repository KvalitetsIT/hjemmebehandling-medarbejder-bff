package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class UserContextInterceptor implements HandlerInterceptor {

    private static final String BEARER = "Bearer";

    private final IUserContextHandler contextHandler;
    private final UserContextProvider userContextProvider;
    private final FhirClient client;

    public UserContextInterceptor(FhirClient client, UserContextProvider userContextProvider, IUserContextHandler userContextHandler) {
        this.client = client;
        this.userContextProvider = userContextProvider;
        this.contextHandler = userContextHandler;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        DecodedJWT jwt = null;

        // get authorizationheader.Jwt token could/should be cached.
        String autHeader = request.getHeader("authorization");
        if (autHeader != null) {
            String[] token = autHeader.split(" ");
            if (token[0] != null && BEARER.equals(token[0])) {
                //Removes "Bearer"
                jwt = JWT.decode(token[1]);
                //We should verify bearer token
            }
        }

        if (jwt != null) System.out.println(jwt);


        userContextProvider.setUserContext(contextHandler.mapTokenToUser(client, jwt));

        return true;
    }

    @Override
    public void postHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, ModelAndView modelAndView) throws Exception {

    }
}
