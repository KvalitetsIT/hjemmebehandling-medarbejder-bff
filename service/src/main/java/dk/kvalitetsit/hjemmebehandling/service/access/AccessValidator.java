package dk.kvalitetsit.hjemmebehandling.service.access;

import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccessValidator {
    private UserContextProvider userContextProvider;
    private FhirClient fhirClient;

    public AccessValidator(UserContextProvider userContextProvider, FhirClient fhirClient) {
        this.userContextProvider = userContextProvider;
        this.fhirClient = fhirClient;
    }

    public void validateAccess(DomainResource resource) throws AccessValidationException, ServiceException {
        validateAccess(List.of(resource));
    }

    public void validateAccess(List<? extends DomainResource> resources) throws AccessValidationException, ServiceException {
        // Validate that the user is allowed to access all the resources.
        String userOrganizationId = getOrganizationIdForUser();

        for (var resource : resources) {
            String resourceOrganizationId = getOrganizationIdForResource(resource);

            if (!userOrganizationId.equals(resourceOrganizationId)) {
                throw new AccessValidationException(String.format(
                        "Error updating status on resource of type %s. Id was %s. User belongs to organization %s, but resource belongs to organization %s.",
                        resource.getResourceType(),
                        resource.getId(),
                        userOrganizationId,
                        resourceOrganizationId));
            }
        }
    }

    private String getOrganizationIdForUser() throws AccessValidationException, ServiceException {
        var context = userContextProvider.getUserContext();
        if (context == null) {
            throw new IllegalStateException("UserContext was not initialized!");
        }



        Organization organization = fhirClient.lookupOrganizationBySorCode(context.getOrgId())
                .orElseThrow(() -> new AccessValidationException(
                        String.format("No organization was present for sorCode %s!", context.getOrgId())));

        return organization.getIdElement().toUnqualifiedVersionless().getValue();
    }

    private String getOrganizationIdForResource(DomainResource resource) {
        var extension = resource.getExtension()
                .stream()
                .filter(e -> e.getUrl().equals(Systems.ORGANIZATION) && e.getValue() instanceof Reference)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        String.format("No organization id was present on resource %s!", resource.getId())));
        return ((Reference) extension.getValue()).getReference();
    }
}
