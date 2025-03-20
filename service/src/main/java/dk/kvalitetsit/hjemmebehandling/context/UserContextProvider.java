package dk.kvalitetsit.hjemmebehandling.context;


import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.openapitools.model.UserContext;
@Component
@RequestScope
public class UserContextProvider {
    private UserContext context;

    public UserContext getUserContext() {
        return context;
    }

    public void setUserContext(UserContext context) {
        this.context = context;
        
    }
}
