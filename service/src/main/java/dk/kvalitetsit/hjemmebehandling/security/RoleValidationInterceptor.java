package dk.kvalitetsit.hjemmebehandling.security;


import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.controller.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

public class RoleValidationInterceptor implements HandlerInterceptor {


    private final UserContextProvider userContextProvider;
    private final List<String> allowedRoles;

    public RoleValidationInterceptor(UserContextProvider userContextProvider, List<String> allowedRoles) {
        this.userContextProvider = userContextProvider;
        this.allowedRoles = allowedRoles;
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws UnauthorizedException {
        // Check if the user has any of the allowed roles
        List<String> userEntitlements = userContextProvider.getUserContext().getEntitlements();

        if (userEntitlements == null) throw new UnauthorizedException("The user is not having any entitlements");

        for (String userEntitlement : userEntitlements) {
            if (allowedRoles.contains(userEntitlement)) return true;
        }


        throw new UnauthorizedException("The user does not have the correct permissions");
    }
}
