package dk.kvalitetsit.hjemmebehandling.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.PersonDto;
import dk.kvalitetsit.hjemmebehandling.model.PersonModel;
import dk.kvalitetsit.hjemmebehandling.service.PersonService;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Person", description = "API for manipulating and retrieving information about persons.")
public class PersonController {
    private static final Logger logger = LoggerFactory.getLogger(PersonController.class);

    private PersonService personService;
    private DtoMapper dtoMapper;

    public PersonController(PersonService patientService, DtoMapper dtoMapper) {
        this.personService = patientService;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping(value = "/v1/person")
    public @ResponseBody PersonDto getPerson(@RequestParam("cpr") String cpr) throws JsonMappingException, JsonProcessingException {
        logger.info("Getting person from cpr service");

        PersonModel personModel = personService.getPerson(cpr);

        return dtoMapper.mapPersonModel(personModel);
    }

}
