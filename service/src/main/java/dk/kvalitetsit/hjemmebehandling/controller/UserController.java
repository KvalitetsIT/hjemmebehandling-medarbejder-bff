package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import org.openapitools.api.UserApi;
import org.openapitools.model.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class UserController extends BaseController implements UserApi {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserContextProvider userContextProvider;

    public UserController(UserContextProvider userContextProvider) {
        this.userContextProvider = userContextProvider;
    }

    @Override
    public ResponseEntity<UserContext> getUser() {
        logger.info("Getting user context information");
        return ResponseEntity.ok(userContextProvider.getUserContext());
    }

}
