package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FhirClient {
    private static final Logger logger = LoggerFactory.getLogger(FhirClient.class);

    private FhirContext context;
    private String endpoint;
    private UserContextProvider userContextProvider;

    private static final List<ResourceType> UNTAGGED_RESOURCE_TYPES = List.of(ResourceType.Patient);

    public FhirClient(FhirContext context, String endpoint, UserContextProvider userContextProvider) {
        this.context = context;
        this.endpoint = endpoint;
        this.userContextProvider = userContextProvider;
    }

    public FhirLookupResult lookupCarePlansByPatientId_new(String patientId) {
        var patientCriterion = CarePlan.PATIENT.hasId(patientId);
        var organizationCriterion = buildOrganizationCriterion();

        return lookupCarePlansByCriteria(List.of(patientCriterion, organizationCriterion));
    }

    public List<CarePlan> lookupCarePlansByPatientId(String patientId) {
        var patientCriterion = CarePlan.PATIENT.hasId(patientId);
        var organizationCriterion = buildOrganizationCriterion();

        return lookupByCriteria(CarePlan.class, patientCriterion, organizationCriterion);
    }

    public FhirLookupResult lookupCarePlansUnsatisfiedAt_new(Instant pointInTime) {
        // The criterion expresses that the careplan must no longer be satisfied at the given point in time.
        var satisfiedUntilCriterion = new DateClientParam(SearchParameters.CAREPLAN_SATISFIED_UNTIL).before().millis(Date.from(pointInTime));
        var organizationCriterion = buildOrganizationCriterion();

        return lookupCarePlansByCriteria(List.of(satisfiedUntilCriterion, organizationCriterion));
    }

    public List<CarePlan> lookupCarePlansUnsatisfiedAt(Instant pointInTime) {
        // The criterion expresses that the careplan must no longer be satisfied at the given point in time.
        var satisfiedUntilCriterion = new DateClientParam(SearchParameters.CAREPLAN_SATISFIED_UNTIL).before().millis(Date.from(pointInTime));
        var organizationCriterion = buildOrganizationCriterion();

        return lookupByCriteria(CarePlan.class, satisfiedUntilCriterion, organizationCriterion);
    }

    public FhirLookupResult lookupCarePlanById_new(String carePlanId) {
        var idCriterion = CarePlan.RES_ID.exactly().code(carePlanId);

        return lookupCarePlansByCriteria(List.of(idCriterion));
    }

    public Optional<CarePlan> lookupCarePlanById(String carePlanId) {
        return lookupById(carePlanId, CarePlan.class);
    }

    public Optional<Organization> lookupOrganizationBySorCode(String sorCode) {
        return lookupSingletonByCriterion(Organization.class, Organization.IDENTIFIER.exactly().systemAndValues(Systems.SOR, sorCode));
    }

    public Optional<Patient> lookupPatientById(String patientId) {
        return lookupById(patientId, Patient.class);
    }

    public Optional<Patient> lookupPatientByCpr_new(String cpr) {
        var cprCriterion = Patient.IDENTIFIER.exactly().systemAndValues(Systems.CPR, cpr);

        var lookupResult = lookup_new(Patient.class, List.of(cprCriterion));

        if(lookupResult.getPatients().isEmpty()) {
            return Optional.empty();
        }
        if(lookupResult.getPatients().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", Patient.class));
        }
        return Optional.of(lookupResult.getPatients().get(0));
    }

    public Optional<Patient> lookupPatientByCpr(String cpr) {
        var cprCriterion = Patient.IDENTIFIER.exactly().systemAndValues(Systems.CPR, cpr);

        return lookupSingletonByCriterion(Patient.class, cprCriterion);
    }

    public List<Patient> lookupPatientsById(Collection<String> patientIds) {
        return lookupByCriterion(Patient.class, Patient.RES_ID.exactly().codes(patientIds));
    }

    public Optional<QuestionnaireResponse> lookupQuestionnaireResponseById(String questionnaireResponseId) {
        return lookupById(questionnaireResponseId, QuestionnaireResponse.class);
    }

    public FhirLookupResult lookupQuestionnaireResponses_new(String carePlanId, List<String> questionnaireIds) {
        var questionnaireCriterion = QuestionnaireResponse.QUESTIONNAIRE.hasAnyOfIds(questionnaireIds);
        var basedOnCriterion = QuestionnaireResponse.BASED_ON.hasId(carePlanId);

        return lookup_new(QuestionnaireResponse.class, List.of(questionnaireCriterion, basedOnCriterion), List.of(QuestionnaireResponse.INCLUDE_BASED_ON, QuestionnaireResponse.INCLUDE_QUESTIONNAIRE, QuestionnaireResponse.INCLUDE_SUBJECT));
    }

    public List<QuestionnaireResponse> lookupQuestionnaireResponses(String carePlanId, List<String> questionnaireIds) {
        var questionnaireCriterion = QuestionnaireResponse.QUESTIONNAIRE.hasAnyOfIds(questionnaireIds);
        var basedOnCriterion = QuestionnaireResponse.BASED_ON.hasId(carePlanId);
        return  lookupByCriteria(QuestionnaireResponse.class, questionnaireCriterion, basedOnCriterion);
    }

    public List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) {
        var codes = statuses.stream().map(s -> s.toString()).collect(Collectors.toList());
        var statusCriterion = new TokenClientParam(SearchParameters.EXAMINATION_STATUS).exactly().codes(codes.toArray(new String[codes.size()]));

        var organizationCriterion = buildOrganizationCriterion();

        return lookupByCriteria(QuestionnaireResponse.class, statusCriterion, organizationCriterion);
    }

    public List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatus(ExaminationStatus status) {
        return lookupQuestionnaireResponsesByStatus(List.of(status));
    }

    public String saveCarePlan(CarePlan carePlan) {
        return save(carePlan);
    }

    public String saveCarePlan(CarePlan carePlan, Patient patient) {
        // Build a transaction bundle.
        var bundle = new BundleBuilder().buildCarePlanBundle(carePlan, patient);

        return saveInTransaction(bundle, ResourceType.CarePlan);
    }

    public String savePatient(Patient patient) {
        return save(patient);
    }

    public String saveQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) {
        return save(questionnaireResponse);
    }

    public Optional<PlanDefinition> lookupPlanDefinition(String planDefinitionId) {
        return lookupById(planDefinitionId, PlanDefinition.class);
    }

    public FhirLookupResult lookupPlanDefinitions() {
        var organizationCriterion = buildOrganizationCriterion();
        // Includes the Questionnaire resources.
        var definitionInclude = PlanDefinition.INCLUDE_DEFINITION;

        return lookup_new(PlanDefinition.class, List.of(organizationCriterion), List.of(definitionInclude));
    }

    public FhirLookupResult lookupPlanDefinitions_new(Collection<String> planDefinitionIds) {
        var idCriterion = PlanDefinition.RES_ID.exactly().codes(planDefinitionIds);

        return lookup_new(PlanDefinition.class, List.of(idCriterion));
    }

    public List<PlanDefinition> lookupPlanDefinitions(Collection<String> planDefinitionIds) {
        return lookupByCriterion(PlanDefinition.class, PlanDefinition.RES_ID.exactly().codes(planDefinitionIds));
    }

    public FhirLookupResult lookupQuestionnaires_new(Collection<String> questionnaireIds) {
        var idCriterion = Questionnaire.RES_ID.exactly().codes(questionnaireIds);
        var organizationCriterion = buildOrganizationCriterion();

        return lookup_new(Questionnaire.class, List.of(idCriterion, organizationCriterion));
    }

    public List<Questionnaire> lookupQuestionnaires(Collection<String> questionnaireIds) {
        return lookupByCriterion(Questionnaire.class, Questionnaire.RES_ID.exactly().codes(questionnaireIds));
    }

    public void updateCarePlan(CarePlan carePlan) {
        update(carePlan);
    }

    public void updateQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) {
        update(questionnaireResponse);
    }

    private FhirLookupResult lookupCarePlansByCriteria(List<ICriterion<?>> criteria) {
        var carePlanResult = lookup_new(CarePlan.class, criteria, List.of(CarePlan.INCLUDE_SUBJECT, CarePlan.INCLUDE_INSTANTIATES_CANONICAL));

        // The FhirLookupResult includes the patient- and plandefinition-resources that we need,
        // but due to limitations of the FHIR server, not the questionnaire-resources. Se wo look up those in a separate call.
        if(carePlanResult.getCarePlans().isEmpty()) {
            return carePlanResult;
        }

        // Get the related questionnaire-resources
        List<String> questionnaireIds = getQuestionnaireIds(carePlanResult.getCarePlans());
        FhirLookupResult questionnaireResult = lookupQuestionnaires_new(questionnaireIds);

        // Merge the results
        return carePlanResult.merge(questionnaireResult);
    }

    private List<String> getQuestionnaireIds(List<CarePlan> carePlans) {
        return carePlans
                .stream()
                .flatMap(cp -> cp.getActivity().stream().map(a -> getQuestionnaireId(a.getDetail())))
                .collect(Collectors.toList());
    }

    private String getQuestionnaireId(CarePlan.CarePlanActivityDetailComponent detail) {
        if(detail.getInstantiatesCanonical() == null || detail.getInstantiatesCanonical().size() != 1) {
            throw new IllegalStateException("Expected InstantiatesCanonical to be present, and to contain exactly one value!");
        }
        return detail.getInstantiatesCanonical().get(0).getValue();
    }

    private <T extends Resource> FhirLookupResult lookup_new(Class<T> resourceClass, List<ICriterion<?>> criteria) {
        return lookup_new(resourceClass, criteria, null);
    }

    private <T extends Resource> FhirLookupResult lookup_new(Class<T> resourceClass, List<ICriterion<?>> criteria, List<Include> includes) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        var query = client
                .search()
                .forResource(resourceClass);
        if(criteria != null && criteria.size() > 0) {
            query = query.where(criteria.get(0));
            for(int i = 1; i < criteria.size(); i++) {
                query = query.and(criteria.get(i));
            }
        }
        if(includes != null) {
            for(var include : includes) {
                query = query.include(include);
            }
        }

        Bundle bundle = (Bundle) query.execute();
        return FhirLookupResult.fromBundle(bundle);
    }

    private <T extends Resource> Optional<T> lookupSingletonByCriterion(Class<T> resourceClass, ICriterion<?> criterion) {
        List<T> result = lookupByCriterion(resourceClass, criterion);

        if(result == null || result.isEmpty()) {
            return Optional.empty();
        }
        if(result.size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", resourceClass.getName()));
        }
        return Optional.of(result.get(0));
    }

    private <T extends Resource> List<T> lookupByCriterion(Class<T> resourceClass, ICriterion<?> criterion) {
        return lookupByCriteria(resourceClass, criterion);
    }

    private <T extends Resource> List<T> lookupByCriteria(Class<T> resourceClass, ICriterion<?>... criteria) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        var query = client
                .search()
                .forResource(resourceClass);
        if(criteria.length > 0) {
            query = query.where(criteria[0]);
            for(int i = 1; i < criteria.length; i++) {
                query = query.and(criteria[i]);
            }
        }

        Bundle bundle = (Bundle) query.execute();
        return bundle.getEntry().stream().map(e -> ((T) e.getResource())).collect(Collectors.toList());
    }

    private <T extends Resource> Optional<T> lookupById(String id, Class<T> resourceClass) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        try {
            T resource = client
                    .read()
                    .resource(resourceClass)
                    .withId(id)
                    .execute();
            return Optional.of(resource);
        }
        catch(ResourceNotFoundException e) {
            // Swallow the exception - corresponds to a 404 response
            return Optional.empty();
        }
    }

    private <T extends Resource> String save(Resource resource) {
        addOrganizationTag(resource);

        IGenericClient client = context.newRestfulGenericClient(endpoint);

        MethodOutcome outcome = client.create().resource(resource).execute();
        if(!outcome.getCreated()) {
            throw new IllegalStateException(String.format("Tried to create resource of type %s, but it was not created!", resource.getResourceType().name()));
        }
        return outcome.getId().toUnqualifiedVersionless().getIdPart();
    }

    private String saveInTransaction(Bundle transactionBundle, ResourceType resourceType) {
        addOrganizationTag(transactionBundle);

        IGenericClient client = context.newRestfulGenericClient(endpoint);

        // Execute the transaction
        var responseBundle = client.transaction().withBundle(transactionBundle).execute();

        // Locate the 'primary' entry in the response
        var id = "";
        for(var responseEntry : responseBundle.getEntry()) {
            var status = responseEntry.getResponse().getStatus();
            var location = responseEntry.getResponse().getLocation();
            if(!status.startsWith("201")) {
                throw new IllegalStateException(String.format("Creating %s failed. Received unwanted http statuscode: %s", resourceType, status));
            }
            if(location.startsWith(resourceType.toString())) {
                id = location.replaceFirst("/_history.*$", "");
            }
        }

        if(id.isEmpty()) {
            throw new IllegalStateException("Could not locate location-header in response when executing transaction.");
        }
        return id;
    }

    private <T extends Resource> void update(Resource resource) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);
        client.update().resource(resource).execute();
    }

    private void addOrganizationTag(Resource resource) {
        if(resource.getResourceType() == ResourceType.Bundle) {
            addOrganizationTag((Bundle) resource);
        }
        if(resource instanceof DomainResource) {
            addOrganizationTag((DomainResource) resource);
        }
        else {
            throw new IllegalArgumentException(String.format("Trying to add organization tag to resource %s, but the resource was of incorrect type %s!", resource.getId(), resource.getResourceType()));
        }
    }

    private void addOrganizationTag(Bundle bundle) {
        for(var entry : bundle.getEntry()) {
            addOrganizationTag(entry.getResource());
        }
    }

    private void addOrganizationTag(DomainResource extendable) {
        if(excludeFromOrganizationTagging(extendable)) {
            return;
        }
        if(extendable.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.ORGANIZATION))) {
            throw new IllegalArgumentException(String.format("Trying to add organization tag to resource, but the tag was already present!", extendable.getId()));
        }

        extendable.addExtension(Systems.ORGANIZATION, new Reference(getOrganizationId()));
    }

    private boolean excludeFromOrganizationTagging(DomainResource extendable) {
        return UNTAGGED_RESOURCE_TYPES.contains(extendable.getResourceType());
    }

    private ICriterion<?> buildOrganizationCriterion() {
        String organizationId = getOrganizationId();
        return new ReferenceClientParam(SearchParameters.ORGANIZATION).hasId(organizationId);
    }

    private String getOrganizationId() {
        var context = userContextProvider.getUserContext();
        if(context == null) {
            throw new IllegalStateException("UserContext was not initialized!");
        }

        var organization = lookupOrganizationBySorCode(context.getOrgId())
                .orElseThrow(() -> new IllegalStateException(String.format("No Organization was present for sorCode %s!", context.getOrgId())));

        var organizationId = organization.getIdElement().toUnqualifiedVersionless().getValue();
        return FhirUtils.qualifyId(organizationId, ResourceType.Organization);
    }
}
