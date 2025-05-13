package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.ICriterion;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.model.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Concrete client implementation which covers the communication with the fhir server
 */
public class FhirClient {
    private static final Logger logger = LoggerFactory.getLogger(FhirClient.class);
    private static final List<ResourceType> UNTAGGED_RESOURCE_TYPES = List.of(ResourceType.Patient);
    private final UserContextProvider userContextProvider;
    private final IGenericClient client;


    public FhirClient(FhirContext context, String endpoint, UserContextProvider userContextProvider) {
        this.userContextProvider = userContextProvider;
        this.client = context.newRestfulGenericClient(endpoint);
    }

    // TODO: Below is unused consider deleting
    public static List<String> getPractitionerIds(List<QuestionnaireResponse> questionnaireResponses) {
        return questionnaireResponses.stream().map(qr -> ExtensionMapper.tryExtractExaminationAuthorPractitionerId(qr.getExtension())).filter(Objects::nonNull).distinct().toList();
    }

    // TODO: This should not return fhirLookupResult But "T"
    public <T extends Resource> FhirLookupResult lookupByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria, List<Include> includes, boolean withOrganizations, Optional<SortSpec> sortSpec, Optional<Integer> offset, Optional<Integer> count) throws ServiceException {

        var organizationId = userContextProvider.getUserContext().orgId().orElseThrow(() -> new ServiceException("Expected organisation id", ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE));
        var organizationCriterion = FhirUtils.buildOrganizationCriterion(organizationId);

        // Adding organizationCriterion in order to avoid doing this in repositories
        criteria = Stream.concat(Stream.of(organizationCriterion),criteria.stream()).toList();

        var query = client.search().forResource(resourceClass);
        if (!criteria.isEmpty()) {
            query = query.where(criteria.getFirst());
            for (int i = 1; i < criteria.size(); i++) {
                query = query.and(criteria.get(i));
            }
        }
        if (includes != null) {
            for (var include : includes) {
                query = query.include(include);
            }
        }
        if (sortSpec.isPresent()) {
            query = query.sort(sortSpec.get());
        }
        if (offset.isPresent()) {
            query = query.offset(offset.get());
        }
        if (count.isPresent()) {
            query = query.count(count.get());
        }

        Bundle bundle = (Bundle) query.execute();
        FhirLookupResult lookupResult = FhirLookupResult.fromBundle(bundle);
        if (withOrganizations) {
            List<String> organizationIds = lookupResult.values().stream().map(r -> ExtensionMapper.tryExtractOrganizationId(r.getExtension())).filter(Optional::isPresent).map(Optional::get).distinct().toList();

            // TODO: Uncomment below
            // lookupResult = lookupResult.merge(lookupOrganizations(organizationIds));
        }
        return lookupResult;
    }

    public  <T extends Resource> FhirLookupResult lookup(Class<T> resourceClass) throws ServiceException {
        return lookupByCriteria(resourceClass,null, null);
    }

    public  <T extends Resource> FhirLookupResult lookupByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria) throws ServiceException {
        return lookupByCriteria(resourceClass, criteria, null);
    }

    public  <T extends Resource> FhirLookupResult lookupByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria, List<Include> includes) throws ServiceException {
        boolean withOrganizations = true;
        return lookupByCriteria(resourceClass, criteria, includes, withOrganizations, Optional.empty(), Optional.empty(), Optional.empty());
    }


    public Optional<String> saveInTransaction(Bundle transactionBundle, ResourceType resourceType) {

        // Execute the transaction
        var responseBundle = client.transaction().withBundle(transactionBundle).execute();

        // Look for an entry with status 201 to retrieve the location-header.
        var id = "";
        for (var responseEntry : responseBundle.getEntry()) {
            var status = responseEntry.getResponse().getStatus();
            var location = responseEntry.getResponse().getLocation();
            if (status.startsWith("201") && location.startsWith(resourceType.toString())) {
                id = location.replaceFirst("/_history.*$", "");
            }
        }

        if (id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    public void addOrganizationTag(Bundle bundle) throws ServiceException {
        for (var entry : bundle.getEntry()) {
            addOrganizationTag(entry.getResource());
        }
    }

    public <T extends Resource> String saveResource(Resource resource) throws ServiceException {
        addOrganizationTag(resource);

        MethodOutcome outcome = client.create().resource(resource).execute();

        if (!outcome.getCreated()) {
            throw new IllegalStateException(String.format("Tried to create resource of type %s, but it was not created!", resource.getResourceType().name()));
        }
        return outcome.getId().toUnqualifiedVersionless().getIdPart();
    }

    @NotNull
    public ArrayList<ICriterion<?>> createCriteria(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        var organizationId = userContextProvider.getUserContext().orgId().orElseThrow(() -> new ServiceException("Expected organisation id", ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE));
        var organizationCriterion = FhirUtils.buildOrganizationCriterion(organizationId);
        var criteria = new ArrayList<ICriterion<?>>(List.of(organizationCriterion));

        // The criterion expresses that the careplan must no longer be satisfied at the given point in time.
        if (onlyUnSatisfied) {
            var satisfiedUntilCriterion = new DateClientParam(SearchParameters.CAREPLAN_SATISFIED_UNTIL).before().millis(Date.from(unsatisfiedToDate));
            criteria.add(satisfiedUntilCriterion);
        }

        if (onlyActiveCarePlans) {
            var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
            criteria.add(statusCriterion);
        }
        return criteria;
    }

    public  <T extends Resource> void updateResource(Resource resource) {
        client.update().resource(resource).execute();
    }

    private void addOrganizationTag(Resource resource) throws ServiceException {
        if (resource.getResourceType() == ResourceType.Bundle) {
            addOrganizationTag((Bundle) resource);
        }
        if (resource instanceof DomainResource) {
            addOrganizationTag((DomainResource) resource);
        } else {
            throw new IllegalArgumentException(String.format("Trying to add organization tag to resource %s, but the resource was of incorrect type %s!", resource.getId(), resource.getResourceType()));
        }
    }

    private void addOrganizationTag(DomainResource extendable) throws ServiceException {
        if (excludeFromOrganizationTagging(extendable)) {
            return;
        }
        if (extendable.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.ORGANIZATION))) {
            throw new IllegalArgumentException(String.format("Trying to add organization tag to resource, but the tag was already present! - %S", extendable.getId()));
        }
        var organizationId = userContextProvider.getUserContext().orgId().orElseThrow(() -> new ServiceException("Expected organisation id", ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE));
        extendable.addExtension(Systems.ORGANIZATION, new Reference(organizationId.unqualified()));
    }

    private boolean excludeFromOrganizationTagging(DomainResource extendable) {
        return UNTAGGED_RESOURCE_TYPES.contains(extendable.getResourceType());
    }


    public List<Questionnaire> lookupHistorical(List<QualifiedId.QuestionnaireId> ids, Class<Questionnaire> questionnaireClass) {
        List<Questionnaire> resources = new LinkedList<>();
        ids.forEach(id -> {
            Bundle bundle = client.history().onInstance(new IdType(questionnaireClass.getTypeName(), id.unqualified())).returnBundle(Bundle.class).execute();
            bundle.getEntry().stream().filter(bec -> bec.getResource() != null).forEach(x -> resources.add((Questionnaire) x.getResource()));
        });
        return resources;
    }
}
