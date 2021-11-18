package dk.kvalitetsit.hjemmebehandling.context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

public class UserContextInterceptor implements HandlerInterceptor {
	
	private static final String DIAS_CONTEXT = "DIAS";

	private UserContextProvider userContextProvider;

	private String contextHandlerName = null;

    public UserContextInterceptor(UserContextProvider userContextProvider, String contextHandlerName) {
        this.userContextProvider = userContextProvider;
		this.contextHandlerName = contextHandlerName;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    	IUserContextHandler contextHandler;
    	DecodedJWT jwt = null;
    	
    	if(DIAS_CONTEXT.equals(contextHandlerName)) {
    		// get authorizationheader, jwt token could be cached.
    		String autHeader = request.getHeader("authorization");
    		//Removes "Bearer"
    		String[] token = autHeader.split(" ");
    		jwt = JWT.decode(token[1]);

    		contextHandler = new DIASUserContextHandler();	
    	} else {
    		contextHandler = new MockContextHandler();	
    	}

        userContextProvider.setUserContext(contextHandler.mapTokenToUser(jwt));

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }
}
