package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.*;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ForbiddenException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    // Arrange
    CreatePatientRequest request = new CreatePatientRequest();
    request.setPatient(new PatientDto());

    PatientModel patientModel = new PatientModel();
    Mockito.when(patientService.getPatients(Mockito.anyString())).thenReturn(List.of(patientModel));

    PatientDto patientDto = new PatientDto();
    Mockito.when(dtoMapper.mapPatientModel(patientModel)).thenReturn(patientDto);

    // Act
    PatientListResponse result = subject.getPatientList();

    // Assert
    //assertEquals(HttpStatus.CREATED, result.getStatusCode());
    assertEquals(1, result.getPatients().size());
    assertEquals(patientDto, result.getPatients().get(0));
  }

  @Test
  public void createPatient_success_201() {
    // Arrange
    CreatePatientRequest request = new CreatePatientRequest();
    request.setPatient(new PatientDto());

    Mockito.when(dtoMapper.mapPatientDto(request.getPatient())).thenReturn(new PatientModel());

    // Act
    subject.createPatient(request);

    // Assert
    //assertEquals(HttpStatus.CREATED, result.getStatusCode());
  }

  @Test
  public void createPatient_error() throws Exception {
    // Arrange
    CreatePatientRequest request = new CreatePatientRequest();
    request.setPatient(new PatientDto());

    PatientModel patientModel = new PatientModel();
    Mockito.when(dtoMapper.mapPatientDto(request.getPatient())).thenReturn(patientModel);

    Mockito.doThrow(ServiceException.class).when(patientService).createPatient(patientModel);

    // Act

    // Assert
    assertThrows(InternalServerErrorException.class, () -> subject.createPatient(request));
  }

  @Test
  public void getPatient_error_notExist() {
    // Arrange
    Mockito.when(patientService.getPatient(Mockito.anyString())).thenReturn(null);
    // Act

    // Assert
    Exception e = assertThrows(ResourceNotFoundException.class, () -> subject.getPatient(Mockito.anyString()));
  }

  @Test
  public void getPatient_success_201() {
    // Arrange
    PatientModel patientModel = new PatientModel();
    Mockito.when(patientService.getPatient(Mockito.anyString())).thenReturn(patientModel);

    PatientDto patientDto = new PatientDto();
    Mockito.when(dtoMapper.mapPatientModel(patientModel)).thenReturn(patientDto);

    // Act
    PatientDto result = subject.getPatient(Mockito.anyString());

    // Assert
    //assertEquals(HttpStatus.CREATED, result.getStatusCode());
    assertEquals(patientDto, result);
  }

  @Test
  public void searchPatient() {
    // Arrange
    PatientModel patientModel = new PatientModel();
    Mockito.when(patientService.searchPatients(Mockito.anyList())).thenReturn(List.of(patientModel));
    //Mockito.doReturn(List.of(patientModel)).when(patientService).searchPatients(List.of(Mockito.anyString()));

    PatientDto patientDto = new PatientDto();
    Mockito.when(dtoMapper.mapPatientModel(patientModel)).thenReturn(patientDto);

    // Act
    PatientListResponse result = subject.searchPatients(List.of(Mockito.anyString()));

    // Assert
    //assertEquals(HttpStatus.CREATED, result.getStatusCode());
    assertEquals(1, result.getPatients().size());
    assertEquals(patientDto, result.getPatients().get(0));
  }
}
