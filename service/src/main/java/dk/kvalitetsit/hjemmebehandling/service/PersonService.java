package dk.kvalitetsit.hjemmebehandling.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.model.PersonModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

public class PersonService {
    private static final Logger logger = LoggerFactory.getLogger(PersonService.class);
    private final RestTemplate restTemplate;
    @Value("${cpr.url}")
    private String cprUrl;


    public PersonService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // http://localhost:8080/api/v1/person?cpr=2512489996
    public PersonModel getPerson(String cpr) throws JsonProcessingException, ServiceException {
        try {
            String result = restTemplate.getForObject(cprUrl + cpr, String.class);
            return new ObjectMapper().readValue(result, PersonModel.class);
        } catch (HttpClientErrorException httpClientErrorException) {
            httpClientErrorException.printStackTrace();
            if (HttpStatus.NOT_FOUND.equals(httpClientErrorException.getStatusCode())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "entity not found");
            }
            throw new ServiceException("Could not fetch person from cpr-service", ErrorKind.BAD_GATEWAY, ErrorDetails.CPRSERVICE_UNKOWN_ERROR);
        }
    }
}


;


