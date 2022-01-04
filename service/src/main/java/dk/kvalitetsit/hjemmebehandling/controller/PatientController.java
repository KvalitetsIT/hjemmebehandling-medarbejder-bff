package dk.kvalitetsit.hjemmebehandling.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
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

    @GetMapping(value = "/test")
    public @ResponseBody PatientListResponse test() {
        logger.info("kommer her");
        //Map<String, String> citizenMap = new HashMap();

        PatientModel p = new PatientModel();
        p.setCpr("0101010101");
        p.setGivenName("Test");
        p.setFamilyName("Testersen");
        PatientModel p1 = new PatientModel();
        p1.setCpr("0101010102");
        p1.setGivenName("Test");
        p1.setFamilyName("Testersen");
        PatientModel p2 = new PatientModel();
        p2.setCpr("0101010103");
        p2.setGivenName("Test");
        p2.setFamilyName("Testersen");
        PatientModel p3 = new PatientModel();
        p3.setCpr("0101010104");
        p3.setGivenName("Test");
        p3.setFamilyName("Testersen");
        PatientModel p4 = new PatientModel();
        p4.setCpr("0101010105");
        p4.setGivenName("Test");
        p4.setFamilyName("Testersen");
        PatientModel p5 = new PatientModel();
        p5.setCpr("0101010106");
        p5.setGivenName("Test");
        p5.setFamilyName("Testersen");
        PatientModel p6 = new PatientModel();
        p6.setCpr("0101010107");
        p6.setGivenName("Test");
        p6.setFamilyName("Testersen");
        PatientModel p7 = new PatientModel();
        p7.setCpr("0101010108");
        p7.setGivenName("Test");
        p7.setFamilyName("Testersen");
        PatientModel p8 = new PatientModel();
        p8.setCpr("0101010109");
        p8.setGivenName("Test");
        p8.setFamilyName("Testersen");
        PatientModel p9 = new PatientModel();
        p9.setCpr("0101010110");
        p9.setGivenName("Test");
        p9.setFamilyName("Testersen");
        PatientModel p10 = new PatientModel();
        p10.setCpr("0101010111");
        p10.setGivenName("Test");
        p10.setFamilyName("Testersen");
        PatientModel p11 = new PatientModel();
        p11.setCpr("0101010112");
        p11.setGivenName("Test");
        p11.setFamilyName("Testersen");
        PatientModel p12 = new PatientModel();
        p12.setCpr("0101010113");
        p12.setGivenName("Test");
        p12.setFamilyName("Testersen");
        PatientModel p13 = new PatientModel();
        p13.setCpr("0101010114");
        p13.setGivenName("Test");
        p13.setFamilyName("Testersen");

        List patients = List.of(p,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13);

        auditLoggingService.log(RequestMethod.GET.name() + " /test", patients);
        return new PatientListResponse();
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
