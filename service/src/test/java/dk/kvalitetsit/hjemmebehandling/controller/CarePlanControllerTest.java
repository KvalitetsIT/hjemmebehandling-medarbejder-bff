package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.CarePlanDto;
import dk.kvalitetsit.hjemmebehandling.api.CreateCarePlanRequest;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ForbiddenException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
import org.checkerframework.checker.nullness.Opt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.ServletContext;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CarePlanControllerTest {
    @InjectMocks
    private CarePlanController subject;

    @Mock
    private CarePlanService carePlanService;

    @Mock
    private AuditLoggingService auditLoggingService;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private LocationHeaderBuilder locationHeaderBuilder;

    private static final String REQUEST_URI = "http://localhost:8080";

    @Test
    public void createCarePlan_success_201() {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(new CarePlanModel());

        // Act
        ResponseEntity<Void> result = subject.createCarePlan(request);

        // Assert
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    public void createCarePlan_success_setsLocationHeader() throws Exception {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(carePlanModel);
        Mockito.when(carePlanService.createCarePlan(carePlanModel)).thenReturn("careplan-1");

        String location = "http://localhost:8080/api/v1/careplan/careplan-1";
        Mockito.when(locationHeaderBuilder.buildLocationHeader("careplan-1")).thenReturn(URI.create(location));

        // Act
        ResponseEntity<Void> result = subject.createCarePlan(request);

        // Assert
        assertNotNull(result.getHeaders().get("Location"));
        assertEquals(location, result.getHeaders().get("Location").get(0));
    }

    @Test
    public void createCarePlan_accessViolation_403() throws Exception {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(carePlanModel);

        Mockito.when(carePlanService.createCarePlan(carePlanModel)).thenThrow(AccessValidationException.class);

        // Act

        // Assert
        assertThrows(ForbiddenException.class, () -> subject.createCarePlan(request));
    }

    @Test
    public void createCarePlan_badRequest_400() throws Exception {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(carePlanModel);

        Mockito.when(carePlanService.createCarePlan(carePlanModel)).thenThrow(new ServiceException("error", ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_EXISTS));

        // Act

        // Assert
        assertThrows(BadRequestException.class, () -> subject.createCarePlan(request));
    }

    @Test
    public void createCarePlan_failure_500() throws Exception {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(carePlanModel);

        Mockito.when(carePlanService.createCarePlan(carePlanModel)).thenThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR));

        // Act

        // Assert
        assertThrows(InternalServerErrorException.class, () -> subject.createCarePlan(request));
    }

    @Test
    public void getCarePlanById_carePlanPresent_200() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        CarePlanModel carePlanModel = new CarePlanModel();
        CarePlanDto carePlanDto = new CarePlanDto();
        Mockito.when(carePlanService.getCarePlanById(carePlanId)).thenReturn(Optional.of(carePlanModel));
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlanDto);

        // Act
        ResponseEntity<CarePlanDto> result = subject.getCarePlanById(carePlanId);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(carePlanDto, result.getBody());
    }

    @Test
    public void getCarePlanById_carePlanMissing_404() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.when(carePlanService.getCarePlanById(carePlanId)).thenReturn(Optional.empty());

        // Act

        // Assert
        assertThrows(ResourceNotFoundException.class, () -> subject.getCarePlanById(carePlanId));
    }

    @Test
    public void getCarePlanById_accessViolation_403() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.doThrow(AccessValidationException.class).when(carePlanService).getCarePlanById(carePlanId);

        // Act

        // Assert
        assertThrows(ForbiddenException.class, () -> subject.getCarePlanById(carePlanId));
    }

    @Test
    public void getCarePlanById_failure_500() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.doThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR)).when(carePlanService).getCarePlanById(carePlanId);

        // Act

        // Assert
        assertThrows(InternalServerErrorException.class, () -> subject.getCarePlanById(carePlanId));
    }

    @Test
    public void resolveAlarm_success_200() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        //Mockito.doNothing().when(carePlanService).resolveAlarm(carePlanId);
        Mockito.when(carePlanService.resolveAlarm(carePlanId)).thenReturn(new CarePlanModel());

        // Act
        ResponseEntity<Void> result = subject.resolveAlarm(carePlanId);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    public void resolveAlarm_badRequest_400() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.doThrow(new ServiceException("error", ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_EXISTS)).when(carePlanService).resolveAlarm(carePlanId);

        // Act

        // Assert
        assertThrows(BadRequestException.class, () -> subject.resolveAlarm(carePlanId));
    }

    @Test
    public void resolveAlarm_accessViolation_403() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.doThrow(AccessValidationException.class).when(carePlanService).resolveAlarm(carePlanId);

        // Act

        // Assert
        assertThrows(ForbiddenException.class, () -> subject.resolveAlarm(carePlanId));
    }

    @Test
    public void resolveAlarm_failureToUpdate_500() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.doThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR)).when(carePlanService).resolveAlarm(carePlanId);

        // Act

        // Assert
        assertThrows(InternalServerErrorException.class, () -> subject.resolveAlarm(carePlanId));
    }

    @Test
    public void searchCarePlans_badParameterCombination_400() {
        // Arrange
        Optional<String> cpr = Optional.empty();
        Optional<Boolean> onlyUnsatisfiedSchedules = Optional.empty();
        Optional<Boolean> onlyActiveCarePlans = Optional.empty();
        Optional<Integer> pageNumber = Optional.of(1);
        Optional<Integer> pageSize = Optional.of(10);

        // Act

        // Assert
        assertThrows(BadRequestException.class, () -> subject.searchCarePlans(cpr, onlyUnsatisfiedSchedules, onlyActiveCarePlans, pageNumber, pageSize));
    }

    @Test
    public void searchCarePlans_carePlansPresentForCpr_200() throws Exception {
        // Arrange
        Optional<String> cpr = Optional.of("0101010101");
        Optional<Boolean> onlyUnsatisfiedSchedules = Optional.empty();
        Optional<Boolean> onlyActiveCarePlans = Optional.of(true);
        Optional<Integer> pageNumber = Optional.empty();
        Optional<Integer> pageSize = Optional.empty();

        CarePlanModel carePlanModel1 = new CarePlanModel();
        CarePlanModel carePlanModel2 = new CarePlanModel();
        CarePlanDto carePlanDto1 = new CarePlanDto();
        CarePlanDto carePlanDto2 = new CarePlanDto();

        Mockito.when(carePlanService.getCarePlansByCpr("0101010101", onlyActiveCarePlans.get())).thenReturn(List.of(carePlanModel1, carePlanModel2));
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel1)).thenReturn(carePlanDto1);
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel2)).thenReturn(carePlanDto2);

        // Act
        ResponseEntity<List<CarePlanDto>> result = subject.searchCarePlans(cpr, onlyUnsatisfiedSchedules, onlyActiveCarePlans, pageNumber, pageSize);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(carePlanDto1));
        assertTrue(result.getBody().contains(carePlanDto2));
    }

    @Test
    public void searchCarePlans_unsatisfiedCarePlansPresent_200() throws Exception {
        // Arrange
        Optional<String> cpr = Optional.empty();
        Optional<Boolean> onlyUnsatisfiedSchedules = Optional.of(true);
        Optional<Boolean> onlyActiveCarePlans = Optional.of(true);
        Optional<Integer> pageNumber = Optional.of(1);
        Optional<Integer> pageSize = Optional.of(10);
        PageDetails pageDetails = new PageDetails(1, 10);

        CarePlanModel carePlanModel1 = new CarePlanModel();
        CarePlanModel carePlanModel2 = new CarePlanModel();
        CarePlanDto carePlanDto1 = new CarePlanDto();
        CarePlanDto carePlanDto2 = new CarePlanDto();

        Mockito.when(carePlanService.getCarePlansWithUnsatisfiedSchedules(onlyActiveCarePlans.get(), pageDetails)).thenReturn(List.of(carePlanModel1, carePlanModel2));
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel1)).thenReturn(carePlanDto1);
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel2)).thenReturn(carePlanDto2);

        // Act
        ResponseEntity<List<CarePlanDto>> result = subject.searchCarePlans(cpr, onlyUnsatisfiedSchedules, onlyActiveCarePlans, pageNumber, pageSize);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(carePlanDto1));
        assertTrue(result.getBody().contains(carePlanDto2));
    }

    @Test
    public void searchCarePlans_carePlansMissing_200() throws Exception {
        // Arrange
        Optional<String> cpr = Optional.of("0101010101");
        Optional<Boolean> onlyUnsatisfiedSchedules = Optional.empty();
        Optional<Boolean> onlyActiveCarePlans = Optional.of(true);
        Optional<Integer> pageNumber = Optional.empty();
        Optional<Integer> pageSize = Optional.empty();

        Mockito.when(carePlanService.getCarePlansByCpr("0101010101", onlyActiveCarePlans.get())).thenReturn(List.of());

        // Act
        ResponseEntity<List<CarePlanDto>> result = subject.searchCarePlans(cpr, onlyUnsatisfiedSchedules, onlyActiveCarePlans, pageNumber, pageSize);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    public void searchCarePlans_failureToFetch_500() throws Exception {
        // Arrange
        Optional<String> cpr = Optional.of("0101010101");
        Optional<Boolean> onlyUnsatisfiedSchedules = Optional.empty();
        Optional<Boolean> onlyActiveCarePlans = Optional.of(true);
        Optional<Integer> pageNumber = Optional.empty();
        Optional<Integer> pageSize = Optional.empty();

        Mockito.when(carePlanService.getCarePlansByCpr("0101010101", onlyActiveCarePlans.get())).thenThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR));

        // Act

        // Assert
        assertThrows(InternalServerErrorException.class, () -> subject.searchCarePlans(cpr, onlyUnsatisfiedSchedules, onlyActiveCarePlans, pageNumber, pageSize));
    }
}