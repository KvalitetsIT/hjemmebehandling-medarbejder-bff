package dk.kvalitetsit.hjemmebehandling.service;


import java.util.Optional;

import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.kvalitetsit.hjemmebehandling.api.CustomUserRequestDto;
import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;

public class CustomUserService {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserService.class);

	@Value("${patientidp.api.url}")
	private String patientidpApiUrl;
    
	private RestTemplate restTemplate;

	private final ObjectMapper mapper = new ObjectMapper();

	private FhirClient fhirClient;

	private FhirMapper fhirMapper;
	

    public CustomUserService(RestTemplate restTemplate, FhirClient client, FhirMapper mapper) {
    	this.restTemplate = restTemplate;
		this.fhirClient = client;
		this.fhirMapper = mapper;
    }

    public Optional<CustomUserResponseDto> createUser(CustomUserRequestDto userCreateRequest) throws JsonMappingException, JsonProcessingException {
    	if(patientidpApiUrl==null || patientidpApiUrl.equals("")) {
    		logger.info("The custom url: patientidp.api.url i not set. User not created in customuser");
    		return Optional.ofNullable(null);
    	}
    	// Init headers
    	HttpHeaders headers = new HttpHeaders();
    	headers.setContentType(MediaType.APPLICATION_JSON);
    	// create request
    	String jsonArg = mapper.writeValueAsString(userCreateRequest);
    	HttpEntity<String> request = new HttpEntity<String>(jsonArg,headers);
    	// Send request
    	CustomUserResponseDto userCreated  = restTemplate.postForObject(patientidpApiUrl,request, CustomUserResponseDto.class);
        
    	return Optional.ofNullable(userCreated);
    }
    
    
    // http://localhost:8080/api/v1/resetpassword
    public Optional<PatientModel> resetPassword(CustomUserRequestDto userCreateRequest) throws JsonMappingException, JsonProcessingException {
    	if("".equals(patientidpApiUrl)) {
    		logger.info("patientidpApiUrl is null. Cannot reset password");
    		return Optional.empty();
    	}
    	// Find patient
    	Optional<Patient> patient = fhirClient.lookupPatientByCpr(userCreateRequest.getAttributes().getCpr());
    	if(patient.isEmpty()) {
    		logger.info("resetPassword: Can not find patient");
    		return Optional.empty();
    	}
    	// Map patient
    	PatientModel patientModel = fhirMapper.mapPatient(patient.get());
    	String customUserLoginId = patientModel.getCustomUserId();
    	if(customUserLoginId == null || customUserLoginId.equals("")) {
    		logger.info("No custom user login set on patient");
    	} else {
    		// init headers
    		HttpHeaders headers = new HttpHeaders();
    		headers.setContentType(MediaType.APPLICATION_JSON);
    		// create request
    		userCreateRequest.setAttributes(null);
    		String jsonArg = mapper.writeValueAsString(userCreateRequest);
    		HttpEntity<String> request = new HttpEntity<String>(jsonArg,headers);
    		// send request
    		restTemplate.put(patientidpApiUrl+"/"+customUserLoginId+"/reset-password",request);
    	}
			return Optional.of(patientModel);
    }
}
