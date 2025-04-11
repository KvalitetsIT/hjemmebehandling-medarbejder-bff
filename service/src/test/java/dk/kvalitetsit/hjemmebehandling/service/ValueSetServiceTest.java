package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.*;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ValueSetServiceTest {
    @InjectMocks
    private ValueSetService subject;

    @Mock
    private FhirClient fhirClient;

    @Mock
    private FhirMapper fhirMapper;

    @Test
    public void getPlanDefinitions_sucecss() throws Exception {
        ValueSet valueSet = new ValueSet();
        MeasurementTypeModel measurementTypeModel = MeasurementTypeModel.builder().build();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(valueSet);
        Mockito.when(fhirClient.lookupValueSet()).thenReturn(lookupResult);
        Mockito.when(fhirMapper.extractMeasurementTypes(valueSet)).thenReturn(List.of(measurementTypeModel));

        List<MeasurementTypeModel> result = subject.getMeasurementTypes();

        assertEquals(1, result.size());
        assertEquals(measurementTypeModel, result.getFirst());
    }
}