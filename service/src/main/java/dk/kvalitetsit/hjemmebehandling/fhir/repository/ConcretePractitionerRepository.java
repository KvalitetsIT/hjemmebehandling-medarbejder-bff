package dk.kvalitetsit.hjemmebehandling.fhir.repository;

import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.List;
import java.util.Optional;

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
        String orgId = userContextProvider.getUserContext().getOrgId().get();
        String userId = userContextProvider.getUserContext().getUserId().get();

        ICriterion<TokenClientParam> sorIdentifier = Practitioner.IDENTIFIER.exactly().systemAndIdentifier(Systems.SOR, orgId);
        ICriterion<TokenClientParam> userIdIdentifier = Practitioner.IDENTIFIER.exactly().systemAndIdentifier(Systems.USER_ID, userId);
        //FhirLookupResult lookupResult = lookupByCriteria(Practitioner.class, List.of(sorIdentifier, userIdIdentifier));
        Optional<Practitioner> practitioner = lookupPractitioner(List.of(sorIdentifier, userIdIdentifier));

        if (practitioner.isPresent()) {
            return practitioner.get();
        } else {
            Practitioner p = new Practitioner();
            p.addIdentifier().setSystem(Systems.SOR).setValue(orgId);
            p.addIdentifier().setSystem(Systems.USER_ID).setValue(userId);

            // TODO: Handle 'Optional.get()' without 'isPresent()' check below
            p.getNameFirstRep().addGiven(userContextProvider.getUserContext().getFirstName().get());
            p.getNameFirstRep().setFamily(userContextProvider.getUserContext().getLastName().get());

            String practitionerId = save(p);
            return fetch(practitionerId).get();
        }
    }


    @Override
    public void update(Practitioner practitioner) {
        throw new NotImplementedException();
    }

    @Override
    public String save(Practitioner practitioner) throws ServiceException {
        return client.saveResource(practitioner);

    }

    @Override
    public Optional<Practitioner> fetch(QualifiedId id) throws ServiceException {
        var idCriterion = Practitioner.RES_ID.exactly().code(id.id());
        return lookupPractitioner(idCriterion);
    }

    @Override
    public List<Practitioner> fetch(List<QualifiedId> ids) {
        var idCriterion = Practitioner.RES_ID.exactly().codes(ids.stream().map(QualifiedId::id).toList());
        return client.lookupByCriteria(Practitioner.class, List.of(idCriterion)).getPractitioners();
    }

    @Override
    public List<Practitioner> fetch() {
        throw new NotImplementedException();
    }



    private Optional<Practitioner> lookupPractitioner(ICriterion<?> criterion) {
        return lookupPractitioner(List.of(criterion));
    }

    private Optional<Practitioner> lookupPractitioner(List<ICriterion<?>> criterions) {
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
