package dk.kvalitetsit.hjemmebehandling.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kvalitetsit.hjemmebehandling.model.PersonModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

public interface PersonService {
    PersonModel getPerson(String cpr) throws JsonProcessingException, ServiceException;
}
