package dk.kvalitetsit.hjemmebehandling.context;

import com.auth0.jwt.interfaces.DecodedJWT;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.OrganizationModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.UserContextModel;
import dk.kvalitetsit.hjemmebehandling.repository.OrganizationRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.Organization;
import org.openapitools.model.NameDto;

import java.util.List;
import java.util.Optional;

public class DIASUserContextHandler implements IUserContextHandler {

    private static final String FULL_NAME = "FullName";
    private static final String FIRST_NAME = "FirstName";
    private static final String SUR_NAME = "SurName";
    private static final String BSK_DIAS_ENTITLEMENTS = "bSKDIASEntitlements";
    private static final String SOR_ID = "SORID";
    private static final String AUTORISATIONS_IDS = "autorisationsids";
    private static final String REGIONS_ID = "RegionsID";
    private static final String EMAIL = "email";
    private static final String BSK_AUTORISATIONS_INFORMATION = "bSKAutorisationsInformation";
    private final OrganizationRepository<Organization> organizationRepository;

    public DIASUserContextHandler(OrganizationRepository<Organization> organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    public UserContextModel mapTokenToUserContext(FhirClient client, DecodedJWT jwt) throws ServiceException {
        Optional.ofNullable(jwt).orElseThrow(() -> new IllegalArgumentException("Expected a JSON Web token, but it is missing"));

        var builder = UserContextModel.builder();

        Optional.ofNullable(jwt.getClaim(DIASUserContextHandler.SOR_ID))
                .map(Object::toString)
                .map(QualifiedId.OrganizationId::new)
                .ifPresent(SOR -> {
                    try {
                        Optional<Organization> organization = organizationRepository.lookupOrganizationBySorCode(SOR);
                        var orgName = organization.map(Organization::getName).orElse(null);
                        builder.organization(new OrganizationModel(SOR, orgName));
                    } catch (ServiceException e) {
                        throw new RuntimeException(e);
                    }
                });


        // The fullname is ignored since this may be derived from the first/lastname
        //context.setFullName(Optional.ofNullable(jwt.getClaim(DIASUserContextHandler.FULL_NAME)).map(Object::toString));


        var name = new NameDto()
                .family(jwt.getClaim(DIASUserContextHandler.SUR_NAME).toString())
                .given(jwt.getClaim(DIASUserContextHandler.FIRST_NAME).toString());

        return builder
                .name(name)
                .userId(jwt.getClaim(DIASUserContextHandler.REGIONS_ID).asString())
                .email(jwt.getClaim(DIASUserContextHandler.EMAIL).asString())
                .entitlements(Optional.ofNullable(jwt.getClaim(DIASUserContextHandler.BSK_DIAS_ENTITLEMENTS)).map(x -> List.of(x.toString())).orElse(null))
                .authorizationIds(Optional.ofNullable(jwt.getClaim(DIASUserContextHandler.AUTORISATIONS_IDS)).map(x -> List.of(x.toString())).orElse(null))
                .build();

    }

}
