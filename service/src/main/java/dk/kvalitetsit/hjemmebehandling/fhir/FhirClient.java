package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class FhirClient {
    private static final Logger logger = LoggerFactory.getLogger(FhirClient.class);

    private final FhirContext context;
    private final String endpoint;
    private final UserContextProvider userContextProvider;

    private static final List<ResourceType> UNTAGGED_RESOURCE_TYPES = List.of(ResourceType.Patient);

    public FhirClient(FhirContext context, String endpoint, UserContextProvider userContextProvider) {
        this.context = context;
        this.endpoint = endpoint;
        this.userContextProvider = userContextProvider;
    }

    public FhirLookupResult lookupActiveCarePlansWithQuestionnaire(String questionnaireId) throws ServiceException {

        var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
        var questionnaireCriterion = CarePlan.INSTANTIATES_CANONICAL.hasChainedProperty(PlanDefinition.DEFINITION.hasId(questionnaireId));
        var organizationCriterion = buildOrganizationCriterion();
        var criteria = new ArrayList<>(List.of(statusCriterion, questionnaireCriterion, organizationCriterion));

        return lookupCarePlansByCriteria(criteria);
    }



    public FhirLookupResult lookupActivePlanDefinitionsUsingQuestionnaireWithId(String questionnaireId) throws ServiceException {

        var statusCriterion = PlanDefinition.STATUS.exactly().code(Enumerations.PublicationStatus.ACTIVE.toCode());
        var questionnaireCriterion = PlanDefinition.DEFINITION.hasId(questionnaireId);
        var organizationCriterion = buildOrganizationCriterion();
        var criteria = new ArrayList<ICriterion<?>>(List.of(statusCriterion, questionnaireCriterion, organizationCriterion));

        return lookupPlanDefinitionsByCriteria(criteria);
    }


    public FhirLookupResult lookupActiveCarePlansWithPlanDefinition(String plandefinitionId) throws ServiceException {

        var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
        var plandefinitionCriterion = CarePlan.INSTANTIATES_CANONICAL.hasId(plandefinitionId);
        var organizationCriterion = buildOrganizationCriterion();
        var criteria = new ArrayList<ICriterion<?>>(List.of(statusCriterion, plandefinitionCriterion, organizationCriterion));

        return lookupCarePlansByCriteria(criteria);
    }

    public FhirLookupResult lookupCarePlansByPatientId(String patientId, boolean onlyActiveCarePlans) throws ServiceException {

        var patientCriterion = CarePlan.PATIENT.hasId(patientId);
        var organizationCriterion = buildOrganizationCriterion();
        var criteria = new ArrayList<ICriterion<?>>(List.of(patientCriterion, organizationCriterion));
        if(onlyActiveCarePlans) {
            var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
            criteria.add(statusCriterion);
        }

        return lookupCarePlansByCriteria(criteria);
    }

    
    public FhirLookupResult lookupCarePlans(Optional<String> cpr,Instant unsatisfiedToDate, boolean onlyActiveCarePlans,boolean onlyUnSatisfied) throws ServiceException {

        var organizationCriterion = buildOrganizationCriterion();
        var criteria = new ArrayList<ICriterion<?>>(List.of(organizationCriterion));

        // The criterion expresses that the careplan must no longer be satisfied at the given point in time.
        if(onlyUnSatisfied){
            var satisfiedUntilCriterion = new DateClientParam(SearchParameters.CAREPLAN_SATISFIED_UNTIL).before().millis(Date.from(unsatisfiedToDate));
            criteria.add(satisfiedUntilCriterion);
        }

        if(onlyActiveCarePlans) {
            var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
            criteria.add(statusCriterion);
        }

        if(cpr.isPresent()){
            Optional<Patient> patient = lookupPatientByCpr(cpr.get());
            if(patient.isEmpty()) {
                return FhirLookupResult.fromResources();
            }
            String patientId = patient.get().getIdElement().toUnqualifiedVersionless().toString();
            var patientCriterion = CarePlan.PATIENT.hasId(patientId);
            criteria.add(patientCriterion);
        }

        var sortSpec = new SortSpec(SearchParameters.CAREPLAN_SATISFIED_UNTIL, SortOrderEnum.ASC);

        return lookupCarePlansByCriteria(criteria, Optional.of(sortSpec));
    }

    public FhirLookupResult lookupCarePlanById(String carePlanId) throws ServiceException {
        var idCriterion = CarePlan.RES_ID.exactly().code(carePlanId);

        return lookupCarePlansByCriteria(List.of(idCriterion));
    }

    public Optional<Organization> lookupOrganizationBySorCode(String sorCode) throws ServiceException {
        if(sorCode == null || sorCode.isBlank() || sorCode.isEmpty()) throw new ServiceException("The SOR-code was not specified", ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE);
        var sorCodeCriterion = Organization.IDENTIFIER.exactly().systemAndValues(Systems.SOR, sorCode);

        var lookupResult = lookupOrganizationsByCriteria(List.of(sorCodeCriterion));
        if(lookupResult.getOrganizations().isEmpty()) {
            return Optional.empty();
        }
        if(lookupResult.getOrganizations().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of %s!", Organization.class));
        }
        return Optional.of(lookupResult.getOrganizations().get(0));
    }

    public Optional<Patient> lookupPatientById(String patientId) {
        var idCriterion = Patient.RES_ID.exactly().code(patientId);
        return lookupPatient(List.of(idCriterion));
    }

    public Optional<Patient> lookupPatientByCpr(String cpr) {
        var cprCriterion = Patient.IDENTIFIER.exactly().systemAndValues(Systems.CPR, cpr);
        return lookupPatient(List.of(cprCriterion));
    }

    public FhirLookupResult searchPatients(List<String> searchStrings, CarePlan.CarePlanStatus status) throws ServiceException {
        // FHIR has no way of expressing 'search patient with name like %search% OR cpr like %search%'
        // so we have to do that in two seperate queries
        var cprCriterion = CarePlan.PATIENT.hasChainedProperty(new StringClientParam("patient_identifier_cpr").matches().values(searchStrings));
        var nameCriterion = CarePlan.PATIENT.hasChainedProperty(Patient.NAME.matches().values(searchStrings));
        var organizationCriterion = buildOrganizationCriterion();
        var statusCriterion = CarePlan.STATUS.exactly().code(status.toCode());

        FhirLookupResult fhirLookupResult = lookupCarePlansByCriteria(List.of(cprCriterion, statusCriterion, organizationCriterion));
        fhirLookupResult.merge(lookupCarePlansByCriteria(List.of(nameCriterion, statusCriterion, organizationCriterion)));

        return fhirLookupResult;
    }

    public FhirLookupResult getPatientsByStatus(CarePlan.CarePlanStatus status) throws ServiceException {
        var organizationCriterion = buildOrganizationCriterion();
        var statusCriterion = CarePlan.STATUS.exactly().code(status.toCode());
        return lookupCarePlansByCriteria(List.of(statusCriterion, organizationCriterion));
    }

    public FhirLookupResult lookupQuestionnaireResponseById(String questionnaireResponseId) {
        var idCriterion = QuestionnaireResponse.RES_ID.exactly().code(questionnaireResponseId);
        return lookupQuestionnaireResponseByCriteria(List.of(idCriterion));
    }

    public FhirLookupResult lookupQuestionnaireResponses(String carePlanId, List<String> questionnaireIds) {
        var questionnaireCriterion = QuestionnaireResponse.QUESTIONNAIRE.hasAnyOfIds(questionnaireIds);
        var basedOnCriterion = QuestionnaireResponse.BASED_ON.hasId(carePlanId);

        return lookupQuestionnaireResponseByCriteria(List.of(questionnaireCriterion, basedOnCriterion));
    }

    public List<Questionnaire> lookupVersionsOfQuestionnaireById(List<String> ids){

        IGenericClient client = context.newRestfulGenericClient(endpoint);

        List<Questionnaire> resources = new LinkedList<>();

        ids.forEach( id -> {
            Bundle bundle = client.history().onInstance(new IdType("Questionnaire", id)).returnBundle(Bundle.class).execute();
            bundle.getEntry().stream().filter(bec -> bec.getResource() != null).forEach(x -> resources.add((Questionnaire) x.getResource()));
        });

        return resources;
    }

    public FhirLookupResult lookupQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException {

        var statusCriterion = new TokenClientParam(SearchParameters.EXAMINATION_STATUS).exactly().codes(statuses.stream().map(Enum::toString).toArray(String[]::new));
        var organizationCriterion = buildOrganizationCriterion();

        return lookupQuestionnaireResponseByCriteria(List.of(statusCriterion, organizationCriterion));
    }

    public FhirLookupResult lookupQuestionnaireResponsesByStatusAndCarePlanId(List<ExaminationStatus> statuses, String carePlanId) throws ServiceException {

        var statusCriterion = new TokenClientParam(SearchParameters.EXAMINATION_STATUS).exactly().codes(statuses.stream().map(Enum::toString).toArray(String[]::new));
        var organizationCriterion = buildOrganizationCriterion();
        var basedOnCriterion = QuestionnaireResponse.BASED_ON.hasId(carePlanId);

        return lookupQuestionnaireResponseByCriteria(List.of(statusCriterion, organizationCriterion, basedOnCriterion));
    }

    public FhirLookupResult lookupQuestionnaireResponsesByStatus(ExaminationStatus status) throws ServiceException {
        return lookupQuestionnaireResponsesByStatus(List.of(status));
    }

    public String saveCarePlan(CarePlan carePlan) throws ServiceException {
        return save(carePlan);
    }

    public String saveCarePlan(CarePlan carePlan, Patient patient) throws ServiceException {

        // Build a transaction bundle.
        var bundle = new BundleBuilder().buildCreateCarePlanBundle(carePlan, patient);
        addOrganizationTag(bundle);

        var id = saveInTransaction(bundle, ResourceType.CarePlan);
        if(id.isEmpty()) {
            throw new IllegalStateException("Could not locate location-header in response when executing transaction.");
        }
        return id.get();
    }

    public String savePatient(Patient patient) throws ServiceException {
        return save(patient);
    }

    public String saveQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) throws ServiceException {
        return save(questionnaireResponse);
    }

    public FhirLookupResult lookupPlanDefinition(String planDefinitionId) {
        var idCriterion = PlanDefinition.RES_ID.exactly().code(planDefinitionId);

        return lookupPlanDefinitionsByCriteria(List.of(idCriterion));
    }

    public FhirLookupResult lookupPlanDefinitions() throws ServiceException {
        var organizationCriterion = buildOrganizationCriterion();

        return lookupPlanDefinitionsByCriteria(List.of(organizationCriterion));
    }

    public FhirLookupResult lookupPlanDefinitionsById(Collection<String> planDefinitionIds) {
        var idCriterion = PlanDefinition.RES_ID.exactly().codes(planDefinitionIds);

        return lookupPlanDefinitionsByCriteria(List.of(idCriterion));
    }

    public FhirLookupResult lookupPlanDefinitionsByStatus(Collection<String> statusesToInclude) throws ServiceException {
        var organizationCriterion = buildOrganizationCriterion();
        var criterias = new ArrayList<ICriterion<?>>();
        criterias.add(organizationCriterion);

        if(!statusesToInclude.isEmpty()){
            Collection<String> statusesToIncludeToLowered = statusesToInclude.stream().map(String::toLowerCase).collect(Collectors.toList()); //status should be to lowered
            var statusCriteron = PlanDefinition.STATUS.exactly().codes(statusesToIncludeToLowered);
            criterias.add(statusCriteron);
        }
        return lookupPlanDefinitionsByCriteria(criterias);
    }

    public FhirLookupResult lookupQuestionnairesByStatus(Collection<String> statusesToInclude) throws ServiceException {
        var criterias = new ArrayList<ICriterion<?>>();
        var organizationCriterion = buildOrganizationCriterion();
        criterias.add(organizationCriterion);

        if(!statusesToInclude.isEmpty()){
            Collection<String> statusesToIncludeToLowered = statusesToInclude.stream().map(String::toLowerCase).collect(Collectors.toList()); //status should be to lowered
            var statusCriteron = Questionnaire.STATUS.exactly().codes(statusesToIncludeToLowered);
            criterias.add(statusCriteron);
        }
        return lookupByCriteria(Questionnaire.class, criterias);
    }

    public FhirLookupResult lookupQuestionnairesById(Collection<String> questionnaireIds) throws ServiceException {
        var idCriterion = Questionnaire.RES_ID.exactly().codes(questionnaireIds);
        var organizationCriterion = buildOrganizationCriterion();

        return lookupByCriteria(Questionnaire.class, List.of(idCriterion, organizationCriterion));
    }

    public void updateCarePlan(CarePlan carePlan) {
        update(carePlan);
    }

    public void updateCarePlan(CarePlan carePlan, Patient patient) {
        var bundle = new BundleBuilder().buildUpdateCarePlanBundle(carePlan, patient);

        saveInTransaction(bundle, ResourceType.CarePlan);
    }

    public void updateQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) {
        update(questionnaireResponse);
    }

    private FhirLookupResult lookupCarePlansByCriteria(List<ICriterion<?>> criteria) throws ServiceException {
        return lookupCarePlansByCriteria(criteria, Optional.empty());
    }

    private FhirLookupResult lookupCarePlansByCriteria(List<ICriterion<?>> criteria, Optional<SortSpec> sortSpec) throws ServiceException {
        boolean withOrganizations = true;
        var carePlanResult = lookupByCriteria(CarePlan.class, criteria, List.of(CarePlan.INCLUDE_SUBJECT, CarePlan.INCLUDE_INSTANTIATES_CANONICAL), withOrganizations, sortSpec, Optional.empty(), Optional.empty());

        // The FhirLookupResult includes the patient- and plandefinition-resources that we need,
        // but due to limitations of the FHIR server, not the questionnaire-resources. Se wo look up those in a separate call.
        if(carePlanResult.getCarePlans().isEmpty()) {
            return carePlanResult;
        }

        // Get the related questionnaire-resources
        List<String> questionnaireIds = new ArrayList<>();
        questionnaireIds.addAll(getQuestionnaireIdsFromCarePlan(carePlanResult.getCarePlans()));
        questionnaireIds.addAll(getQuestionnaireIdsFromPlanDefinition(carePlanResult.getPlanDefinitions()));
        FhirLookupResult questionnaireResult = lookupQuestionnairesById(questionnaireIds);

        // Merge the results
        return carePlanResult.merge(questionnaireResult);
    }

    private FhirLookupResult lookupOrganizationsByCriteria(List<ICriterion<?>> criteria) {
        // Don't try to include Organization-resources when we are looking up organizations ...
        boolean withOrganizations = false;
        return lookupByCriteria(Organization.class, criteria, List.of(), withOrganizations, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private Optional<Patient> lookupPatient(List<ICriterion<?>> criterion) {
        var lookupResult = lookupByCriteria(Patient.class, criterion);

        if(lookupResult.getPatients().isEmpty()) {
            return Optional.empty();
        }
        if(lookupResult.getPatients().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", Patient.class));
        }
        return Optional.of(lookupResult.getPatients().get(0));
    }

    private FhirLookupResult lookupPlanDefinitionsByCriteria(List<ICriterion<?>> criteria) {
        // Includes the Questionnaire resources.
        return lookupByCriteria(PlanDefinition.class, criteria, List.of(PlanDefinition.INCLUDE_DEFINITION));
    }

    private FhirLookupResult lookupQuestionnaireResponseByCriteria(List<ICriterion<?>> criteria) {
        var questionnaireResponseResult = lookupByCriteria(QuestionnaireResponse.class, criteria, List.of(QuestionnaireResponse.INCLUDE_BASED_ON, QuestionnaireResponse.INCLUDE_QUESTIONNAIRE, QuestionnaireResponse.INCLUDE_SUBJECT));

        // We also need the planDefinitions, which are found by following the chain QuestionnaireResponse.based-on -> CarePlan.instantiates-canonical.
        // This requires a separate lookup.
        if(questionnaireResponseResult.getQuestionnaireResponses().isEmpty()) {
            return questionnaireResponseResult;
        }

        // Get the related planDefinitions
        List<String> planDefinitionIds = getPlanDefinitionIds(questionnaireResponseResult.getCarePlans());
        FhirLookupResult planDefinitionResult = lookupPlanDefinitionsById(planDefinitionIds);

        // Merge the results
        questionnaireResponseResult.merge(planDefinitionResult);

        // We also need to lookup the practitioner who (potentially) changed the examination status
        List<String> practitionerIds = getPractitionerIds(questionnaireResponseResult.getQuestionnaireResponses());
        if (!practitionerIds.isEmpty()) {
            FhirLookupResult practitionerResult = lookupPractitioners(practitionerIds);
            questionnaireResponseResult.merge(practitionerResult);
        }
        return questionnaireResponseResult;
    }

    private List<String> getQuestionnaireIdsFromCarePlan(List<CarePlan> carePlans) {
        return carePlans
                .stream()
                .flatMap(cp -> cp.getActivity().stream().map(a -> getQuestionnaireId(a.getDetail())))
                .collect(Collectors.toList());
    }

    private List<String> getQuestionnaireIdsFromPlanDefinition(List<PlanDefinition> planDefinitions) {
        return planDefinitions
                .stream()
                .flatMap(pd -> pd.getAction().stream().map(a -> a.getDefinitionCanonicalType().getValue()))
                .collect(Collectors.toList());
    }

    private List<String> getPlanDefinitionIds(List<CarePlan> carePlans) {
        return carePlans
                .stream()
                .flatMap(cp -> cp.getInstantiatesCanonical().stream().map(PrimitiveType::getValue))
                .collect(Collectors.toList());
    }

    private List<String> getPractitionerIds(List<QuestionnaireResponse> questionnaireResponses) {
        return questionnaireResponses
            .stream()
            .map(qr -> ExtensionMapper.tryExtractExaminationAuthorPractitionerId(qr.getExtension()))
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    private String getQuestionnaireId(CarePlan.CarePlanActivityDetailComponent detail) {
        if(detail.getInstantiatesCanonical() == null || detail.getInstantiatesCanonical().size() != 1) {
            throw new IllegalStateException("Expected InstantiatesCanonical to be present, and to contain exactly one value!");
        }
        return detail.getInstantiatesCanonical().get(0).getValue();
    }

    private <T extends Resource> FhirLookupResult lookupByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria) {
        return lookupByCriteria(resourceClass, criteria, null);
    }

    private <T extends Resource> FhirLookupResult lookupByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria, List<Include> includes) {
        boolean withOrganizations = true;
        return lookupByCriteria(resourceClass, criteria, includes, withOrganizations, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private <T extends Resource> FhirLookupResult lookupByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria, List<Include> includes, boolean withOrganizations, Optional<SortSpec> sortSpec, Optional<Integer> offset, Optional<Integer> count) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        var query = client
                .search()
                .forResource(resourceClass);
        if(criteria != null && !criteria.isEmpty()) {
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
        if(sortSpec.isPresent()) {
            query = query.sort(sortSpec.get());
        }
        if(offset.isPresent()) {
            query = query.offset(offset.get());
        }
        if(count.isPresent()) {
            query = query.count(count.get());
        }

        Bundle bundle = (Bundle) query.execute();
        FhirLookupResult lookupResult = FhirLookupResult.fromBundle(bundle);
        if(withOrganizations) {
            List<String> organizationIds = lookupResult.values()
                    .stream()
                    .map(r -> ExtensionMapper.tryExtractOrganizationId(r.getExtension()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .distinct()
                    .collect(Collectors.toList());

            lookupResult = lookupResult.merge(lookupOrganizations(organizationIds));
        }
        return lookupResult;
    }

    private FhirLookupResult lookupOrganizations(List<String> organizationIds) {
        var idCriterion = Organization.RES_ID.exactly().codes(organizationIds);

        return lookupOrganizationsByCriteria(List.of(idCriterion));
    }

    private <T extends Resource> String save(Resource resource) throws ServiceException {
        addOrganizationTag(resource);

        IGenericClient client = context.newRestfulGenericClient(endpoint);

        MethodOutcome outcome = client.create().resource(resource).execute();
        if(!outcome.getCreated()) {
            throw new IllegalStateException(String.format("Tried to create resource of type %s, but it was not created!", resource.getResourceType().name()));
        }
        return outcome.getId().toUnqualifiedVersionless().getIdPart();
    }

    private Optional<String> saveInTransaction(Bundle transactionBundle, ResourceType resourceType) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);

        // Execute the transaction
        var responseBundle = client.transaction().withBundle(transactionBundle).execute();

        // Look for an entry with status 201 to retrieve the location-header.
        var id = "";
        for(var responseEntry : responseBundle.getEntry()) {
            var status = responseEntry.getResponse().getStatus();
            var location = responseEntry.getResponse().getLocation();
            if(status.startsWith("201") && location.startsWith(resourceType.toString())) {
                id = location.replaceFirst("/_history.*$", "");
            }
        }

        if(id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    private <T extends Resource> void update(Resource resource) {
        IGenericClient client = context.newRestfulGenericClient(endpoint);
        client.update().resource(resource).execute();
    }

    private void addOrganizationTag(Resource resource) throws ServiceException {
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

    private void addOrganizationTag(Bundle bundle) throws ServiceException {
        for(var entry : bundle.getEntry()) {
            addOrganizationTag(entry.getResource());
        }
    }

    private void addOrganizationTag(DomainResource extendable) throws ServiceException {
        if(excludeFromOrganizationTagging(extendable)) {
            return;
        }
        if(extendable.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.ORGANIZATION))) {
            throw new IllegalArgumentException(String.format("Trying to add organization tag to resource, but the tag was already present! - %S", extendable.getId()));
        }

        extendable.addExtension(Systems.ORGANIZATION, new Reference(getOrganizationId()));
    }

    private boolean excludeFromOrganizationTagging(DomainResource extendable) {
        return UNTAGGED_RESOURCE_TYPES.contains(extendable.getResourceType());
    }

    private ICriterion<?> buildOrganizationCriterion() throws ServiceException {
        String organizationId = getOrganizationId();
        return new ReferenceClientParam(SearchParameters.ORGANIZATION).hasId(organizationId);
    }

    public String getOrganizationId() throws ServiceException {
        Organization organization = getCurrentUsersOrganization();

        return organization.getIdElement().toUnqualifiedVersionless().getValue();

    }

    public Organization getCurrentUsersOrganization() throws ServiceException {
        var context = userContextProvider.getUserContext();
        if(context == null) {
            throw new IllegalStateException("UserContext was not initialized!");
        }
        return lookupOrganizationBySorCode(context.getOrgId()).orElseThrow(() -> new ServiceException(String.format("No Organization was present for sorCode %s!", context.getOrgId()), ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE));


    }

    public Practitioner getOrCreateUserAsPractitioner() throws ServiceException {

        String orgId = userContextProvider.getUserContext().getOrgId();
        String userId = userContextProvider.getUserContext().getUserId();

        ICriterion<TokenClientParam> sorIdentifier = Practitioner.IDENTIFIER.exactly().systemAndIdentifier(Systems.SOR, orgId);
        ICriterion<TokenClientParam> userIdIdentifier = Practitioner.IDENTIFIER.exactly().systemAndIdentifier(Systems.USER_ID, userId);
        //FhirLookupResult lookupResult = lookupByCriteria(Practitioner.class, List.of(sorIdentifier, userIdIdentifier));
        Optional<Practitioner> practitioner = lookupPractitioner(List.of(sorIdentifier, userIdIdentifier));

        if (practitioner.isPresent()) {
            return practitioner.get();
        }
        else {
            Practitioner p = new Practitioner();
            p.addIdentifier().setSystem(Systems.SOR).setValue(orgId);
            p.addIdentifier().setSystem(Systems.USER_ID).setValue(userId);
            p.getNameFirstRep().addGiven(userContextProvider.getUserContext().getFirstName());
            p.getNameFirstRep().setFamily(userContextProvider.getUserContext().getLastName());

            String practitionerId = save(p);
            return lookupPractitionerById(practitionerId).get();
        }
    }


    public Optional<Practitioner> lookupPractitionerById(String practitionerId) {
        var idCriterion = Practitioner.RES_ID.exactly().code(practitionerId);

        return lookupPractitioner(idCriterion);
    }

    private Optional<Practitioner> lookupPractitioner(ICriterion<?> criterion) {
        return lookupPractitioner(List.of(criterion));
    }

    private Optional<Practitioner> lookupPractitioner(List<ICriterion<?>> criterions) {
        var lookupResult = lookupByCriteria(Practitioner.class, criterions);

        if(lookupResult.getPractitioners().isEmpty()) {
            return Optional.empty();
        }
        if(lookupResult.getPractitioners().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", Practitioner.class));
        }
        return Optional.of(lookupResult.getPractitioners().get(0));
    }

    public FhirLookupResult lookupPractitioners(Collection<String> practitionerIds) {
        var idCriterion = Practitioner.RES_ID.exactly().codes(practitionerIds);

        return lookupByCriteria(Practitioner.class, List.of(idCriterion));
    }

    public FhirLookupResult lookupValueSet() throws ServiceException {
        var organizationCriterion = buildOrganizationCriterion();

        return lookupByCriteria(ValueSet.class, List.of(organizationCriterion));
    }

    public String saveQuestionnaire(Questionnaire questionnaire) throws ServiceException {
        return save(questionnaire);
    }

    public void updateQuestionnaire(Questionnaire questionnaire) {
        update(questionnaire);
    }

    public String savePlanDefinition(PlanDefinition planDefinition) throws ServiceException {
        return save(planDefinition);
    }

    public void updatePlanDefinition(PlanDefinition planDefinition) {
        update(planDefinition);
    }

    public void updatePatient(Patient patient) {
        update(patient);
    }
}
