package dk.kvalitetsit.hjemmebehandling.fhir.repository;

import ca.uhn.fhir.rest.gclient.ICriterion;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Organization;

import java.util.List;
import java.util.Optional;

public class ConcreteOrganizationRepository implements OrganizationRepository<Organization> {

    private final FhirClient client;
    private final UserContextProvider userContextProvider;

    public ConcreteOrganizationRepository(FhirClient client, UserContextProvider userContextProvider) {
        this.client = client;
        this.userContextProvider = userContextProvider;
    }


    public Optional<Organization> lookupOrganizationBySorCode(String sorCode) throws ServiceException {
        if (sorCode == null || sorCode.isBlank() || sorCode.isEmpty())
            throw new ServiceException("The SOR-code was not specified", ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE);
        var sorCodeCriterion = Organization.IDENTIFIER.exactly().systemAndValues(Systems.SOR, sorCode);

        var lookupResult = lookupOrganizationsByCriteria(List.of(sorCodeCriterion));
        if (lookupResult.getOrganizations().isEmpty()) {
            return Optional.empty();
        }
        if (lookupResult.getOrganizations().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of %s!", Organization.class));
        }
        return Optional.of(lookupResult.getOrganizations().getFirst());
    }

    public String getOrganizationId() throws ServiceException {
        Organization organization = getCurrentUsersOrganization();
        return organization.getIdElement().toUnqualifiedVersionless().getValue();
    }

    public Organization getCurrentUsersOrganization() throws ServiceException {
        var context = userContextProvider.getUserContext();
        if (context == null) {
            throw new IllegalStateException("UserContext was not initialized!");
        }

        var orgId = context.getOrgId().orElseThrow(() -> new ServiceException(String.format("No Organization was present for sorCode %s!", context.getOrgId()), ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE));
        return lookupOrganizationBySorCode(orgId).orElseThrow();
    }


    private FhirLookupResult lookupOrganizationsByCriteria(List<ICriterion<?>> criteria) {
        // Don't try to include Organization-resources when we are looking up organizations ...
        boolean withOrganizations = false;
        return client.lookupByCriteria(Organization.class, criteria, List.of(), withOrganizations, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private FhirLookupResult lookupOrganizations(List<String> organizationIds) {
        var idCriterion = Organization.RES_ID.exactly().codes(organizationIds);
        return lookupOrganizationsByCriteria(List.of(idCriterion));
    }
}
