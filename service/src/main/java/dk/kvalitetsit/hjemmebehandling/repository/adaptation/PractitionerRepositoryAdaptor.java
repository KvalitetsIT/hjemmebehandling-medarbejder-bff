package dk.kvalitetsit.hjemmebehandling.repository.adaptation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.PractitionerModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.PractitionerRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.List;
import java.util.Optional;

/**
 * Adapter responsible for translating between FHIR resources and domain-specific logic.
 * <p>
 * This class primarily handles the mapping of business models to domain representations
 * and delegates calls deeper into the application stack with the appropriate arguments.
 * <p>
 * Currently, it implements the {@link PractitionerRepository} interface for {@link PractitionerModel} entities.
 * Note that this implementation detail may change in the future.
 */
public class PractitionerRepositoryAdaptor implements PractitionerRepository<PractitionerModel> {

    private final PractitionerRepository<Practitioner> repository;
    private final FhirMapper mapper;

    public PractitionerRepositoryAdaptor(PractitionerRepository<Practitioner> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public QualifiedId.PractitionerId save(PractitionerModel practitionerModel) throws ServiceException {
        return repository.save(mapper.mapPractitionerModel(practitionerModel));
    }

    @Override
    public PractitionerModel getOrCreateUserAsPractitioner() throws ServiceException, AccessValidationException {
        return mapper.mapPractitioner(repository.getOrCreateUserAsPractitioner());
    }

    @Override
    public void update(PractitionerModel resource) throws ServiceException, AccessValidationException {
        repository.update(mapper.mapPractitionerModel(resource));
    }

    @Override
    public Optional<PractitionerModel> fetch(QualifiedId.PractitionerId id) throws ServiceException, AccessValidationException {
        return repository.fetch(id).map(mapper::mapPractitioner);
    }

    @Override
    public List<PractitionerModel> fetch(List<QualifiedId.PractitionerId> id) throws ServiceException, AccessValidationException {
        return repository.fetch(id).stream().map(mapper::mapPractitioner).toList();
    }


    @Override
    public List<PractitionerModel> fetch() throws ServiceException, AccessValidationException {
        return repository.fetch().stream().map(mapper::mapPractitioner).toList();
    }

    @Override
    public List<PractitionerModel> history(QualifiedId.PractitionerId id) throws ServiceException, AccessValidationException {
        return repository.history(id).stream().map(mapper::mapPractitioner).toList();
    }

    @Override
    public List<PractitionerModel> history(List<QualifiedId.PractitionerId> ids) throws ServiceException, AccessValidationException {
        return repository.history(ids).stream().map(mapper::mapPractitioner).toList();
    }
}
