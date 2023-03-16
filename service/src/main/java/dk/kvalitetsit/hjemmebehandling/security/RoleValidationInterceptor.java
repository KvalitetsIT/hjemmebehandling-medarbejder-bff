package dk.kvalitetsit.hjemmebehandling.security;


import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.controller.exception.UnauthorizedException;

public class RoleValidationInterceptor implements HandlerInterceptor {


	private final UserContextProvider userContextProvider;
	private final List<String> allowedRoles;
	
    public RoleValidationInterceptor(UserContextProvider userContextProvider, List<String> allowedRoles) {
		this.userContextProvider = userContextProvider;
        this.allowedRoles = allowedRoles;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws UnauthorizedException {
    	String[] userEntitlements = userContextProvider.getUserContext().getEntitlements();

		printRoles(userEntitlements);

		for (String userEntitlement : userEntitlements) {
			if (allowedRoles.contains(userEntitlement)) return true;
		}

		throw new UnauthorizedException("The user does not have the correct permissions");
	}

	private static void printRoles(String[] userEntitlements) {
		System.out.println("Roles:");
		for (String entitlement: userEntitlements) {
			System.out.println("\t" + entitlement);
		}
	}


}
