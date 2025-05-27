package dk.kvalitetsit.hjemmebehandling.security;


import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.controller.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * An implementation of {@link HandlerInterceptor} that performs role-based access control
 * by checking whether the current user has at least one of the allowed roles.
 *
 * <p>This interceptor is typically used to guard specific endpoints or request handlers,
 * ensuring that only users with specific entitlements (roles) can access them.</p>
 */
public class RoleValidationInterceptor implements HandlerInterceptor {

    private final UserContextProvider userContextProvider;
    private final List<String> allowedRoles;

    /**
     * Constructs a new {@code RoleValidationInterceptor}.
     *
     * @param userContextProvider the provider used to retrieve the current user's context, including entitlements
     * @param allowedRoles        the list of roles that are allowed to access the protected resource
     */
    public RoleValidationInterceptor(UserContextProvider userContextProvider, List<String> allowedRoles) {
        this.userContextProvider = userContextProvider;
        this.allowedRoles = allowedRoles;
    }

    /**
     * Checks whether the current user has at least one of the allowed roles before the request is handled.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @param handler  the chosen handler to execute, for type and/or instance evaluation
     * @return {@code true} if the user has permission to proceed, {@code false} otherwise
     * @throws UnauthorizedException if the user has no entitlements or lacks the required role(s)
     */
    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws UnauthorizedException {
        // Check if the user has any of the allowed roles
        List<String> userEntitlements = userContextProvider.getUserContext().entitlements();

        if (userEntitlements == null) throw new UnauthorizedException("The user is not having any entitlements");

        for (String userEntitlement : userEntitlements) {
            if (allowedRoles.contains(userEntitlement)) return true;
        }

        throw new UnauthorizedException("The user does not have the correct permissions");
    }
}
