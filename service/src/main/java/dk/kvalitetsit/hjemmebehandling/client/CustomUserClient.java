package dk.kvalitetsit.hjemmebehandling.client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kvalitetsit.hjemmebehandling.api.CustomUserRequestDto;
import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

public class CustomUserClient {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserClient.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DtoMapper dtoMapper;
    @Value("${patientidp.api.url}")
    private String patientidpApiUrl;

    public CustomUserClient(RestTemplate restTemplate, DtoMapper dtoMapper) {
        this.dtoMapper = dtoMapper;
        this.restTemplate = restTemplate;
    }

    public Optional<CustomUserResponseDto> createUser(PatientModel patient) throws JsonProcessingException {

        CustomUserRequestDto userCreateRequest = dtoMapper.mapPatientModelToCustomUserRequest(patient);
        if (patientidpApiUrl == null || patientidpApiUrl.isEmpty()) {
            logger.info("The custom url: patientidp.api.url i not set. User not created in customuser");
            return Optional.empty();
        }
        // Init headers
        HttpHeaders headers = new HttpHeaders();
        // create request
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonArg = mapper.writeValueAsString(userCreateRequest);
        HttpEntity<String> request = new HttpEntity<String>(jsonArg, headers);
        // Send request
        CustomUserResponseDto userCreated = restTemplate.postForObject(patientidpApiUrl, request, CustomUserResponseDto.class);

        return Optional.ofNullable(userCreated);
    }


    // http://localhost:8080/api/v1/resetpassword
    public void resetPassword(String cpr, String customUserLoginName) throws JsonProcessingException {
        if ("".equals(patientidpApiUrl)) {
            logger.info("patientidpApiUrl is null. Cannot reset password");
            return;
        }
        if (cpr == null || customUserLoginName == null) {
            logger.info("resetPassword: Can not find cpr or customUserId");
            return;
        }
        // init headers
        CustomUserRequestDto customUserRequestDto = new CustomUserRequestDto();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // create request
        customUserRequestDto.setAttributes(null);
        customUserRequestDto.setTempPassword(cpr.substring(0, 6));

        String jsonArg = mapper.writeValueAsString(customUserRequestDto);
        HttpEntity<String> request = new HttpEntity<String>(jsonArg, headers);
        // send request
        restTemplate.put(patientidpApiUrl + "/" + customUserLoginName + "/reset-password", request);
    }
}
