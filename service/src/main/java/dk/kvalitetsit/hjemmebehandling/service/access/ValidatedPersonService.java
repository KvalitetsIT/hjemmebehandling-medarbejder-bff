package dk.kvalitetsit.hjemmebehandling.service.access;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kvalitetsit.hjemmebehandling.model.PersonModel;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.PersonService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

public class ValidatedPersonService implements PersonService {

    private final AccessValidator accessValidator;
    private final PatientService service;

    public ValidatedPersonService(AccessValidator accessValidator, PatientService service) {
        this.accessValidator = accessValidator;
        this.service = service;
    }


    @Override
    public PersonModel getPerson(String cpr) throws JsonProcessingException, ServiceException {
        return null;
    }
}
