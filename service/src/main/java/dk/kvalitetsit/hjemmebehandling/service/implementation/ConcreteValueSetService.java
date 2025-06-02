package dk.kvalitetsit.hjemmebehandling.service.implementation;

import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;
import dk.kvalitetsit.hjemmebehandling.model.ValueSetModel;
import dk.kvalitetsit.hjemmebehandling.repository.ValueSetRepository;
import dk.kvalitetsit.hjemmebehandling.service.ValueSetService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class ConcreteValueSetService implements ValueSetService {

    private final ValueSetRepository<ValueSetModel> repository;

    public ConcreteValueSetService(ValueSetRepository<ValueSetModel> repository) {
        this.repository = repository;
    }

    @Override
    public List<MeasurementTypeModel> getMeasurementTypes() throws ServiceException, AccessValidationException {
        // as of now we only have one ValueSet in the system which holds the measurement type codes, so no special search handling is needed.
        List<ValueSetModel> response = repository.fetch();

        return response.stream().map(ValueSetModel::measurementTypes).flatMap(Collection::stream).toList();
    }



}
