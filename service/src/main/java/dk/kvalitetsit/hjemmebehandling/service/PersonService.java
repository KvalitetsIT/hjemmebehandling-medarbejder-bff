package dk.kvalitetsit.hjemmebehandling.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.kvalitetsit.hjemmebehandling.model.PersonModel;

public class PersonService {
    private static final Logger logger = LoggerFactory.getLogger(PersonService.class);

	@Value("${cpr.url}")
	private String cprUrl;
    
	private RestTemplate restTemplate;
	

    public PersonService(RestTemplate restTemplate) {
    	this.restTemplate = restTemplate;
    }

    // http://localhost:8080/api/v1/person?cpr=2512489996
    public PersonModel getPerson(String cpr) throws JsonMappingException, JsonProcessingException {
        String result = restTemplate.getForObject(cprUrl+cpr, String.class);
        PersonModel person = new ObjectMapper().readValue(result, PersonModel.class);
        return person;
    }
}

