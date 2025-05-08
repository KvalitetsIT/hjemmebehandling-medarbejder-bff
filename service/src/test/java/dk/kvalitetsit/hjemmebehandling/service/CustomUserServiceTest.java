//package dk.kvalitetsit.hjemmebehandling.service;
//
//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.spi.ILoggingEvent;
//import ch.qos.logback.core.read.ListAppender;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import dk.kvalitetsit.hjemmebehandling.api.CustomUserRequestAttributesDto;
//import dk.kvalitetsit.hjemmebehandling.api.CustomUserRequestDto;
//import dk.kvalitetsit.hjemmebehandling.api.CustomUserResponseDto;
//import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
//import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
//import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
//import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
//import static org.hamcrest.MatcherAssert.assertThat;
//import org.hamcrest.core.StringContains;
//import org.hl7.fhir.r4.model.Patient;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpEntity;
//import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
/// /@RestClientTest
//public class CustomUserServiceTest {
//  @InjectMocks
//  private CustomUserClient subject;
//
//  @Mock
//  private RestTemplate restTemplate;
//
//  @Mock
//  private FhirClient fhirClient;
//
//  @Mock
//  private FhirMapper fhirMapper;
//
//  private ListAppender<ILoggingEvent> listAppender;
//
//  @BeforeEach
//  public void setup() {
//    Logger fooLogger = (Logger) LoggerFactory.getLogger(subject.getClass());
//    // create and start a ListAppender
//    listAppender = new ListAppender<>();
//    listAppender.start();
//
//    // add the appender to the logger
//    fooLogger.addAppender(listAppender);
//  }
//
//  @Test
//  public void createUser_patientidpApiUrl_isNull() throws Exception {
//    
//    ReflectionTestUtils.setField(subject,"patientidpApiUrl", null);
//    CustomUserRequestDto dto = new CustomUserRequestDto();
//
//    
//    Optional<CustomUserResponseDto> user = subject.createUser(new CustomUserRequestDto());
//
//    
//    assertEquals(1, listAppender.list.size());
//    assertThat(listAppender.list.get(0).getMessage(), StringContains.containsString("patientidp.api.url is not set"));
//    assertTrue(user.isEmpty());
//  }
//
//  @Test
//  public void createUser_success() throws Exception {
//    
//    final String apiUrl = "http://foo";
//    ReflectionTestUtils.setField(subject,"patientidpApiUrl", apiUrl);
//
//    CustomUserResponseDto dto = new CustomUserResponseDto();
//
//    Mockito.when(restTemplate.postForObject(
//        Mockito.eq(apiUrl),
//        Mockito.any(HttpEntity.class),
//        Mockito.eq(CustomUserResponseDto.class)))
//        .thenReturn(dto);
//
//    
//    Optional<CustomUserResponseDto> user = subject.createUser(new CustomUserRequestDto());
//
//    
//    assertTrue(user.isPresent());
//    assertEquals(dto, user.get());
//  }
//
//  @Test
//  public void resetPassword_patientidpApiUrl_isNull() throws Exception {
//    
//    ReflectionTestUtils.setField(subject,"patientidpApiUrl", "");
//    CustomUserRequestDto dto = new CustomUserRequestDto();
//
//    
//    Optional<PatientModel> patient = subject.resetPassword(new CustomUserRequestDto());
//
//    
//    assertEquals(1, listAppender.list.size());
//    assertThat(listAppender.list.get(0).getMessage(), StringContains.containsString("patientidpApiUrl is null. Cannot reset password"));
//    assertTrue(patient.isEmpty());
//  }
//
//  @Test
//  public void resetPassword_patient_notFound() throws Exception {
//    
//    ReflectionTestUtils.setField(subject,"patientidpApiUrl", "http://foo");
//
//    CustomUserRequestDto mock = Mockito.mock(CustomUserRequestDto.class);
//    Mockito.when(mock.getAttributes()).thenReturn(new CustomUserRequestAttributesDto());
//
//    
//    Optional<PatientModel> patient = subject.resetPassword(mock);
//
//    
//    assertEquals(1, listAppender.list.size());
//    assertThat(listAppender.list.get(0).getMessage(), StringContains.containsString("resetPassword: Can not find patient"));
//    assertTrue(patient.isEmpty());
//  }
//
//  @Test
//  public void resetPassword_customUserLogin_isNull() throws Exception {
//    
//    ReflectionTestUtils.setField(subject,"patientidpApiUrl", "http://foo");
//
//    CustomUserRequestDto requestDto = Mockito.mock(CustomUserRequestDto.class);
//    Mockito.when(requestDto.getAttributes()).thenReturn(new CustomUserRequestAttributesDto());
//
//    //Patient patientMock = Mockito.mock(Patient.class);
//    Mockito.when(fhirClient.lookupPatientByCpr(Mockito.any())).thenReturn(Optional.of(new Patient()));
//
//    PatientModel patientModelMock = Mockito.mock(PatientModel.class);
//    Mockito.when(patientModelMock.getCustomUserId()).thenReturn(null);
//
//    Mockito.when(fhirMapper.mapPatient(Mockito.any(Patient.class))).thenReturn(patientModelMock);
//
//    
//    Optional<PatientModel> patient = subject.resetPassword(requestDto);
//
//    
//    assertEquals(1, listAppender.list.size());
//    assertThat(listAppender.list.get(0).getMessage(), StringContains.containsString("No custom user login set on patient"));
//    assertTrue(patient.isPresent());
//  }
//
//  @Test
//  public void resetPassword_customUserLogin_isEmpty() throws Exception {
//    
//    ReflectionTestUtils.setField(subject,"patientidpApiUrl", "http://foo");
//
//    CustomUserRequestDto requestDto = Mockito.mock(CustomUserRequestDto.class);
//    Mockito.when(requestDto.getAttributes()).thenReturn(new CustomUserRequestAttributesDto());
//
//    //Patient patientMock = Mockito.mock(Patient.class);
//    Mockito.when(fhirClient.lookupPatientByCpr(Mockito.any())).thenReturn(Optional.of(new Patient()));
//
//    PatientModel patientModelMock = Mockito.mock(PatientModel.class);
//    Mockito.when(patientModelMock.getCustomUserId()).thenReturn("");
//
//    Mockito.when(fhirMapper.mapPatient(Mockito.any(Patient.class))).thenReturn(patientModelMock);
//
//    
//    Optional<PatientModel> patient = subject.resetPassword(requestDto);
//
//    
//    assertEquals(1, listAppender.list.size());
//    assertThat(listAppender.list.get(0).getMessage(), StringContains.containsString("No custom user login set on patient"));
//    assertTrue(patient.isPresent());
//  }
//
//  @Test
//  public void resetPassword_success() throws Exception {
//    
//    final String apiUrl = "http://foo";
//    final String customUserLoginId = "customUserId";
//    final String resetUrl = apiUrl + "/" + customUserLoginId + "/reset-password";
//    ReflectionTestUtils.setField(subject,"patientidpApiUrl", apiUrl);
//    ReflectionTestUtils.setField(subject,"mapper", Mockito.mock(ObjectMapper.class));
//
//    CustomUserRequestDto requestDto = Mockito.mock(CustomUserRequestDto.class);
//    Mockito.when(requestDto.getAttributes()).thenReturn(new CustomUserRequestAttributesDto());
//
//    Mockito.when(fhirClient.lookupPatientByCpr(Mockito.any())).thenReturn(Optional.of(new Patient()));
//
//    PatientModel patientModelMock = Mockito.mock(PatientModel.class);
//    Mockito.when(patientModelMock.getCustomUserId()).thenReturn(customUserLoginId);
//
//    Mockito.when(fhirMapper.mapPatient(Mockito.any(Patient.class))).thenReturn(patientModelMock);
//
//    
//    Optional<PatientModel> patient = subject.resetPassword(requestDto);
//
//    
//    assertTrue(patient.isPresent());
//
//    Mockito.verify(restTemplate, Mockito.times(1)).put(
//        Mockito.eq(resetUrl),
//        Mockito.any(HttpEntity.class));
//  }
//}
