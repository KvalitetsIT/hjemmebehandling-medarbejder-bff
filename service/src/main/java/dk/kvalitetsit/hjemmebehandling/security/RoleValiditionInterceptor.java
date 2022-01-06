package dk.kvalitetsit.hjemmebehandling.security;

import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.controller.exception.UnauthorizedException;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;

public class RoleValiditionInterceptor implements HandlerInterceptor {

	private UserContextProvider userContextProvider;
	private List<String> allowedRoles;
	
    public RoleValiditionInterceptor(UserContextProvider userContextProvider, String allowedRoles) {
		this.userContextProvider = userContextProvider;
        
        this.allowedRoles = parseRoles(allowedRoles);
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    	
    	String[] userEntitlements = userContextProvider.getUserContext().getEntitlements();
    	boolean hasPermission = false;
    	
    	for (int i = 0; i < userEntitlements.length; i++) {
			if (allowedRoles.contains(userEntitlements[i])) {
				hasPermission = true;
				break;
			}
		}
    	if (!hasPermission) {
    		throw new UnauthorizedException("The user does not have the correct permissions");
    	}
    	
        return true;
    }

    private List<String> parseRoles(String str) {
        return Collections.list(new StringTokenizer(str, ",")).stream()
        	      .map(token -> ((String) token).trim())
        	      .collect(Collectors.toList());
	}

	@Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }
	
}
