package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.CreatePatientRequest;
import org.openapitools.model.PatientDto;
import org.openapitools.model.PatientListResponse;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class PatientControllerTest {
    @InjectMocks
    private PatientController subject;

    @Mock
    private PatientService patientService;
    @Mock
    private AuditLoggingService auditLoggingService;
    @Mock
    private DtoMapper dtoMapper;
    @Mock
    private CustomUserClient customUserClient;


    @Test
    public void getPatientList_success_201() {

        CreatePatientRequest request = new CreatePatientRequest();
        request.setPatient(Optional.of(new PatientDto()));

        PatientModel patientModel = new PatientModel();
        Mockito.when(patientService.getPatients(Mockito.anyString())).thenReturn(List.of(patientModel));

        PatientDto patientDto = new PatientDto();
        Mockito.when(dtoMapper.mapPatientModel(patientModel)).thenReturn(patientDto);

        PatientListResponse result = subject.getPatientList().getBody();

        //assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1, Objects.requireNonNull(result).getPatients().size());
        assertEquals(patientDto, result.getPatients().getFirst());
    }

    @Test
    public void createPatient_success_201() {

        CreatePatientRequest request = new CreatePatientRequest();
        request.setPatient(Optional.of(new PatientDto()));

        Mockito.when(dtoMapper.mapPatientDto(request.getPatient().get())).thenReturn(new PatientModel());


        subject.createPatient(request);


        //assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    public void createPatient_error() throws Exception {

        CreatePatientRequest request = new CreatePatientRequest();
        request.setPatient(Optional.of(new PatientDto()));

        PatientModel patientModel = new PatientModel();
        Mockito.when(dtoMapper.mapPatientDto(request.getPatient().get())).thenReturn(patientModel);

        Mockito.doThrow(ServiceException.class).when(patientService).createPatient(patientModel);


        assertThrows(InternalServerErrorException.class, () -> subject.createPatient(request));
    }

    @Test
    public void getPatient_error_notExist() throws ServiceException {

        Mockito.when(patientService.getPatient(Mockito.anyString())).thenReturn(null);


        Exception e = assertThrows(ResourceNotFoundException.class, () -> subject.getPatient(Mockito.anyString()));
    }

    @Test
    public void getPatient_success_201() throws ServiceException {

        PatientModel patientModel = new PatientModel();
        Mockito.when(patientService.getPatient(Mockito.anyString())).thenReturn(patientModel);

        PatientDto patientDto = new PatientDto();
        Mockito.when(dtoMapper.mapPatientModel(patientModel)).thenReturn(patientDto);


        PatientDto result = subject.getPatient(Mockito.anyString()).getBody();


        //assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(patientDto, result);
    }

    @Test
    public void searchPatient() throws ServiceException {

        PatientModel patientModel = new PatientModel();
        Mockito.when(patientService.searchPatients(Mockito.anyList())).thenReturn(List.of(patientModel));
        //Mockito.doReturn(List.of(patientModel)).when(patientService).searchPatients(List.of(Mockito.anyString()));

        PatientDto patientDto = new PatientDto();
        Mockito.when(dtoMapper.mapPatientModel(patientModel)).thenReturn(patientDto);


        PatientListResponse result = subject.searchPatients(Mockito.anyString()).getBody();


        //assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1, Objects.requireNonNull(result).getPatients().size());
        assertEquals(patientDto, result.getPatients().getFirst());
    }
}
