package dk.kvalitetsit.hjemmebehandling.repository.adaptation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.repository.PractitionerRepository;
import dk.kvalitetsit.hjemmebehandling.model.PractitionerModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.List;
import java.util.Optional;

/**
 * An adapter whose responsibility is to adapt between FHIR and the domain logic.
 * This primarily covers mapping from business models and calling further into the stack with the expected arguments
 * For now, it implements the PractitionerRepository interface, but this might change in the future
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
    public PractitionerModel getOrCreateUserAsPractitioner() throws ServiceException {
        return mapper.mapPractitioner(repository.getOrCreateUserAsPractitioner());
    }

    @Override
    public void update(PractitionerModel resource) throws ServiceException {
        repository.update(mapper.mapPractitionerModel(resource));
    }

    @Override
    public Optional<PractitionerModel> fetch(QualifiedId.PractitionerId id) throws ServiceException {
        return repository.fetch(id).map(mapper::mapPractitioner);
    }

    @Override
    public List<PractitionerModel> fetch(List<QualifiedId.PractitionerId> id) throws ServiceException {
        return repository.fetch(id).stream().map(mapper::mapPractitioner).toList();
    }


    @Override
    public List<PractitionerModel> fetch() throws ServiceException {
        return repository.fetch().stream().map(mapper::mapPractitioner).toList();
    }
}
