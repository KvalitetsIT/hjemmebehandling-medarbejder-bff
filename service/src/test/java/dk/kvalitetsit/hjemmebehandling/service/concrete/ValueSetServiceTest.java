package dk.kvalitetsit.hjemmebehandling.service.concrete;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.repository.ValueSetRepository;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteValueSetService;
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
    private ConcreteValueSetService subject;

    @Mock
    private ValueSetRepository<ValueSet> repository;

    @Test
    public void getPlanDefinitions_sucecss() throws Exception {
        ValueSet valueSet = new ValueSet();
        MeasurementTypeModel measurementTypeModel = MeasurementTypeModel.builder().build();
        List<ValueSet> lookupResult = List.of(valueSet);
        Mockito.when(repository.fetch()).thenReturn(lookupResult);

        List<MeasurementTypeModel> result = subject.getMeasurementTypes();

        assertEquals(1, result.size());
        assertEquals(measurementTypeModel, result.getFirst());
    }
}