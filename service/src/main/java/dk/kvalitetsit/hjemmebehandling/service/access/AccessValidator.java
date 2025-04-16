package dk.kvalitetsit.hjemmebehandling.service.access;

import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.client.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.BaseModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Organization;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccessValidator {
    private final UserContextProvider userContextProvider;
    private final FhirClient fhirClient;

    public AccessValidator(UserContextProvider userContextProvider, FhirClient fhirClient) {
        this.userContextProvider = userContextProvider;
        this.fhirClient = fhirClient;
    }

    public void validateAccess(BaseModel resource) throws AccessValidationException, ServiceException {
        validateAccess(List.of(resource));
    }

    public void validateAccess(List<? extends BaseModel> resources) throws AccessValidationException, ServiceException {
        // Validate that the user is allowed to access all the resources.
        String userOrganizationId = getOrganizationIdForUser();

        for (var resource : resources) {
            String resourceOrganizationId = getOrganizationIdForResource(resource);

            if (!userOrganizationId.equals(resourceOrganizationId)) {
                throw new AccessValidationException(String.format(
                        "Error updating status on resource of type %s. Id was %s. User belongs to organization %s, but resource belongs to organization %s.",
                        resource.getClass(),
                        resource.id(),
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
        // TODO: Handle 'Optional.get()' without 'isPresent()' check below
        Organization organization = fhirClient.lookupOrganizationBySorCode(context.getOrgId().get())
                .orElseThrow(() -> new AccessValidationException(
                        String.format("No organization was present for sorCode %s!", context.getOrgId())));

        return organization.getIdElement().toUnqualifiedVersionless().getValue();
    }

    private String getOrganizationIdForResource(BaseModel resource) {
        return  resource.organizationId();
    }
}
