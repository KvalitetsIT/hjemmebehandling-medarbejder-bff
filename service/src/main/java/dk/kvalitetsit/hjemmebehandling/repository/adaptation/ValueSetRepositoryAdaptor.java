package dk.kvalitetsit.hjemmebehandling.repository.adaptation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.ValueSetModel;
import dk.kvalitetsit.hjemmebehandling.repository.ValueSetRepository;
import dk.kvalitetsit.hjemmebehandling.repository.implementation.ConcreteValueSetRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.ValueSet;

import java.util.List;
import java.util.Optional;

public class ValueSetRepositoryAdaptor implements ValueSetRepository<ValueSetModel> {

    private final ConcreteValueSetRepository repository;
    private final FhirMapper mapper;

    public ValueSetRepositoryAdaptor(ConcreteValueSetRepository repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<ValueSetModel> history(List<QualifiedId.ValueSetId> valueSetIds) throws ServiceException, AccessValidationException {
        return repository.history(valueSetIds).stream().map(mapper::mapValueSet).toList();
    }

    @Override
    public List<ValueSetModel> history(QualifiedId.ValueSetId id) throws ServiceException, AccessValidationException {
        return repository.history(id).stream().map(mapper::mapValueSet).toList();
    }

    @Override
    public List<ValueSetModel> fetch() throws ServiceException {
        return repository.fetch().stream().map(mapper::mapValueSet).toList();
    }

    @Override
    public List<ValueSetModel> fetch(List<QualifiedId.ValueSetId> id) throws ServiceException {
        return repository.fetch(id).stream().map(mapper::mapValueSet).toList();
    }

    @Override
    public void update(ValueSetModel resource) throws ServiceException {
        repository.update(mapper.mapValueSet(resource));
    }

    @Override
    public QualifiedId.ValueSetId save(ValueSetModel resource) throws ServiceException {
        return repository.save(mapper.mapValueSet(resource));
    }

    @Override
    public Optional<ValueSetModel> fetch(QualifiedId.ValueSetId id) throws ServiceException {
        return repository.fetch(id).map(mapper::mapValueSet);
    }

    public QualifiedId.ValueSetId save(ValueSet resource) throws ServiceException {
        return repository.save(resource);
    }

    public void update(ValueSet resource) throws ServiceException {
        repository.update(resource);
    }
}
