package dk.kvalitetsit.hjemmebehandling.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class PatientService extends AccessValidatingService {
    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;
    
    private DtoMapper dtoMapper;
    
    private CustomUserService customUserService;

    public PatientService(FhirClient fhirClient, FhirMapper fhirMapper, AccessValidator accessValidator, DtoMapper dtoMapper) {
        super(accessValidator);

        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
        this.dtoMapper = dtoMapper;
    }

    public void createPatient(PatientModel patientModel) throws ServiceException {
        try {
        	Optional<CustomUserResponseDto> customUserResponseDto = customUserService.createUser(dtoMapper.mapPatientModelToCustomUserRequest(patientModel));
        	if(customUserResponseDto.isPresent()) {
        		String customerUserLinkId = customUserResponseDto.get().getId();
        		patientModel.setCustomUserId(customerUserLinkId);
        	}
        	fhirClient.savePatient(fhirMapper.mapPatientModel(patientModel));
        }
        catch(Exception e) {
            throw new ServiceException("Error saving patient", e, ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }

    public List<PatientModel> getPatients(String clinicalIdentifier) {
        FhirContext context = FhirContext.forR4();
        IGenericClient client = context.newRestfulGenericClient("http://hapi-server:8080/fhir");

        Bundle bundle = (Bundle) client.search().forResource("Patient").prettyPrint().execute();

//        org.hl7.fhir.r4.model.Patient patient = new org.hl7.fhir.r4.model.Patient();

        PatientModel p = new PatientModel();

        p.setCpr("0101010101");
        p.setFamilyName("Ærtegærde Ømø Ååstrup");
        p.setGivenName("Torgot");

        return List.of(p);
    }

    public PatientModel getPatient(String cpr) {
        // Look up the patient
        Optional<Patient> patient = fhirClient.lookupPatientByCpr(cpr);
        if(!patient.isPresent()) {
            return null;
        }

        // Map to the domain model
        return fhirMapper.mapPatient(patient.get());
    }
}
