package dk.kvalitetsit.hjemmebehandling.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.openapitools.api.PatientApi;
import org.openapitools.model.CreatePatientRequest;
import org.openapitools.model.PatientDto;
import org.openapitools.model.PatientListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class PatientController extends BaseController implements PatientApi {
    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    private final PatientService patientService;
    private final AuditLoggingService auditLoggingService;
    private final DtoMapper dtoMapper;
    private final CustomUserClient customUserClient;

    public PatientController(PatientService patientService, AuditLoggingService auditLoggingService, DtoMapper dtoMapper, CustomUserClient customUserClient) {
        this.patientService = patientService;
        this.auditLoggingService = auditLoggingService;
        this.dtoMapper = dtoMapper;
        this.customUserClient = customUserClient;
    }

    private PatientListResponse buildResponse(List<PatientModel> patients) {
        PatientListResponse response = new PatientListResponse();
        response.setPatients(patients.stream().map(dtoMapper::mapPatientModel).toList());
        return response;
    }

    @Override
    public ResponseEntity<Void> createPatient(CreatePatientRequest createPatientRequest) {
//         Create the patient
        try {
            // todo: handle 'Optional.get()' without 'isPresent()' check below
            PatientModel patient = createPatientRequest.getPatient().map(dtoMapper::mapPatientDto).get();
            patientService.createPatient(patient);
            auditLoggingService.log("POST /v1/patient", patient);

        } catch (ServiceException e) {
            logger.error("Error creating patient", e);
            throw new InternalServerErrorException(ErrorDetails.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PatientDto> getPatient(String cpr) {
        logger.info("Getting patient ...");

        try {
            PatientModel patient = patientService.getPatient(cpr);
            auditLoggingService.log("GET /v1/patient", patient);
            if (patient == null) {
                throw new ResourceNotFoundException("Patient did not exist!", ErrorDetails.PATIENT_DOES_NOT_EXIST);
            }
            return ResponseEntity.ok(dtoMapper.mapPatientModel(patient));
        } catch (ServiceException e) {
            throw toStatusCodeException(e);
        }
    }


    @Override
    public ResponseEntity<org.openapitools.model.PatientListResponse> getPatientList() {
        logger.info("Getting patient list ...");
        auditLoggingService.log("GET /v1/patientlist", List.of());
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<org.openapitools.model.PatientListResponse> getPatients(Boolean includeActive, Boolean includeCompleted, Integer pageNumber, Integer pageSize) {
        logger.info("Getting patient ...");
        if (!includeActive && !includeCompleted) return ResponseEntity.ok(buildResponse(new ArrayList<>()));

        var pagination = new Pagination(pageNumber, pageSize);
        try {
            List<PatientModel> patients = patientService.getPatients(includeActive, includeCompleted, pagination);
            patientService.getPatients(includeActive, includeCompleted);
            auditLoggingService.log("GET /v1/patients", patients);

            return ResponseEntity.ok(buildResponse(patients));
        } catch (ServiceException e) {
            throw toStatusCodeException(e);
        }
    }

    @Override
    public ResponseEntity<Void> resetPassword(String cpr) {
        logger.info("reset password for patient");
        try {
            PatientModel patientModel = patientService.getPatient(cpr);
            customUserClient.resetPassword(cpr, patientModel.customUserName());
            return ResponseEntity.ok().build();
        } catch (ServiceException e) {
            throw toStatusCodeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseEntity<org.openapitools.model.PatientListResponse> searchPatients(String searchString) {
        logger.info("Getting patient ...");
        try {
            List<PatientModel> patients = patientService.searchPatients(List.of(searchString));
            auditLoggingService.log("GET /v1/patient/search", patients);
            return ResponseEntity.ok(buildResponse(patients));
        } catch (ServiceException e) {
            throw toStatusCodeException(e);
        }
    }
}
