package dk.kvalitetsit.hello.controller;

import dk.kvalitetsit.hello.api.PatientDto;
import dk.kvalitetsit.hello.api.PatientListResponse;
import dk.kvalitetsit.hello.service.PatientService;
import dk.kvalitetsit.hello.service.model.Patient;
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

        List<Patient> patients = patientService.getPatients(clinicalIdentifier);

        return buildResponse(patients);
    }

    private String getClinicalIdentifier() {
        // TODO - get clinical identifier (from token?)
        return "1234";
    }

    private PatientListResponse buildResponse(List<Patient> patients) {
        PatientListResponse response = new PatientListResponse();

        response.setPatients(patients.stream().map(p -> mapPatient(p)).collect(Collectors.toList()));

        return response;
    }

    private PatientDto mapPatient(Patient patient) {
        PatientDto patientDto = new PatientDto();

        patientDto.setCpr(patient.getCpr());
        patientDto.setFamilyName(patient.getFamilyName());
        patientDto.setGivenName(patient.getGivenName());

        return patientDto;
    }
}
