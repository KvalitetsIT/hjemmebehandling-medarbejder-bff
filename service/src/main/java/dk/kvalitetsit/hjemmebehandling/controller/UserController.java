package dk.kvalitetsit.hjemmebehandling.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import dk.kvalitetsit.hjemmebehandling.context.UserContext;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "User", description = "API for retrieving information about users.")
public class UserController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserContextProvider userContextProvider;

    public UserController(UserContextProvider userContextProvider) {
        this.userContextProvider = userContextProvider;
    }

    @GetMapping(value = "/v1/user")
    public @ResponseBody UserContext getUser() throws JsonMappingException, JsonProcessingException {
        logger.info("Getting user context information");
        return userContextProvider.getUserContext();
    }

}
