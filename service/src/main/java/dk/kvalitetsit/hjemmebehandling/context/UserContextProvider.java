package dk.kvalitetsit.hjemmebehandling.context;


import dk.kvalitetsit.hjemmebehandling.model.UserContextModel;
import org.openapitools.model.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class UserContextProvider {

    //TODO: May be an optional
    private UserContextModel context;

    public UserContextModel getUserContext() {
        return context;
    }

    public void setUserContext(UserContextModel context) {
        this.context = context;

    }
}
