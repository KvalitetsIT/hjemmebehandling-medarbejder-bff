package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.model.OrganizationModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.model.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Concrete client implementation which covers the communication with the fhir server
 */
public class FhirClient {
    private static final List<ResourceType> UNTAGGED_RESOURCE_TYPES = List.of(ResourceType.Patient);
    private final UserContextProvider userContextProvider;
    private final IGenericClient client;

    public FhirClient(FhirContext context, String endpoint, UserContextProvider userContextProvider) {
        this.userContextProvider = userContextProvider;
        this.client = context.newRestfulGenericClient(endpoint);
    }

    private static <T extends Resource> @NotNull List<T> fetch(IQuery<IBaseBundle> query, Class<T> clazz) {
        var bundle = (Bundle) query.execute();
        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .toList();
    }

    public <T extends Resource> List<T> lookupByCriteria(
            Class<T> resourceClass,
            List<ICriterion<?>> criteria,
            List<Include> includes,
            Optional<SortSpec> sortSpec,
            Optional<Integer> offset,
            Optional<Integer> count
    ) throws ServiceException {

        List<ICriterion<?>> allCriteria = new ArrayList<>();
        allCriteria.add(FhirUtils.buildOrganizationCriterion(getRequiredOrganizationId()));
        if (criteria != null) allCriteria.addAll(criteria);

        var query = client.search().forResource(resourceClass);

        for (int i = 0; i < allCriteria.size(); i++) {
            query = (i == 0) ? query.where(allCriteria.get(i)) : query.and(allCriteria.get(i));
        }

        if (includes != null) includes.forEach(query::include);

        sortSpec.ifPresent(query::sort);
        offset.ifPresent(query::offset);
        count.ifPresent(query::count);

        return fetch(query, resourceClass);
    }

    public <T extends Resource> List<T> lookup(Class<T> resourceClass) throws ServiceException {
        return lookupByCriteria(resourceClass, Collections.emptyList(), Collections.emptyList());
    }

    public <T extends Resource> List<T> lookupByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria) throws ServiceException {
        return lookupByCriteria(resourceClass, criteria, Collections.emptyList());
    }

    public <T extends Resource> List<T> lookupByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria, List<Include> includes) throws ServiceException {
        return lookupByCriteria(resourceClass, criteria, includes, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Optional<String> saveInTransaction(Bundle transactionBundle, ResourceType resourceType) {
        var responseBundle = client.transaction().withBundle(transactionBundle).execute();

        return responseBundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResponse)
                .filter(response -> response.getStatus().startsWith("201") && response.getLocation().startsWith(resourceType.toString()))
                .map(response -> response.getLocation().replaceFirst("/_history.*$", ""))
                .findFirst();
    }

    public <T extends Resource> String saveResource(Resource resource) throws ServiceException {
        addOrganizationTag(resource);
        var outcome = client.create().resource(resource).execute();

        if (!outcome.getCreated())
            throw new IllegalStateException("Resource not created: " + resource.getResourceType());

        return outcome.getId().toUnqualifiedVersionless().getIdPart();
    }

    public void addOrganizationTag(Resource resource) throws ServiceException {
        if (resource.getResourceType() == ResourceType.Bundle) addOrganizationTag(resource);
        else if (resource instanceof DomainResource domainResource) addOrganizationTag(domainResource);
        else throw new IllegalArgumentException("Unsupported resource type for tagging: " + resource.getResourceType());
    }

    public <T extends Resource> void updateResource(Resource resource) {
        client.update().resource(resource).execute();
    }

    public <T extends Resource> List<T> lookupHistorical(List<? extends QualifiedId> ids, Class<T> clazz) {
        return ids.stream()
                .flatMap(id -> lookupHistorical(id, clazz).stream())
                .toList();
    }

    public <T extends Resource> List<T> lookupHistorical(QualifiedId id, Class<T> clazz) {
        var bundle = client.history()
                .onInstance(new IdType(clazz.getTypeName(), id.unqualified()))
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .toList();
    }

    public ArrayList<ICriterion<?>> createCriteria(Instant unsatisfiedToDate, boolean onlyActive, boolean onlyUnsatisfied) throws ServiceException {
        var criteria = new ArrayList<ICriterion<?>>();
        criteria.add(FhirUtils.buildOrganizationCriterion(getRequiredOrganizationId()));

        if (onlyUnsatisfied)
            criteria.add(new DateClientParam(SearchParameters.CAREPLAN_SATISFIED_UNTIL).before().millis(Date.from(unsatisfiedToDate)));

        if (onlyActive) criteria.add(CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode()));

        return criteria;
    }

    private void addOrganizationTag(DomainResource resource) throws ServiceException {
        if (excludeFromOrganizationTagging(resource)) return;

        boolean alreadyTagged = resource.getExtension().stream()
                .anyMatch(e -> e.getUrl().equals(Systems.ORGANIZATION));

        if (alreadyTagged)
            throw new IllegalArgumentException("Resource already has organization tag: " + resource.getId());

        resource.addExtension(Systems.ORGANIZATION, new Reference(getRequiredOrganizationId().unqualified()));
    }

    private boolean excludeFromOrganizationTagging(DomainResource resource) {
        return UNTAGGED_RESOURCE_TYPES.contains(resource.getResourceType());
    }

    private QualifiedId.OrganizationId getRequiredOrganizationId() throws ServiceException {
        return userContextProvider.getUserContext()
                .organization()
                .map(OrganizationModel::id)
                .orElseThrow(() -> new ServiceException("Expected organisation id", ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE));
    }
}
