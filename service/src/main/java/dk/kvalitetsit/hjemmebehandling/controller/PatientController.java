package dk.kvalitetsit.hjemmebehandling.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import dk.kvalitetsit.hjemmebehandling.api.CreatePatientRequest;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.api.PatientDto;
import dk.kvalitetsit.hjemmebehandling.api.PatientListResponse;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Patient", description = "API for manipulating and retrieving patients.")
public class PatientController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    private PatientService patientService;
    private AuditLoggingService auditLoggingService;
    private DtoMapper dtoMapper;
	private CustomUserClient customUserClient;

    public PatientController(PatientService patientService, AuditLoggingService auditLoggingService, DtoMapper dtoMapper, CustomUserClient customUserClient) {
        this.patientService = patientService;
        this.auditLoggingService = auditLoggingService;
        this.dtoMapper = dtoMapper;
        this.customUserClient = customUserClient;
    }

    @GetMapping(value = "/v1/patientlist")
    public @ResponseBody PatientListResponse getPatientList() {
        logger.info("Getting patient list ...");

        String clinicalIdentifier = getClinicalIdentifier();

        List<PatientModel> patients = patientService.getPatients(clinicalIdentifier);
        auditLoggingService.log("GET /v1/patientlist", patients);

        return buildResponse(patients);
    }

    @PostMapping(value = "/v1/patient")
    public void createPatient(@RequestBody CreatePatientRequest request) {
        // Create the patient
        try {
            PatientModel patient = dtoMapper.mapPatientDto(request.getPatient());
            patientService.createPatient(patient);
            auditLoggingService.log("POST /v1/patient", patient);
        }
        catch(ServiceException e) {
            logger.error("Error creating patient", e);
            throw new InternalServerErrorException(ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/v1/patient")
    public @ResponseBody PatientDto getPatient(String cpr) {
        logger.info("Getting patient ...");

        String clinicalIdentifier = getClinicalIdentifier();

        PatientModel patient = patientService.getPatient(cpr);
        auditLoggingService.log("GET /v1/patient", patient);

        if(patient == null) {
            throw new ResourceNotFoundException("Patient did not exist!", ErrorDetails.PATIENT_DOES_NOT_EXIST);
        }
        return dtoMapper.mapPatientModel(patient);
    }

    @GetMapping(value = "/v1/patient/search")
    public @ResponseBody PatientListResponse searchPatients(String searchString) {
        logger.info("Getting patient ...");

        List<PatientModel> patients = patientService.searchPatients(List.of(searchString));
        auditLoggingService.log("GET /v1/patient/search", patients);

        return buildResponse(patients);
    }
    
    @PutMapping(value = "/v1/resetpassword")
    public void resetPassword(@RequestParam("cpr") String cpr) throws JsonMappingException, JsonProcessingException {
        logger.info("reset password for patient");
        PatientModel patientModel = patientService.getPatient(cpr);
        customUserClient.resetPassword(cpr, patientModel.getCustomUserId());
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
