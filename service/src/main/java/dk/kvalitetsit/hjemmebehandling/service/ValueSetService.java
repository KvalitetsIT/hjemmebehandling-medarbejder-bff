package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ValueSetService {
    private static final Logger logger = LoggerFactory.getLogger(ValueSetService.class);

    private FhirClient fhirClient;

    private FhirMapper fhirMapper;

    public ValueSetService(FhirClient fhirClient, FhirMapper fhirMapper) {
        this.fhirClient = fhirClient;
        this.fhirMapper = fhirMapper;
    }

    public List<MeasurementTypeModel> getMeasurementTypes() throws ServiceException {
        // as of now we only have one ValueSet in the system which holds the measurement type codes, so no special search handling is needed.
        FhirLookupResult lookupResult = fhirClient.lookupValueSet();

        List<MeasurementTypeModel> result = new ArrayList<>();
        lookupResult.getValueSets().stream()
            .forEach(vs -> {
                var list = fhirMapper.extractMeasurementTypes(vs);
                result.addAll(list);
            });

        return result;
    }
}
