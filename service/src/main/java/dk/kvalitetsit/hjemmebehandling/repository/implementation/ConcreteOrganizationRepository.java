package dk.kvalitetsit.hjemmebehandling.repository.implementation;

import ca.uhn.fhir.rest.gclient.ICriterion;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.repository.OrganizationRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.Organization;

import java.util.List;
import java.util.Optional;

/**
 * A concrete implementation of the {@link OrganizationRepository} interface for managing
 * {@link Organization} entities.
 * <p>
 * This class provides the underlying logic to retrieve, store, and manipulate organization-related data
 * within the domain, serving as the bridge between the domain model and data source.
 */
public class ConcreteOrganizationRepository implements OrganizationRepository<Organization> {

    private final FhirClient client;
    private final UserContextProvider userContextProvider;

    public ConcreteOrganizationRepository(FhirClient client, UserContextProvider userContextProvider) {
        this.client = client;
        this.userContextProvider = userContextProvider;
    }

    public Optional<Organization> lookupOrganizationBySorCode(QualifiedId.OrganizationId sorCode) throws ServiceException {
        if (sorCode == null || sorCode.unqualified().isBlank() || sorCode.unqualified().isEmpty())
            throw new ServiceException("The SOR-code was not specified", ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE);
        var sorCodeCriterion = Organization.IDENTIFIER.exactly().systemAndValues(Systems.SOR, sorCode.unqualified());

        var lookupResult = lookupOrganizationsByCriteria(List.of(sorCodeCriterion));
        if (lookupResult.getOrganizations().isEmpty()) {
            return Optional.empty();
        }
        if (lookupResult.getOrganizations().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of %s!", Organization.class));
        }
        return Optional.of(lookupResult.getOrganizations().getFirst());
    }


    public QualifiedId.OrganizationId getOrganizationId() throws ServiceException {
        return userContextProvider.getUserContext().orgId()
                .orElseThrow(() -> new ServiceException("No SOR code was present", ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE));

// The code below will eventually request the fhir server however the organisation id is already specified on the usercontext
//        Organization organization = fetchCurrentUsersOrganization();
//        return new QualifiedId.OrganizationId(organization.getIdElement().toUnqualifiedVersionless().getValue());
    }

    public Organization fetchCurrentUsersOrganization() throws ServiceException {
        var context = userContextProvider.getUserContext();
        if (context == null) {
            throw new IllegalStateException("UserContext was not initialized!");
        }

        var orgId = context.orgId().orElseThrow(() -> new ServiceException(String.format("No Organization was present for sorCode %s!", context.orgId()), ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE));
        return lookupOrganizationBySorCode(orgId).orElseThrow();
    }

    @Override
    public void update(Organization resource) {
        client.updateResource(resource);
    }

    @Override
    public QualifiedId.OrganizationId save(Organization resource) throws ServiceException {
        return new QualifiedId.OrganizationId(client.saveResource(resource));
    }

    @Override
    public Optional<Organization> fetch(QualifiedId.OrganizationId id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<Organization> fetch(List<QualifiedId.OrganizationId> ids) throws ServiceException {
        return this.lookupOrganizations(ids.stream().map(QualifiedId::unqualified).toList());
    }

    @Override
    public List<Organization> fetch() throws ServiceException {
        throw new NotImplementedException();
    }

    private List<Organization> lookupOrganizations(List<String> organizationIds) throws ServiceException {
        var idCriterion = Organization.RES_ID.exactly().codes(organizationIds);
        return lookupOrganizationsByCriteria(List.of(idCriterion)).getOrganizations();
    }

    private FhirLookupResult lookupOrganizationsByCriteria(List<ICriterion<?>> criteria) throws ServiceException {
        // Don't try to include Organization-resources when we are looking up organizations ...
        boolean withOrganizations = false;
        return client.lookupByCriteria(Organization.class, criteria, List.of(), withOrganizations, Optional.empty(), Optional.empty(), Optional.empty());
    }

}
