package dk.kvalitetsit.hjemmebehandling.service.access;

import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.BaseModel;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.UserContextModel;
import dk.kvalitetsit.hjemmebehandling.repository.OrganizationRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.UserContext;

import java.util.List;
import java.util.Optional;

import static dk.kvalitetsit.hjemmebehandling.service.Constants.*;
import static dk.kvalitetsit.hjemmebehandling.service.MockFactory.buildOrganization;
import static dk.kvalitetsit.hjemmebehandling.service.MockFactory.buildResource;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AccessValidatorTest {


    @InjectMocks
    private AccessValidator subject;

    @Mock
    private UserContextProvider userContextProvider;

    @Mock
    private OrganizationRepository<Organization> organisationRepository;

    @Test
    public void validateAccess_contextNotInitialized() {
        var resource = buildResource();
        assertThrows(IllegalStateException.class, () -> subject.validateAccess(resource));
    }

    @Test
    public void validateAccess_unknownOrganization() throws ServiceException {
        var resource = buildResource();
        var context = UserContextModel.builder().orgId(new QualifiedId.OrganizationId(SOR_CODE_1)).build();
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);
        Mockito.when(organisationRepository.lookupOrganizationBySorCode(context.orgId().get())).thenReturn(Optional.empty());
        assertThrows(AccessValidationException.class, () -> subject.validateAccess(resource));
    }

    @Test
    public void validateAccess_noOrganizationTag() throws ServiceException {
        var resource = buildResource();
        var context = UserContextModel.builder().orgId(new QualifiedId.OrganizationId(SOR_CODE_1)).build();
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);
        var organization = buildOrganization();
        Mockito.when(organisationRepository.lookupOrganizationBySorCode(context.orgId().get())).thenReturn(Optional.of(organization));
        assertThrows(IllegalStateException.class, () -> subject.validateAccess(resource));
    }

    @Test
    public void validateAccess_wrongOrganization_accessViolation() throws ServiceException {
        var resource = buildResource(ORGANIZATION_ID_2);
        var context = UserContextModel.builder().orgId(new QualifiedId.OrganizationId(SOR_CODE_1)).build();
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);
        var organization = buildOrganization();
        Mockito.when(organisationRepository.lookupOrganizationBySorCode(context.orgId().get())).thenReturn(Optional.of(organization));
        assertThrows(AccessValidationException.class, () -> subject.validateAccess(resource));
    }

    @Test
    public void validateAccess_correctOrganization_success() throws ServiceException {
        var resource = buildResource(ORGANIZATION_ID_1);
        var context = UserContextModel.builder().orgId(new QualifiedId.OrganizationId(SOR_CODE_1)).build();
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);
        var organization = buildOrganization();
        Mockito.when(organisationRepository.lookupOrganizationBySorCode(context.orgId().get())).thenReturn(Optional.of(organization));
        assertDoesNotThrow(() -> subject.validateAccess(resource));
    }

    @Test
    public void validateAccess_conjunction_failure() throws ServiceException {
        var resource1 = buildResource(ORGANIZATION_ID_1);
        var resource2 = buildResource(ORGANIZATION_ID_2);
        var context = UserContextModel.builder().orgId(new QualifiedId.OrganizationId(SOR_CODE_1)).build();
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);
        var organization = buildOrganization();
        Mockito.when(organisationRepository.lookupOrganizationBySorCode(context.orgId().get())).thenReturn(Optional.of(organization));
        assertThrows(AccessValidationException.class, () -> subject.validateAccess(List.of(resource1, resource2)));
    }

    @Test
    public void whenGettingOrganisationGivenNoSorCodeThenReturnError() {
        var resource1 = buildResource(ORGANIZATION_ID_1);
        var context = UserContextModel.builder().orgId(new QualifiedId.OrganizationId("organization")).build();
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);
        assertThrows(AccessValidationException.class, () -> subject.validateAccess(resource1));
    }

    @Test
    public void validateAccess_conjunction_success() throws ServiceException {
        var resource1 = buildResource(ORGANIZATION_ID_1);
        var resource2 = buildResource(ORGANIZATION_ID_1);
        var context = UserContextModel.builder().orgId(new QualifiedId.OrganizationId(SOR_CODE_1)).build();
        Mockito.when(userContextProvider.getUserContext()).thenReturn(context);
        var organization = buildOrganization();
        Mockito.when(organisationRepository.lookupOrganizationBySorCode(context.orgId().get())).thenReturn(Optional.of(organization));
        assertDoesNotThrow(() -> subject.validateAccess(List.of(resource1, resource2)));
    }


}
