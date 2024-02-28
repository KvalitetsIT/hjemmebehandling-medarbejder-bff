package dk.kvalitetsit.hjemmebehandling.service.access;

import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.context.UserContext;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AccessValidatorTest {
    @InjectMocks
    private AccessValidator subject;

    @Mock
    private UserContextProvider userContextProvider;

    @Mock
    private FhirClient fhirClient;

    private static final String ORGANIZATION_ID_1 = "organization-1";
    private static final String ORGANIZATION_ID_2 = "organization-2";

    private static final String SOR_CODE_1 = "123456";

    @Test
    public void validateAccess_contextNotInitialized() {
        // Arrange
        var resource = buildResource();

        // Act

        // Assert
        assertThrows(IllegalStateException.class, () -> subject.validateAccess(resource));
    }

    @Test
    public void validateAccess_unknownOrganization() {
        // Arrange
        var resource = buildResource();

        var context = new UserContext(SOR_CODE_1);
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);

        Mockito.when(fhirClient.lookupOrganizationBySorCode(context.getOrgId())).thenReturn(Optional.empty());

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.validateAccess(resource));
    }

    @Test
    public void validateAccess_noOrganizationTag() {
        // Arrange
        var resource = buildResource();

        var context = new UserContext(SOR_CODE_1);
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);

        var organization = buildOrganization(ORGANIZATION_ID_1);
        Mockito.when(fhirClient.lookupOrganizationBySorCode(context.getOrgId())).thenReturn(Optional.of(organization));

        // Act

        // Assert
        assertThrows(IllegalStateException.class, () -> subject.validateAccess(resource));
    }

    @Test
    public void validateAccess_wrongOrganization_accessViolation() {
        // Arrange
        var resource = buildResource(ORGANIZATION_ID_2);

        var context = new UserContext(SOR_CODE_1);
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);

        var organization = buildOrganization(ORGANIZATION_ID_1);
        Mockito.when(fhirClient.lookupOrganizationBySorCode(context.getOrgId())).thenReturn(Optional.of(organization));

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.validateAccess(resource));
    }

    @Test
    public void validateAccess_correctOrganization_success() {
        // Arrange
        var resource = buildResource(ORGANIZATION_ID_1);

        var context = new UserContext(SOR_CODE_1);
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);

        var organization = buildOrganization(ORGANIZATION_ID_1);
        Mockito.when(fhirClient.lookupOrganizationBySorCode(context.getOrgId())).thenReturn(Optional.of(organization));

        // Act

        // Assert
        assertDoesNotThrow(() -> subject.validateAccess(resource));
    }

    @Test
    public void validateAccess_conjunction_failure() {
        // Arrange
        var resource1 = buildResource(ORGANIZATION_ID_1);
        var resource2 = buildResource(ORGANIZATION_ID_2);

        var context = new UserContext(SOR_CODE_1);
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);

        var organization = buildOrganization(ORGANIZATION_ID_1);
        Mockito.when(fhirClient.lookupOrganizationBySorCode(context.getOrgId())).thenReturn(Optional.of(organization));

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.validateAccess(List.of(resource1, resource2)));
    }

    @Test
    public void whenGettingOrganisationGivenNoSorCodeThenReturnError() {
        var resource1 = buildResource(ORGANIZATION_ID_1);

        var context = new UserContext("");
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);

        assertThrows(AccessValidationException.class, () -> subject.validateAccess(resource1));
    }

    @Test
    public void validateAccess_conjunction_success() {
        // Arrange
        var resource1 = buildResource(ORGANIZATION_ID_1);
        var resource2 = buildResource(ORGANIZATION_ID_1);

        var context = new UserContext(SOR_CODE_1);
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);

        var organization = buildOrganization(ORGANIZATION_ID_1);
        Mockito.when(fhirClient.lookupOrganizationBySorCode(context.getOrgId())).thenReturn(Optional.of(organization));

        // Act

        // Assert
        assertDoesNotThrow(() -> subject.validateAccess(List.of(resource1, resource2)));
    }

    private DomainResource buildResource() {
        return buildResource(null);
    }

    private DomainResource buildResource(String organizationId) {
        var resource = new CarePlan();

        if(organizationId != null) {
            resource.addExtension(Systems.ORGANIZATION, new Reference(organizationId));
        }

        return resource;
    }

    private Organization buildOrganization(String organizationId) {
        var organization = new Organization();

        organization.setId(organizationId);

        return organization;
    }
}
