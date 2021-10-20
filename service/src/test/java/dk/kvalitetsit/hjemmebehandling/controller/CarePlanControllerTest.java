package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.CarePlanDto;
import dk.kvalitetsit.hjemmebehandling.api.CreateCarePlanRequest;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
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
        request.setCpr("0101010101");
        request.setPlanDefinitionId("plandefinition-1");

        // Act
        ResponseEntity<?> result = subject.createCarePlan(request);

        // Assert
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    public void createCarePlan_success_setsLocationHeader() throws Exception {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCpr("0101010101");
        request.setPlanDefinitionId("plandefinition-1");

        Mockito.when(carePlanService.createCarePlan(request.getCpr(), request.getPlanDefinitionId())).thenReturn("careplan-1");

        String location = "http://localhost:8080/api/v1/careplan/careplan-1";
        Mockito.when(locationHeaderBuilder.buildLocationHeader("careplan-1")).thenReturn(URI.create(location));

        // Act
        ResponseEntity<?> result = subject.createCarePlan(request);

        // Assert
        assertNotNull(result.getHeaders().get("Location"));
        assertEquals(location, result.getHeaders().get("Location").get(0));
    }

    @Test
    public void createCarePlan_failure_500() throws Exception {
        // Arrange
        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCpr("0101010101");
        request.setPlanDefinitionId("plandefinition-1");

        Mockito.when(carePlanService.createCarePlan(request.getCpr(), request.getPlanDefinitionId())).thenThrow(ServiceException.class);

        // Act

        // Assert
        assertThrows(InternalServerErrorException.class, () -> subject.createCarePlan(request));
    }

    @Test
    public void getCarePlan_carePlanPresent_200() {
        // Arrange
        String carePlanId = "careplan-1";

        CarePlanModel carePlanModel = new CarePlanModel();
        CarePlanDto carePlanDto = new CarePlanDto();
        Mockito.when(carePlanService.getCarePlan(carePlanId)).thenReturn(Optional.of(carePlanModel));
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlanDto);

        // Act
        CarePlanDto result = subject.getCarePlan(carePlanId);

        // Assert
        assertEquals(carePlanDto, result);
    }

    @Test
    public void getCarePlan_carePlanMissing_404() {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.when(carePlanService.getCarePlan(carePlanId)).thenReturn(Optional.empty());
        // Act

        // Assert
        assertThrows(ResourceNotFoundException.class, () -> subject.getCarePlan(carePlanId));
    }
}