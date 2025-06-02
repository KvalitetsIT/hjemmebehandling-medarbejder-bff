package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteValueSetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.MeasurementTypeDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValueSetControllerTest {
    @InjectMocks
    private ValueSetController subject;

    @Mock
    private ConcreteValueSetService valueSetService;

    @Mock
    private DtoMapper dtoMapper;

    @Test
    public void getMeasurementTypes_measurementTypesPresent_200() throws Exception {
        MeasurementTypeDto measurementTypeDto1 = new MeasurementTypeDto()
                .code("code")
                .display("display")
                .system("system");

        MeasurementTypeDto measurementTypeDto2 = new MeasurementTypeDto()
                .code("code")
                .display("display")
                .system("system");

        MeasurementTypeModel measurementTypeModel1 = new MeasurementTypeModel("system", "code", "display");
        MeasurementTypeModel measurementTypeModel2 = new MeasurementTypeModel("system", "code", "display");

        Mockito.when(valueSetService.getMeasurementTypes()).thenReturn(List.of(measurementTypeModel1, measurementTypeModel2));
        Mockito.when(dtoMapper.mapMeasurementTypeModel(measurementTypeModel1)).thenReturn(measurementTypeDto1);
        Mockito.when(dtoMapper.mapMeasurementTypeModel(measurementTypeModel2)).thenReturn(measurementTypeDto2);

        ResponseEntity<List<MeasurementTypeDto>> result = subject.getMeasurementTypes();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().contains(measurementTypeDto1));
        assertTrue(result.getBody().contains(measurementTypeDto2));
    }

    @Test
    public void getMeasurementTypes_measurementTypesMissing_200() throws Exception {
        Mockito.when(valueSetService.getMeasurementTypes()).thenReturn(List.of());

        ResponseEntity<List<MeasurementTypeDto>> result = subject.getMeasurementTypes();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(Objects.requireNonNull(result.getBody()).isEmpty());
    }
}