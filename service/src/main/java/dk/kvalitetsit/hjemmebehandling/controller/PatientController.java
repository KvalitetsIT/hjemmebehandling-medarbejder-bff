package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.model.ContactDetailsModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class PatientController {
    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    private PatientService patientService;
    private DtoMapper dtoMapper;

    public PatientController(PatientService patientService, DtoMapper dtoMapper) {
        this.patientService = patientService;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping(value = "/v1/patientlist")
    @CrossOrigin(origins = "http://localhost:3000") // TODO - cross origin only allowed for development purposes - find a solution that avoids this annotation.
    public @ResponseBody PatientListResponse getPatientList() {
        logger.info("Getting patient list ...");

        String clinicalIdentifier = getClinicalIdentifier();

        List<PatientModel> patients = patientService.getPatients(clinicalIdentifier);

        return buildResponse(patients);
    }

    @PostMapping(value = "/v1/patient")
    public void createPatient(@RequestBody CreatePatientRequest request) {
        // Create the patient
        try {
            patientService.createPatient(dtoMapper.mapPatientDto(request.getPatient()));
        }
        catch(ServiceException e) {
            logger.error("Error creating patient", e);
            throw new InternalServerErrorException();
        }

        if(request.getPlanDefinitionId() != null) {
            // TODO: Create CarePlan based on provided planDefinition
        }
    }

    @GetMapping(value = "/v1/patient")
    @CrossOrigin(origins = "http://localhost:3000") // TODO - cross origin only allowed for development purposes - find a solution that avoids this annotation.
    public @ResponseBody PatientDto getPatient(String cpr) {
        logger.info("Getting patient ...");

        String clinicalIdentifier = getClinicalIdentifier();

        PatientModel patient = patientService.getPatient(cpr);

        if(patient == null) {
            throw new ResourceNotFoundException("Patient did not exist!");
        }
        return dtoMapper.mapPatientModel(patient);
    }

    private String getClinicalIdentifier() {
        // TODO - get clinical identifier (from token?)
        return "1234";
    }

    private PatientListResponse buildResponse(List<PatientModel> patients) {
        PatientListResponse response = new PatientListResponse();

        response.setPatients(patients.stream().map(p -> dtoMapper.mapPatientModel(p)).collect(Collectors.toList()));

        return response;
    }
}
