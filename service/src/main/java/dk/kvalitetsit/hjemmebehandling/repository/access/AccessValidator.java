package dk.kvalitetsit.hjemmebehandling.repository.access;

import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.model.BaseModel;
import dk.kvalitetsit.hjemmebehandling.model.OrganizationModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.repository.OrganizationRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Organization;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingConsumer;
import org.springframework.util.function.ThrowingFunction;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class AccessValidator<T extends BaseModel> {
    private final UserContextProvider userContextProvider;
    private final OrganizationRepository<Organization> organizationRepository;

    public AccessValidator(UserContextProvider userContextProvider, OrganizationRepository<Organization> organizationRepository) {
        this.userContextProvider = userContextProvider;
        this.organizationRepository = organizationRepository;
    }

    public T validateAccess(T resource) throws AccessValidationException, ServiceException {
        return validateAccess(List.of(resource)).getFirst();
    }

    public List<T> validateAccess(List<T> resources) throws AccessValidationException, ServiceException {
        // Validate that the user is allowed to access all the resources.
        QualifiedId.OrganizationId userOrganizationId = getOrganizationIdForUser();

        ThrowingFunction<String, String> e = (s) -> {
            throw new ServiceException("", ErrorKind.INTERNAL_SERVER_ERROR, ErrorDetails.MISSING_SOR_CODE);
        };


        for (var resource : resources) {
            QualifiedId.OrganizationId resourceOrganizationId = resource.organizationId();

            if (!userOrganizationId.equals(resourceOrganizationId)) {
                throw new AccessValidationException(String.format(
                        "Error updating status on resource of type %s. Id was %s. User belongs to organization %s, but resource belongs to organization %s.",
                        resource.getClass(),
                        resource.id(),
                        userOrganizationId,
                        resourceOrganizationId));
            }
        }
        return resources;
    }

    private QualifiedId.OrganizationId getOrganizationIdForUser() throws AccessValidationException, ServiceException {
        var context = Optional.ofNullable(userContextProvider.getUserContext()).orElseThrow(() -> new IllegalStateException("UserContext was not initialized!"));

        var SOR = context.organization().map(OrganizationModel::id).orElseThrow(() -> new AccessValidationException("No SOR code was present"));

        Organization organization = organizationRepository.lookupOrganizationBySorCode(SOR).orElseThrow(() -> new AccessValidationException(String.format("No organization was present for sorCode %s!", context.organization().map(OrganizationModel::id))));

        return new QualifiedId.OrganizationId(organization.getIdElement().toUnqualifiedVersionless().getValue());
    }
}
