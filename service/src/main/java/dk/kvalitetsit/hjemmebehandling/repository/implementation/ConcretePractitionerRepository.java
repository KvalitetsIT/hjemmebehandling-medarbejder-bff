package dk.kvalitetsit.hjemmebehandling.repository.implementation;

import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.OrganizationModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.repository.PractitionerRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.SerializationException;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.List;
import java.util.Optional;

/**
 * A concrete implementation of the {@link PractitionerRepository} interface for managing
 * {@link Practitioner} entities.
 * <p>
 * This class provides the underlying logic to retrieve, store, and manipulate organization-related data
 * within the domain, serving as the bridge between the domain model and data source.
 */
public class ConcretePractitionerRepository implements PractitionerRepository<Practitioner> {

    private final FhirClient client;
    private final UserContextProvider userContextProvider;

    public ConcretePractitionerRepository(FhirClient client, UserContextProvider userContextProvider) {
        this.client = client;
        this.userContextProvider = userContextProvider;
    }

    @Override
    public Practitioner getOrCreateUserAsPractitioner() throws ServiceException {
        // TODO: Handle 'Optional.get()' without 'isPresent()' check below
        QualifiedId.OrganizationId orgId = userContextProvider.getUserContext().organization().map(OrganizationModel::id).orElseThrow(() -> new ServiceException("Could not acquire SOR-code from user context", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.MISSING_SOR_CODE));
        String userId = userContextProvider.getUserContext().userId().get();
        ICriterion<TokenClientParam> sorIdentifier = Practitioner.IDENTIFIER.exactly().systemAndIdentifier(Systems.SOR, orgId.unqualified());
        ICriterion<TokenClientParam> userIdIdentifier = Practitioner.IDENTIFIER.exactly().systemAndIdentifier(Systems.USER_ID, userId);

        Optional<Practitioner> practitioner = lookupPractitioner(List.of(sorIdentifier, userIdIdentifier));

        if (practitioner.isPresent()) {
            return practitioner.get();
        } else {
            Practitioner p = new Practitioner();
            p.addIdentifier().setSystem(Systems.SOR).setValue(orgId.unqualified());
            p.addIdentifier().setSystem(Systems.USER_ID).setValue(userId);

            userContextProvider.getUserContext().name().ifPresent(name -> {
                name.getGiven().ifPresent(x -> p.getNameFirstRep().addGiven(x));
                name.getFamily().ifPresent(x -> p.getNameFirstRep().setFamily(x));
            });

            // TODO: save should perhaps not just return the id but the resource which was saved
            var practitionerId = save(p);
            return fetch(practitionerId).get();
        }
    }


    @Override
    public void update(Practitioner practitioner) {
        throw new NotImplementedException();
    }

    @Override
    public QualifiedId.PractitionerId save(Practitioner practitioner) throws ServiceException {
        return new QualifiedId.PractitionerId(client.saveResource(practitioner));

    }

    @Override
    public Optional<Practitioner> fetch(QualifiedId.PractitionerId id) throws ServiceException {
        var idCriterion = Practitioner.RES_ID.exactly().code(id.unqualified());
        return lookupPractitioner(idCriterion);
    }

    @Override
    public List<Practitioner> fetch(List<QualifiedId.PractitionerId> ids) throws ServiceException {
        var idCriterion = Practitioner.RES_ID.exactly().codes(ids.stream().map(QualifiedId::unqualified).toList());
        return client.lookupByCriteria(Practitioner.class, List.of(idCriterion)).getPractitioners();
    }

    @Override
    public List<Practitioner> fetch() {
        throw new NotImplementedException();
    }

    @Override
    public List<Practitioner> history(QualifiedId.PractitionerId id) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public List<Practitioner> history(List<QualifiedId.PractitionerId> practitionerIds) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    private Optional<Practitioner> lookupPractitioner(ICriterion<?> criterion) throws ServiceException {
        return lookupPractitioner(List.of(criterion));
    }

    private Optional<Practitioner> lookupPractitioner(List<ICriterion<?>> criterions) throws ServiceException {
        var lookupResult = client.lookupByCriteria(Practitioner.class, criterions);

        if (lookupResult.getPractitioners().isEmpty()) {
            return Optional.empty();
        }
        if (lookupResult.getPractitioners().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", Practitioner.class));
        }
        return Optional.of(lookupResult.getPractitioners().getFirst());
    }
}
