package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.model.ContactDetailsModel;
import dk.kvalitetsit.hjemmebehandling.service.model.PatientModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class PatientController {
    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    private PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
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
    @CrossOrigin(origins = "http://localhost:3000") // TODO - cross origin only allowed for development purposes - find a solution that avoids this annotation.
    public @ResponseBody PatientDto getPatient(@RequestBody PatientRequest patientRequest) {
        logger.info("Getting patient ...");

        String clinicalIdentifier = getClinicalIdentifier();

        PatientModel patient = patientService.getPatient(patientRequest.getCpr());

        if(patient == null) {
            throw new ResourceNotFoundException("Patient did not exist!");
        }
        return mapPatient(patient);
    }

    private String getClinicalIdentifier() {
        // TODO - get clinical identifier (from token?)
        return "1234";
    }

    private PatientListResponse buildResponse(List<PatientModel> patients) {
        PatientListResponse response = new PatientListResponse();

        response.setPatients(patients.stream().map(p -> mapPatient(p)).collect(Collectors.toList()));

        return response;
    }

    private PatientDto mapPatient(PatientModel patient) {
        PatientDto patientDto = new PatientDto();

        patientDto.setCpr(patient.getCpr());
        patientDto.setFamilyName(patient.getFamilyName());
        patientDto.setGivenName(patient.getGivenName());
        patientDto.setPatientContactDetails(mapContactDetails(patient.getPatientContactDetails()));

        return patientDto;
    }

    private ContactDetailsDto mapContactDetails(ContactDetailsModel contactDetails) {
        ContactDetailsDto contactDetailsDto = new ContactDetailsDto();

        contactDetailsDto.setCountry(contactDetails.getCountry());
        contactDetailsDto.setEmailAddress(contactDetails.getEmailAddress());
        contactDetailsDto.setPrimaryPhone(contactDetails.getPrimaryPhone());
        contactDetailsDto.setSecondaryPhone(contactDetails.getSecondaryPhone());
        contactDetailsDto.setPostalCode(contactDetails.getPostalCode());
        contactDetailsDto.setStreet(contactDetails.getStreet());

        return contactDetailsDto;
    }
}
