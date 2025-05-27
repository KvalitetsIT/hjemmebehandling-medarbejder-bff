package dk.kvalitetsit.hjemmebehandling.service.concrete;

import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;
import dk.kvalitetsit.hjemmebehandling.model.ValueSetModel;
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
    private ValueSetRepository<ValueSetModel> repository;

    @Test
    public void getPlanDefinitions_success() throws Exception {
        MeasurementTypeModel measurementTypeModel = MeasurementTypeModel.builder().build();
        ValueSetModel valueSet = new ValueSetModel(List.of(measurementTypeModel));

        Mockito.when(repository.fetch()).thenReturn(List.of(valueSet));
        List<MeasurementTypeModel> result = subject.getMeasurementTypes();
        assertEquals(1, result.size());
        assertEquals(measurementTypeModel, result.getFirst());
    }
}