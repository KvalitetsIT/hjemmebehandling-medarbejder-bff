package dk.kvalitetsit.hjemmebehandling.service.implementation;

import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;
import dk.kvalitetsit.hjemmebehandling.repository.ValueSetRepository;
import dk.kvalitetsit.hjemmebehandling.repository.implementation.ConcreteValueSetRepository;
import dk.kvalitetsit.hjemmebehandling.service.ValueSetService;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConcreteValueSetService implements ValueSetService {

    private final ValueSetRepository<ValueSet> repository;

    public ConcreteValueSetService(ValueSetRepository<ValueSet> repository) {
        this.repository = repository;
    }

    @Override
    public List<MeasurementTypeModel> getMeasurementTypes() throws ServiceException {
        // as of now we only have one ValueSet in the system which holds the measurement type codes, so no special search handling is needed.
        List<ValueSet> lookupResult = repository.fetch();

        List<MeasurementTypeModel> result = new ArrayList<>();
        lookupResult.forEach(vs -> {
            var list = extractMeasurementTypes(vs);
            result.addAll(list);
        });

        return result;
    }

    private List<MeasurementTypeModel> extractMeasurementTypes(ValueSet valueSet) {
        return valueSet.getCompose().getInclude()
                .stream()
                .flatMap(csc -> csc.getConcept()
                        .stream()
                        .map(crc -> mapCodingConcept(csc.getSystem(), crc))).toList();
    }

    private MeasurementTypeModel mapCodingConcept(String system, ValueSet.ConceptReferenceComponent concept) {
        return new MeasurementTypeModel(system, concept.getCode(), concept.getDisplay());
    }

}
