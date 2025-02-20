package dk.kvalitetsit.hjemmebehandling.security;


import java.util.*;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
		// Check if the user has any of the allowed roles 
		String[] userEntitlements = userContextProvider.getUserContext().getEntitlements();

		if ( userEntitlements == null ) throw new UnauthorizedException("The user is not having any entitlements");

		for (String userEntitlement : userEntitlements) {
			if (allowedRoles.contains(userEntitlement)) return true;
		}


		throw new UnauthorizedException("The user does not have the correct permissions");
	}
}
