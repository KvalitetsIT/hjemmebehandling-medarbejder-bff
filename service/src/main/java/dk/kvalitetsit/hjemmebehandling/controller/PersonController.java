package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.PersonDto;
import dk.kvalitetsit.hjemmebehandling.model.PersonModel;
import dk.kvalitetsit.hjemmebehandling.service.PersonService;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@Tag(name = "Person", description = "API for manipulating and retrieving information about persons.")
public class PersonController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(PersonController.class);

    private final PersonService personService;
    private final AuditLoggingService auditLoggingService;
    private final DtoMapper dtoMapper;

    public PersonController(PersonService patientService, AuditLoggingService auditLoggingService, DtoMapper dtoMapper) {
        this.personService = patientService;
        this.auditLoggingService = auditLoggingService;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping(value = "/v1/person")
    public @ResponseBody PersonDto getPerson(@RequestParam("cpr") String cpr) throws JsonProcessingException {
        logger.info("Getting person from cpr service");
        try {
            PersonModel personModel = personService.getPerson(cpr);
            auditLoggingService.log("GET /v1/person", personModel);
            return dtoMapper.mapPersonModel(personModel);
        } catch(ServiceException e) {
            logger.error("Error fetching person", e);
            throw toStatusCodeException(e);
        }
    }

}
