package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.model.CPR;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.logging.AuditLoggingService;
import org.checkerframework.checker.units.qual.C;
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

import static dk.kvalitetsit.hjemmebehandling.service.Constants.CPR_1;
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
    public void createPatient_success_201() {
        CreatePatientRequest request = new CreatePatientRequest();
        request.setPatient(Optional.of(new PatientDto()));

        Mockito.when(dtoMapper.mapPatientDto(request.getPatient().get())).thenReturn(PatientModel.builder().build());

        subject.createPatient(request);
    }

    @Test
    public void createPatient_error() throws Exception {
        CreatePatientRequest request = new CreatePatientRequest();
        request.setPatient(Optional.of(new PatientDto()));

        PatientModel patientModel = PatientModel.builder().build();
        Mockito.when(dtoMapper.mapPatientDto(request.getPatient().get())).thenReturn(patientModel);

        Mockito.doThrow(ServiceException.class).when(patientService).createPatient(patientModel);

        assertThrows(InternalServerErrorException.class, () -> subject.createPatient(request));
    }

    @Test
    public void getPatient_error_notExist() throws ServiceException {
        Mockito.when(patientService.getPatient(Mockito.any(CPR.class))).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> subject.getPatient(CPR_1.toString()));
    }

    @Test
    public void getPatient_error_malformed_CPR() throws ServiceException {
        var malformed_cpr = "010101010101";
        assertThrows(IllegalArgumentException.class, () -> subject.getPatient(malformed_cpr));
    }


    @Test
    public void getPatient_success_201() throws ServiceException {
        PatientModel patientModel = PatientModel.builder().build();
        Mockito.when(patientService.getPatient(Mockito.any(CPR.class))).thenReturn(patientModel);

        PatientDto patientDto = new PatientDto();
        Mockito.when(dtoMapper.mapPatientModel(patientModel)).thenReturn(patientDto);

        PatientDto result = subject.getPatient(CPR_1.toString()).getBody();

        assertEquals(patientDto, result);
    }

    @Test
    public void searchPatient() throws ServiceException, AccessValidationException {
        PatientModel patientModel = PatientModel.builder().build();

        PatientDto patientDto = new PatientDto();
        Mockito.when(dtoMapper.mapPatientModel(patientModel)).thenReturn(patientDto);
        String searchString = "searchString";
        Mockito.when(patientService.searchPatients(List.of(searchString))).thenReturn(List.of(patientModel));
        PatientListResponse result = subject.searchPatients(searchString).getBody();

        assertEquals(1, Objects.requireNonNull(result).getPatients().size());
        assertEquals(patientDto, result.getPatients().getFirst());
    }
}
