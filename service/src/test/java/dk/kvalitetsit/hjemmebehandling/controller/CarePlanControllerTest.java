package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.*;
import dk.kvalitetsit.hjemmebehandling.controller.http.LocationHeaderBuilder;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.service.AuditLoggingService;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.CarePlanDto;
import org.openapitools.model.CreateCarePlanRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;

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

        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(new CarePlanModel());


        ResponseEntity<Void> result = subject.createCarePlan(request);


        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    public void ThrowErrorWhenCompleteCareplan_WhenThereAreUnhandledResponses() throws Exception {

        var careplanId = "careplan-1";
        var toThrow = new ServiceException(String.format("Careplan with id %s still has unhandled questionnaire-responses!", "careplan-1"), ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_HAS_UNHANDLED_QUESTIONNAIRERESPONSES);
        Mockito.when(carePlanService.completeCarePlan(careplanId)).thenThrow(toThrow);

        try {
            subject.completeCarePlan(careplanId);
            fail();
        } catch (BadRequestException badRequestException) {
            assertEquals(ErrorDetails.CAREPLAN_HAS_UNHANDLED_QUESTIONNAIRERESPONSES, badRequestException.getErrorDetails());
        }
    }

    @Test
    public void createCarePlan_success_setsLocationHeader() throws Exception {

        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(carePlanModel);
        Mockito.when(carePlanService.createCarePlan(carePlanModel)).thenReturn("careplan-1");

        String location = "http://localhost:8080/api/v1/careplan/careplan-1";
        Mockito.when(locationHeaderBuilder.buildLocationHeader("careplan-1")).thenReturn(URI.create(location));


        ResponseEntity<Void> result = subject.createCarePlan(request);


        assertNotNull(result.getHeaders().get("Location"));
        assertEquals(location, result.getHeaders().get("Location").get(0));
    }

    @Test
    public void createCarePlan_accessViolation_403() throws Exception {

        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(carePlanModel);

        Mockito.when(carePlanService.createCarePlan(carePlanModel)).thenThrow(AccessValidationException.class);


        assertThrows(ForbiddenException.class, () -> subject.createCarePlan(request));
    }

    @Test
    public void createCarePlan_badRequest_400() throws Exception {

        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(carePlanModel);

        Mockito.when(carePlanService.createCarePlan(carePlanModel)).thenThrow(new ServiceException("error", ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_EXISTS));


        assertThrows(BadRequestException.class, () -> subject.createCarePlan(request));
    }

    @Test
    public void createCarePlan_failure_500() throws Exception {

        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(carePlanModel);

        Mockito.when(carePlanService.createCarePlan(carePlanModel)).thenThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR));


        assertThrows(InternalServerErrorException.class, () -> subject.createCarePlan(request));
    }

    @Test
    public void createCarePlan_failure_502() throws Exception {

        CreateCarePlanRequest request = new CreateCarePlanRequest();
        request.setCarePlan(new CarePlanDto());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(dtoMapper.mapCarePlanDto(request.getCarePlan())).thenReturn(carePlanModel);

        Mockito.when(carePlanService.createCarePlan(carePlanModel)).thenThrow(new ServiceException("error", ErrorKind.BAD_GATEWAY, ErrorDetails.CUSTOMLOGIN_UNKNOWN_ERROR));


        assertThrows(BadGatewayException.class, () -> subject.createCarePlan(request));
    }

    @Test
    public void getCarePlanById_carePlanPresent_200() throws Exception {

        String carePlanId = "careplan-1";

        CarePlanModel carePlanModel = new CarePlanModel();
        CarePlanDto carePlanDto = new CarePlanDto();
        Mockito.when(carePlanService.getCarePlanById(carePlanId)).thenReturn(Optional.of(carePlanModel));
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlanDto);


        ResponseEntity<CarePlanDto> result = subject.getCarePlanById(carePlanId);


        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(carePlanDto, result.getBody());
    }

    @Test
    public void getCarePlanById_carePlanMissing_404() throws Exception {

        String carePlanId = "careplan-1";

        Mockito.when(carePlanService.getCarePlanById(carePlanId)).thenReturn(Optional.empty());


        assertThrows(ResourceNotFoundException.class, () -> subject.getCarePlanById(carePlanId));
    }

    @Test
    public void getCarePlanById_accessViolation_403() throws Exception {

        String carePlanId = "careplan-1";

        Mockito.doThrow(AccessValidationException.class).when(carePlanService).getCarePlanById(carePlanId);


        assertThrows(ForbiddenException.class, () -> subject.getCarePlanById(carePlanId));
    }

    @Test
    public void getCarePlanById_failure_500() throws Exception {

        String carePlanId = "careplan-1";

        Mockito.doThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR)).when(carePlanService).getCarePlanById(carePlanId);


        assertThrows(InternalServerErrorException.class, () -> subject.getCarePlanById(carePlanId));
    }

    @Test
    public void resolveAlarm_success_200() throws Exception {

        String carePlanId = "careplan-1";
        String questionnaireId = "questionnaire-1";

        //Mockito.doNothing().when(carePlanService).resolveAlarm(carePlanId);
        Mockito.when(carePlanService.resolveAlarm(carePlanId, questionnaireId)).thenReturn(new CarePlanModel());


        ResponseEntity<Void> result = subject.resolveAlarm(carePlanId, questionnaireId);


        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    public void resolveAlarm_badRequest_400() throws Exception {

        String carePlanId = "careplan-1";
        String questionnaireId = "questionnaire-1";

        Mockito.doThrow(new ServiceException("error", ErrorKind.BAD_REQUEST, ErrorDetails.CAREPLAN_EXISTS)).when(carePlanService).resolveAlarm(carePlanId, questionnaireId);


        assertThrows(BadRequestException.class, () -> subject.resolveAlarm(carePlanId, questionnaireId));
    }

    @Test
    public void resolveAlarm_accessViolation_403() throws Exception {

        String carePlanId = "careplan-1";
        String questionnaireId = "questionnaire-1";

        Mockito.doThrow(AccessValidationException.class).when(carePlanService).resolveAlarm(carePlanId, questionnaireId);


        assertThrows(ForbiddenException.class, () -> subject.resolveAlarm(carePlanId, questionnaireId));
    }

    @Test
    public void resolveAlarm_failureToUpdate_500() throws Exception {

        String carePlanId = "careplan-1";
        String questionnaireId = "questionnaire-1";

        Mockito.doThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR)).when(carePlanService).resolveAlarm(carePlanId, questionnaireId);


        assertThrows(InternalServerErrorException.class, () -> subject.resolveAlarm(carePlanId, questionnaireId));
    }


    private static Stream<Arguments> searchCarePlans_VerifyThatCorrectMethodIsCalled_withUndefinedCPR_200() {
        return Stream.of(
                Arguments.of(null, true, true, true, true),
                Arguments.of(null, true, null, true, false),
                Arguments.of(null, null, true, false, true)
        );
    }


    private static Stream<Arguments> searchCarePlans_VerifyThatCorrectMethodIsCalled_DependingOnTheArguments_200() {
        return Stream.of(
                Arguments.of("0101011234", true, true, true, true),
                Arguments.of("0101011234", true, false, true, false),
                Arguments.of("0101011234", null, null, false, false),
                Arguments.of("0101011234", null, true, false, true)
        );
    }

    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void searchCarePlans_VerifyThatCorrectMethodIsCalled_withUndefinedCPR_200(String cpr, Boolean onlyUnsatisfiedSchedules, Boolean onlyActiveCarePlans, boolean expectedUnsatisfied, boolean expectedOnlyActive) throws ServiceException {
        Pagination pagination = new Pagination(1, 10);
        assertDoesNotThrow(() -> subject.searchCarePlans(Optional.ofNullable(cpr), Optional.ofNullable(onlyUnsatisfiedSchedules), Optional.ofNullable(onlyActiveCarePlans), Optional.of(pagination.getOffset()), Optional.of(pagination.getLimit())));
        Mockito.verify(carePlanService, times(1)).getCarePlansWithFilters(expectedOnlyActive, expectedUnsatisfied, pagination);
    }

    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void searchCarePlans_VerifyThatCorrectMethodIsCalled_DependingOnTheArguments_200(String cpr, Boolean onlyUnsatisfiedSchedules, Boolean onlyActiveCarePlans, boolean expectedUnsatisfied, boolean expectedOnlyActive) throws ServiceException {
        Pagination pagination = new Pagination(1, 10);
        assertDoesNotThrow(() -> subject.searchCarePlans(Optional.ofNullable(cpr), Optional.ofNullable(onlyUnsatisfiedSchedules), Optional.ofNullable(onlyActiveCarePlans), Optional.of(pagination.getOffset()), Optional.of(pagination.getLimit())));
        Mockito.verify(carePlanService, times(1)).getCarePlansWithFilters(cpr, expectedOnlyActive, expectedUnsatisfied, pagination);
    }

    @Test
    public void searchCarePlans_carePlansPresentForCpr_200() throws Exception {

        String cpr = "0101010101";
        Boolean onlyUnsatisfiedSchedules = null;
        boolean onlyActiveCarePlans = true;

        Pagination pagination = new Pagination(1, 10);
        CarePlanModel carePlanModel1 = new CarePlanModel();
        CarePlanModel carePlanModel2 = new CarePlanModel();
        CarePlanDto carePlanDto1 = new CarePlanDto();
        CarePlanDto carePlanDto2 = new CarePlanDto();

        Mockito.when(carePlanService.getCarePlansWithFilters(cpr, onlyActiveCarePlans, false, pagination)).thenReturn(List.of(carePlanModel1, carePlanModel2));
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel1)).thenReturn(carePlanDto1);
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel2)).thenReturn(carePlanDto2);


        ResponseEntity<List<CarePlanDto>> result = subject.searchCarePlans(Optional.of(cpr), Optional.ofNullable(onlyUnsatisfiedSchedules), Optional.of(onlyActiveCarePlans), Optional.of(pagination.getOffset()), Optional.of(pagination.getLimit()));


        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(carePlanDto1));
        assertTrue(result.getBody().contains(carePlanDto2));
    }

    @Test
    public void searchCarePlans_unsatisfiedCarePlansPresent_200() throws Exception {

        boolean onlyUnsatisfiedSchedules = true;
        boolean onlyActiveCarePlans = true;

        Pagination pagination = new Pagination(1, 10);

        CarePlanModel carePlanModel1 = new CarePlanModel();
        CarePlanModel carePlanModel2 = new CarePlanModel();
        CarePlanDto carePlanDto1 = new CarePlanDto();
        CarePlanDto carePlanDto2 = new CarePlanDto();

        Mockito.when(carePlanService.getCarePlansWithFilters("0101010101", onlyActiveCarePlans, onlyUnsatisfiedSchedules, pagination)).thenReturn(List.of(carePlanModel1, carePlanModel2));
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel1)).thenReturn(carePlanDto1);
        Mockito.when(dtoMapper.mapCarePlanModel(carePlanModel2)).thenReturn(carePlanDto2);


        ResponseEntity<List<CarePlanDto>> result = subject.searchCarePlans(Optional.of("0101010101"), Optional.of(onlyUnsatisfiedSchedules), Optional.of(onlyActiveCarePlans), Optional.of(pagination.getOffset()), Optional.of(pagination.getLimit()));


        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(carePlanDto1));
        assertTrue(result.getBody().contains(carePlanDto2));
    }

    @Test
    public void searchCarePlans_carePlansMissing_200() throws Exception {

        String cpr = "0101010101";
        Boolean onlyUnsatisfiedSchedules = null;
        Boolean onlyActiveCarePlans = true;

        Pagination pagination = new Pagination(1, 10);


        Mockito.when(carePlanService.getCarePlansWithFilters(cpr, onlyActiveCarePlans, false, pagination)).thenReturn(List.of());


        ResponseEntity<List<CarePlanDto>> result = subject.searchCarePlans(Optional.of(cpr), Optional.ofNullable(onlyUnsatisfiedSchedules), Optional.of(onlyActiveCarePlans), Optional.of(pagination.getOffset()), Optional.of(pagination.getLimit()));


        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    public void searchCarePlans_failureToFetch_500() throws Exception {

        String cpr = "0101010101";
        Boolean onlyUnsatisfiedSchedules = null;
        Boolean onlyActiveCarePlans = true;

        Pagination pagination = new Pagination(1, 10);


        Mockito.when(carePlanService.getCarePlansWithFilters(cpr, onlyActiveCarePlans, false, pagination)).thenThrow(new ServiceException("error", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.INTERNAL_SERVER_ERROR));


        assertThrows(InternalServerErrorException.class, () -> subject.searchCarePlans(Optional.of(cpr), Optional.ofNullable(onlyUnsatisfiedSchedules), Optional.of(onlyActiveCarePlans), Optional.of(pagination.getOffset()), Optional.of(pagination.getLimit())));
    }
}