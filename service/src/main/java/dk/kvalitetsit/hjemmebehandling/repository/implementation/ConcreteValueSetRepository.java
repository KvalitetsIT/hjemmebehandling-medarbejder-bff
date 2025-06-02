package dk.kvalitetsit.hjemmebehandling.repository.implementation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.ValueSetRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.ValueSet;

import java.util.List;
import java.util.Optional;

/**
 * A concrete implementation of the {@link ValueSetRepository} interface for managing
 * {@link ValueSet} entities.
 * <p>
 * This class provides the underlying logic to retrieve, store, and manipulate organization-related data
 * within the domain, serving as the bridge between the domain model and data source.
 */
public class ConcreteValueSetRepository implements ValueSetRepository<ValueSet> {

    private final FhirClient fhirClient;

    public ConcreteValueSetRepository(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    @Override
    public void update(ValueSet resource) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public QualifiedId.ValueSetId save(ValueSet resource) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public Optional<ValueSet> fetch(QualifiedId.ValueSetId id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<ValueSet> fetch(List<QualifiedId.ValueSetId> id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<ValueSet> fetch() throws ServiceException {
        return fhirClient.fetch(ValueSet.class);
    }

    @Override
    public List<ValueSet> history(QualifiedId.ValueSetId id) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public List<ValueSet> history(List<QualifiedId.ValueSetId> valueSetIds) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }
}
