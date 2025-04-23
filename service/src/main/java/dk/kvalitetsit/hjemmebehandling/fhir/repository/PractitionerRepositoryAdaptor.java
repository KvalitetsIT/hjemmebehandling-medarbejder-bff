package dk.kvalitetsit.hjemmebehandling.fhir.repository;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.PractitionerModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PractitionerRepositoryAdaptor implements PractitionerRepository<PractitionerModel> {

    private final PractitionerRepository<Practitioner> repository;
    private final FhirMapper mapper;

    public PractitionerRepositoryAdaptor(PractitionerRepository<Practitioner> repository, FhirMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public String savePractitioner(PractitionerModel practitionerModel) throws ServiceException {
        return repository.savePractitioner(mapper.mapPractitionerModel(practitionerModel));
    }

    @Override
    public PractitionerModel getOrCreateUserAsPractitioner() throws ServiceException {
        return mapper.mapPractitioner(repository.getOrCreateUserAsPractitioner());
    }

    @Override
    public List<PractitionerModel> lookupPractitioners(Collection<QualifiedId> practitionerIds) {
        return repository.lookupPractitioners(practitionerIds).stream().map(mapper::mapPractitioner).toList();
    }

    @Override
    public Optional<PractitionerModel> lookupPractitionerById(QualifiedId practitionerId) {
        return repository.lookupPractitionerById(practitionerId).map(mapper::mapPractitioner);
    }

    @Override
    public void update(PractitionerModel resource) {
        repository.update(mapper.mapPractitionerModel(resource));
    }

    @Override
    public String save(PractitionerModel resource) throws ServiceException {
        return repository.save(mapper.mapPractitionerModel(resource));
    }

    @Override
    public Optional<PractitionerModel> fetch(QualifiedId id) throws ServiceException {
        return repository.fetch(id).map(mapper::mapPractitioner);
    }


    @Override
    public List<PractitionerModel> fetch() {
        return repository.fetch().stream().map(mapper::mapPractitioner).toList();
    }
}
