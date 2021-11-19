package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.CarePlanDto;
import dk.kvalitetsit.hjemmebehandling.api.CreateCarePlanRequest;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
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
    private DtoMapper dtoMapper;

    @Mock
    private LocationHeaderBuilder locationHeaderBuilder;

    private static final String REQUEST_URI = "http://localhost:8080";

    @Test
    public void createCarePlan_success_201() {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        // Act
        ResponseEntity<?> result = subject.createCarePlan(request);

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
        ResponseEntity<?> result = subject.createCarePlan(request);

        // Assert
        assertNotNull(result.getHeaders().get("Location"));
        assertEquals(location, result.getHeaders().get("Location").get(0));
    }

    @Test
    public void createCarePlan_accessViolation_400() throws Exception {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(carePlanModel);

        Mockito.when(carePlanService.createCarePlan(carePlanModel)).thenThrow(AccessValidationException.class);

        // Act
        ResponseEntity<Void> result = subject.createCarePlan(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    public void createCarePlan_failure_500() throws Exception {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(carePlanModel);

        Mockito.when(carePlanService.createCarePlan(carePlanModel)).thenThrow(ServiceException.class);

        // Act
        ResponseEntity<Void> result = subject.createCarePlan(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
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
        ResponseEntity<CarePlanDto> result = subject.getCarePlanById(carePlanId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void getCarePlanById_accessViolation_403() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.doThrow(AccessValidationException.class).when(carePlanService).getCarePlanById(carePlanId);

        // Act
        ResponseEntity<CarePlanDto> result = subject.getCarePlanById(carePlanId);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    public void getCarePlanById_failure_500() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.doThrow(ServiceException.class).when(carePlanService).getCarePlanById(carePlanId);

        // Act
        ResponseEntity<CarePlanDto> result = subject.getCarePlanById(carePlanId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    public void getCarePlansByCpr_parameterMissing_400() {
        // Arrange
        Optional<String> cpr = Optional.empty();

        // Act
        ResponseEntity<List<CarePlanDto>> result = subject.getCarePlansByCpr(cpr);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    public void getCarePlansByCpr_carePlansPresent_200() throws Exception {
        // Arrange
        Optional<String> cpr = Optional.of("0101010101");

        CarePlanModel carePlanModel1 = new CarePlanModel();
        CarePlanModel carePlanModel2 = new CarePlanModel();
        CarePlanDto carePlanDto1 = new CarePlanDto();
        CarePlanDto carePlanDto2 = new CarePlanDto();

        Mockito.when(carePlanService.getCarePlansByCpr("0101010101")).thenReturn(List.of(carePlanModel1, carePlanModel2));
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel1)).thenReturn(carePlanDto1);
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel2)).thenReturn(carePlanDto2);

        // Act
        ResponseEntity<List<CarePlanDto>> result = subject.getCarePlansByCpr(cpr);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(carePlanDto1));
        assertTrue(result.getBody().contains(carePlanDto2));
    }

    @Test
    public void getCarePlansByCpr_carePlansMissing_204() throws Exception {
        // Arrange
        Optional<String> cpr = Optional.of("0101010101");

        Mockito.when(carePlanService.getCarePlansByCpr("0101010101")).thenReturn(List.of());

        // Act
        ResponseEntity<List<CarePlanDto>> result = subject.getCarePlansByCpr(cpr);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    public void getCarePlansByCpr_failureToFetch_500() throws Exception {
        // Arrange
        Optional<String> cpr = Optional.of("0101010101");

        Mockito.when(carePlanService.getCarePlansByCpr("0101010101")).thenThrow(ServiceException.class);

        // Act
        ResponseEntity<List<CarePlanDto>> result = subject.getCarePlansByCpr(cpr);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }
}