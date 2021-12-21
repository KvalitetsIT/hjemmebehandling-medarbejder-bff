package dk.kvalitetsit.hjemmebehandling.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import dk.kvalitetsit.hjemmebehandling.api.CustomUserRequestAttributesDto;
import dk.kvalitetsit.hjemmebehandling.api.CustomUserRequestDto;
import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.context.UserContext;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.service.CustomUserService;
import dk.kvalitetsit.hjemmebehandling.service.PersonService;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "CustomUser", description = "API for retrieving information about users.")
public class CustomUserController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserController.class);

    private CustomUserService customUserService;
    
    public CustomUserController(CustomUserService customUserService) {
        this.customUserService = customUserService;
    }
       
    @GetMapping(value = "/v1/resetpassword")
    public void resetPassword(@RequestParam("cpr") String cpr) throws JsonMappingException, JsonProcessingException {
        logger.info("reset password for patient");
        CustomUserRequestDto userCreatedRequestModel = new CustomUserRequestDto();
        CustomUserRequestAttributesDto userCreatedRequestModelAttributes = new CustomUserRequestAttributesDto();

        userCreatedRequestModel.setTempPassword(cpr);
        userCreatedRequestModelAttributes.setCpr(cpr);
        userCreatedRequestModel.setAttributes(userCreatedRequestModelAttributes);
        
        customUserService.resetPassword(userCreatedRequestModel);
    }

}
