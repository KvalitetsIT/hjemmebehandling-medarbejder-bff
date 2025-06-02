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
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(FhirClient.class);
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

    @NotNull
    private static List<Resource> getResources(List<Bundle.BundleEntryComponent> entries) {
        return entries.stream()
                .map(Bundle.BundleEntryComponent::getResponse)
                .peek(response -> {
                    if (!response.getStatus().startsWith("201")) {
                        logger.error("Expected status 201, but was: {}", response.getStatus());
                    }
                })
                .filter(response -> response.getStatus().startsWith("201"))
                .map(Bundle.BundleEntryResponseComponent::getOutcome)
                .toList();
    }

    public <T extends Resource> List<T> fetchByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria, List<Include> includes, SortSpec sortSpec, Pagination pagination) throws ServiceException {
        var query = createQuery(resourceClass, criteria, includes)
                .sort(sortSpec)
                .offset(pagination.offset().get())
                .count(pagination.limit().get());

        return fetch(query, resourceClass);
    }

    public <T extends Resource> List<T> fetchByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria, List<Include> includes, SortSpec sortSpec) throws ServiceException {
        var query = createQuery(resourceClass, criteria, includes).sort(sortSpec);
        return fetch(query, resourceClass);
    }

    public <T extends Resource> List<T> fetch(Class<T> resourceClass) throws ServiceException {
        return fetchByCriteria(resourceClass);
    }

    public <T extends Resource> List<T> fetchByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria) throws ServiceException {
        return fetchByCriteria(resourceClass, criteria, Collections.emptyList());
    }

    public <T extends Resource> List<T> fetchByCriteria(Class<T> resourceClass) throws ServiceException {
        var query = client.search().forResource(resourceClass);
        return fetch(query, resourceClass);
    }

    public <T extends Resource> List<T> fetchByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria, List<Include> includes) throws ServiceException {
        var query = createQuery(resourceClass, criteria, includes);
        return fetch(query, resourceClass);
    }

    public Optional<String> saveInTransaction(Bundle transactionBundle, ResourceType resourceType) {
        var responseBundle = client.transaction().withBundle(transactionBundle).execute();

        return responseBundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResponse)
                .filter(response -> response.getStatus().startsWith("201") && response.getLocation().startsWith(resourceType.toString()))
                .map(response -> response.getLocation().replaceFirst("/_history.*$", ""))
                .findFirst();
    }

    /**
     * This method is supposed to add multiple resources as one bundle resulting in a single server-call and hopefully a reliable transaction
     * @param resources related resources that are expecting a cascading operation
     * @return All the resource which were added during the execution
     */
    public List<Resource> postAll(Resource... resources) throws ServiceException {
        for (Resource resource : resources) {
            addOrganizationTag(resource);
        }
        var bundle = new BundleBuilder().buildBundle(Bundle.HTTPVerb.POST, resources);
        var responseBundle = client.transaction().withBundle(bundle).execute();
        var entries = responseBundle.getEntry();
        return getResources(entries);
    }

    /**
     * This method is supposed to update multiple resources as one bundle resulting in a single server-call and hopefully a reliable transaction
     * @param resources related resources that are expecting a cascading operation
     * @return All the resource which were added during the execution
     */
    public List<Resource> putAll(Resource... resources) {
        var bundle = new BundleBuilder().buildBundle(Bundle.HTTPVerb.PUT, resources);
        var responseBundle = client.transaction().withBundle(bundle).execute();
        var entries = responseBundle.getEntry();
        return getResources(entries);
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

    public <T extends Resource> List<T> fetchHistorical(List<? extends QualifiedId> ids, Class<T> clazz) {
        return ids.stream()
                .flatMap(id -> fetchHistorical(id, clazz).stream())
                .toList();
    }

    public <T extends Resource> List<T> fetchHistorical(QualifiedId id, Class<T> clazz) {
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
            throw new IllegalStateException("Resource already has organization tag: " + resource.getId());

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


    private <T extends Resource> IQuery<IBaseBundle> createQuery(Class<T> resourceClass, List<ICriterion<?>> criteria) throws ServiceException {
        var query = client.search().forResource(resourceClass);

        List<ICriterion<?>> allCriteria = new ArrayList<>();
        allCriteria.add(FhirUtils.buildOrganizationCriterion(getRequiredOrganizationId()));
        if (criteria != null) allCriteria.addAll(criteria);

        for (int i = 0; i < allCriteria.size(); i++) {
            query = (i == 0) ? query.where(allCriteria.get(i)) : query.and(allCriteria.get(i));
        }
        return query;
    }

    private <T extends Resource> IQuery<IBaseBundle> createQuery(Class<T> resourceClass, List<ICriterion<?>> criteria, List<Include> includes) throws ServiceException {
        var query = createQuery(resourceClass, criteria);
        if (includes != null) includes.forEach(query::include);
        return query;
    }

}
