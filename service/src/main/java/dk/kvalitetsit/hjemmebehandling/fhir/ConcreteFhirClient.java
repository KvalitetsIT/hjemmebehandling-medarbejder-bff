package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import dk.kvalitetsit.hjemmebehandling.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.Instant;
import java.util.*;

public class ConcreteFhirClient implements FhirClient<CarePlan, PlanDefinition, Practitioner, Patient, Questionnaire, QuestionnaireResponse, Organization, CarePlan.CarePlanStatus> {
    private static final Logger logger = LoggerFactory.getLogger(ConcreteFhirClient.class);
    private static final List<ResourceType> UNTAGGED_RESOURCE_TYPES = List.of(ResourceType.Patient);
    private final UserContextProvider userContextProvider;
    private final IGenericClient client;

    public ConcreteFhirClient(FhirContext context, String endpoint, UserContextProvider userContextProvider) {
        this.userContextProvider = userContextProvider;
        this.client = context.newRestfulGenericClient(endpoint);
    }


    public List<CarePlan> lookupActiveCarePlansWithQuestionnaire(String questionnaireId) throws ServiceException {
        var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
        var questionnaireCriterion = CarePlan.INSTANTIATES_CANONICAL.hasChainedProperty(PlanDefinition.DEFINITION.hasId(questionnaireId));
        var organizationCriterion = buildOrganizationCriterion();
        var criteria = new ArrayList<>(List.of(statusCriterion, questionnaireCriterion, organizationCriterion));
        return lookupCarePlansByCriteria(criteria).getCarePlans();
    }


    public List<PlanDefinition> lookupActivePlanDefinitionsUsingQuestionnaireWithId(String questionnaireId) throws ServiceException {
        var statusCriterion = PlanDefinition.STATUS.exactly().code(Enumerations.PublicationStatus.ACTIVE.toCode());
        var questionnaireCriterion = PlanDefinition.DEFINITION.hasId(questionnaireId);
        var organizationCriterion = buildOrganizationCriterion();
        var criteria = new ArrayList<>(List.of(statusCriterion, questionnaireCriterion, organizationCriterion));
        return lookupPlanDefinitionsByCriteria(criteria).getPlanDefinitions();
    }


    public List<CarePlan> lookupActiveCarePlansWithPlanDefinition(String plandefinitionId) throws ServiceException {
        var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
        var plandefinitionCriterion = CarePlan.INSTANTIATES_CANONICAL.hasId(plandefinitionId);
        var organizationCriterion = buildOrganizationCriterion();
        var criteria = new ArrayList<>(List.of(statusCriterion, plandefinitionCriterion, organizationCriterion));
        return lookupCarePlansByCriteria(criteria).getCarePlans();
    }


    public List<CarePlan> lookupCarePlansByPatientId(String patientId, boolean onlyActiveCarePlans) throws ServiceException {
        var patientCriterion = CarePlan.PATIENT.hasId(patientId);
        var organizationCriterion = buildOrganizationCriterion();
        var criteria = new ArrayList<>(List.of(patientCriterion, organizationCriterion));
        if (onlyActiveCarePlans) {
            var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
            criteria.add(statusCriterion);
        }

        return lookupCarePlansByCriteria(criteria).getCarePlans();
    }


    public List<CarePlan> lookupCarePlans(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        var criteria = createCriteria(unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied);
        var sortSpec = new SortSpec(SearchParameters.CAREPLAN_SATISFIED_UNTIL, SortOrderEnum.ASC);
        return lookupCarePlansByCriteria(criteria, Optional.of(sortSpec)).getCarePlans();
    }


    public Practitioner getOrCreateUserAsPractitioner() throws ServiceException {
        // TODO: Handle 'Optional.get()' without 'isPresent()' check below
        String orgId = userContextProvider.getUserContext().getOrgId().get();
        String userId = userContextProvider.getUserContext().getUserId().get();

        ICriterion<TokenClientParam> sorIdentifier = Practitioner.IDENTIFIER.exactly().systemAndIdentifier(Systems.SOR, orgId);
        ICriterion<TokenClientParam> userIdIdentifier = Practitioner.IDENTIFIER.exactly().systemAndIdentifier(Systems.USER_ID, userId);
        //FhirLookupResult lookupResult = lookupByCriteria(Practitioner.class, List.of(sorIdentifier, userIdIdentifier));
        Optional<Practitioner> practitioner = lookupPractitioner(List.of(sorIdentifier, userIdIdentifier));

        if (practitioner.isPresent()) {
            return practitioner.get();
        } else {
            Practitioner p = new Practitioner();
            p.addIdentifier().setSystem(Systems.SOR).setValue(orgId);
            p.addIdentifier().setSystem(Systems.USER_ID).setValue(userId);

            // TODO: Handle 'Optional.get()' without 'isPresent()' check below
            p.getNameFirstRep().addGiven(userContextProvider.getUserContext().getFirstName().get());
            p.getNameFirstRep().setFamily(userContextProvider.getUserContext().getLastName().get());

            String practitionerId = savePractitioner(p);
            return lookupPractitionerById(practitionerId).get();
        }
    }


    public List<Practitioner> lookupPractitioners(Collection<String> practitionerIds) {
        return getPractitioners(practitionerIds).getPractitioners();
    }

    private FhirLookupResult getPractitioners(Collection<String> practitionerIds) {
        var idCriterion = Practitioner.RES_ID.exactly().codes(practitionerIds);

        return lookupByCriteria(Practitioner.class, List.of(idCriterion));
    }


    public FhirLookupResult lookupValueSet() throws ServiceException {
        var organizationCriterion = buildOrganizationCriterion();

        return lookupByCriteria(ValueSet.class, List.of(organizationCriterion));
    }


    public void updatePatient(Patient patient) {
        this.update(patient);
    }


    public void updatePlanDefinition(PlanDefinition planDefinition) {
        this.update(planDefinition);
    }


    public void updateQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) {
        this.update(questionnaireResponse);
    }


    public List<CarePlan> lookupCarePlans(String cpr, Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {

        var criteria = createCriteria(unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied);

        Optional<Patient> patient = lookupPatientByCpr(cpr);
        if (patient.isEmpty()) {
            return List.of();
        }
        String patientId = patient.get().getIdElement().toUnqualifiedVersionless().toString();
        var patientCriterion = CarePlan.PATIENT.hasId(patientId);
        criteria.add(patientCriterion);

        var sortSpec = new SortSpec(SearchParameters.CAREPLAN_SATISFIED_UNTIL, SortOrderEnum.ASC);

        return lookupCarePlansByCriteria(criteria, Optional.of(sortSpec)).getCarePlans();
    }


    public Optional<CarePlan> lookupCarePlanById(String carePlanId) throws ServiceException {
        var idCriterion = CarePlan.RES_ID.exactly().code(carePlanId);

        var result = lookupCarePlansByCriteria(List.of(idCriterion));

        if (result.getCarePlans().isEmpty()) {
            return Optional.empty();
        }
        if (result.getPatients().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", CarePlan.class));
        }

        return Optional.of(result.getCarePlans().getFirst());

    }


    public Optional<Organization> lookupOrganizationBySorCode(String sorCode) throws ServiceException {
        if (sorCode == null || sorCode.isBlank() || sorCode.isEmpty())
            throw new ServiceException("The SOR-code was not specified", ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE);
        var sorCodeCriterion = Organization.IDENTIFIER.exactly().systemAndValues(Systems.SOR, sorCode);

        var lookupResult = lookupOrganizationsByCriteria(List.of(sorCodeCriterion));
        if (lookupResult.getOrganizations().isEmpty()) {
            return Optional.empty();
        }
        if (lookupResult.getOrganizations().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of %s!", Organization.class));
        }
        return Optional.of(lookupResult.getOrganizations().getFirst());
    }


    public Optional<Patient> lookupPatientById(String patientId) {
        var idCriterion = Patient.RES_ID.exactly().code(patientId);
        return lookupPatient(List.of(idCriterion));
    }


    public Optional<Patient> lookupPatientByCpr(String cpr) {
        var cprCriterion = Patient.IDENTIFIER.exactly().systemAndValues(Systems.CPR, cpr);
        return lookupPatient(List.of(cprCriterion));
    }


    public List<Patient> searchPatients(List<String> searchStrings, CarePlan.CarePlanStatus status) throws ServiceException {
        // FHIR has no way of expressing 'search patient with name like %search% OR cpr like %search%'
        // so we have to do that in two seperate queries
        var cprCriterion = CarePlan.PATIENT.hasChainedProperty(new StringClientParam("patient_identifier_cpr").matches().values(searchStrings));
        var nameCriterion = CarePlan.PATIENT.hasChainedProperty(Patient.NAME.matches().values(searchStrings));
        var organizationCriterion = buildOrganizationCriterion();
        var statusCriterion = CarePlan.STATUS.exactly().code(status.toCode());

        FhirLookupResult fhirLookupResult = lookupCarePlansByCriteria(List.of(cprCriterion, statusCriterion, organizationCriterion));
        fhirLookupResult.merge(lookupCarePlansByCriteria(List.of(nameCriterion, statusCriterion, organizationCriterion)));

        throw new NotImplementedException();
    }


    public List<Patient> getPatientsByStatus(CarePlan.CarePlanStatus status) throws ServiceException {
        var organizationCriterion = buildOrganizationCriterion();
        var statusCriterion = CarePlan.STATUS.exactly().code(status.toCode());
        return lookupCarePlansByCriteria(List.of(statusCriterion, organizationCriterion)).getPatients();
    }


    public Optional<QuestionnaireResponse> lookupQuestionnaireResponseById(String questionnaireResponseId) {
        var idCriterion = QuestionnaireResponse.RES_ID.exactly().code(questionnaireResponseId);
        var result =  lookupQuestionnaireResponseByCriteria(List.of(idCriterion));

        if (result.getPatients().isEmpty()) {
            return Optional.empty();
        }
        if (result.getPatients().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", QuestionnaireResponse.class));
        }

        return Optional.of(result.getQuestionnaireResponses().getFirst());

    }


    public List<QuestionnaireResponse> lookupQuestionnaireResponses(String carePlanId, List<String> questionnaireIds) {
        var questionnaireCriterion = QuestionnaireResponse.QUESTIONNAIRE.hasAnyOfIds(questionnaireIds);
        var basedOnCriterion = QuestionnaireResponse.BASED_ON.hasId(carePlanId);

        var result = lookupQuestionnaireResponseByCriteria(List.of(questionnaireCriterion, basedOnCriterion));
        return result.getQuestionnaireResponses();
    }


    public List<Questionnaire> lookupVersionsOfQuestionnaireById(List<String> ids) {

        List<Questionnaire> resources = new LinkedList<>();

        ids.forEach(id -> {
            Bundle bundle = client.history().onInstance(new IdType("Questionnaire", id)).returnBundle(Bundle.class).execute();
            bundle.getEntry().stream().filter(bec -> bec.getResource() != null).forEach(x -> resources.add((Questionnaire) x.getResource()));
        });

        return resources;
    }


    public List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatus(List<ExaminationStatus> statuses) throws ServiceException {
        var statusCriterion = new TokenClientParam(SearchParameters.EXAMINATION_STATUS).exactly().codes(statuses.stream().map(Enum::toString).toArray(String[]::new));
        var organizationCriterion = buildOrganizationCriterion();

        var result = lookupQuestionnaireResponseByCriteria(List.of(statusCriterion, organizationCriterion));
        return result.getQuestionnaireResponses();
    }


    public List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatusAndCarePlanId(List<ExaminationStatus> statuses, String carePlanId) throws ServiceException {
        var statusCriterion = new TokenClientParam(SearchParameters.EXAMINATION_STATUS).exactly().codes(statuses.stream().map(Enum::toString).toArray(String[]::new));
        var organizationCriterion = buildOrganizationCriterion();
        var basedOnCriterion = QuestionnaireResponse.BASED_ON.hasId(carePlanId);

        var result = lookupQuestionnaireResponseByCriteria(List.of(statusCriterion, organizationCriterion, basedOnCriterion));

        return result.getQuestionnaireResponses();
    }


    public List<QuestionnaireResponse> lookupQuestionnaireResponsesByStatus(ExaminationStatus status) throws ServiceException {
        return lookupQuestionnaireResponsesByStatus(List.of(status));
    }


    public String saveCarePlan(CarePlan carePlan, Patient patient) throws ServiceException {
        // Build a transaction bundle.
        var bundle = new BundleBuilder().buildCreateCarePlanBundle(carePlan, patient);
        addOrganizationTag(bundle);

        var id = saveInTransaction(bundle, ResourceType.CarePlan);

        return id.orElseThrow(() -> new IllegalStateException("Could not locate location-header in response when executing transaction."));
    }


    public String saveCarePlan(CarePlan carePlan) throws ServiceException {
        return saveResource(carePlan);
    }


    public String savePractitioner(Practitioner practitioner) throws ServiceException {
        return saveResource(practitioner);
    }


    public String saveQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) throws ServiceException {
        return saveResource(questionnaireResponse);
    }

    public String saveQuestionnaire(Questionnaire questionnaire) throws ServiceException {
        return this.saveResource(questionnaire);
    }

    public String savePlanDefinition(PlanDefinition planDefinition) throws ServiceException {
        return saveResource(planDefinition);
    }

    public String savePatient(Patient patient) throws ServiceException {
        return this.saveResource(patient);
    }

    public Optional<PlanDefinition> lookupPlanDefinition(String planDefinitionId) {
        var idCriterion = PlanDefinition.RES_ID.exactly().code(planDefinitionId);
        var lookupResult = lookupPlanDefinitionsByCriteria(List.of(idCriterion));

        if (lookupResult.getPlanDefinitions().isEmpty()) {
            return Optional.empty();
        }

        if (lookupResult.getPlanDefinitions().size() != 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", PlanDefinition.class));
        }

        return Optional.of(lookupResult.getPlanDefinitions().getFirst());
    }

    public List<PlanDefinition> lookupPlanDefinitions() throws ServiceException {
        var organizationCriterion = buildOrganizationCriterion();
        return lookupPlanDefinitionsByCriteria(List.of(organizationCriterion)).getPlanDefinitions();
    }

    public List<PlanDefinition> lookupPlanDefinitionsById(Collection<String> planDefinitionIds) {
        return getPlanDefinitionsById(planDefinitionIds).getPlanDefinitions();
    }

    private FhirLookupResult getPlanDefinitionsById(Collection<String> planDefinitionIds) {
        var idCriterion = PlanDefinition.RES_ID.exactly().codes(planDefinitionIds);
        return lookupPlanDefinitionsByCriteria(List.of(idCriterion));
    }


    public List<PlanDefinition> lookupPlanDefinitionsByStatus(Collection<String> statusesToInclude) throws ServiceException {
        var organizationCriterion = buildOrganizationCriterion();
        var criterias = new ArrayList<ICriterion<?>>();
        criterias.add(organizationCriterion);

        if (!statusesToInclude.isEmpty()) {
            Collection<String> statusesToIncludeToLowered = statusesToInclude.stream().map(String::toLowerCase).toList(); //status should be to lowered
            var statusCriteron = PlanDefinition.STATUS.exactly().codes(statusesToIncludeToLowered);
            criterias.add(statusCriteron);
        }
        return lookupPlanDefinitionsByCriteria(criterias).getPlanDefinitions();
    }

    public List<Questionnaire> lookupQuestionnairesByStatus(Collection<String> statusesToInclude) throws ServiceException {
        var criterias = new ArrayList<ICriterion<?>>();
        var organizationCriterion = buildOrganizationCriterion();
        criterias.add(organizationCriterion);

        if (!statusesToInclude.isEmpty()) {
            Collection<String> statusesToIncludeToLowered = statusesToInclude.stream().map(String::toLowerCase).toList(); //status should be to lowered
            var statusCriteron = Questionnaire.STATUS.exactly().codes(statusesToIncludeToLowered);
            criterias.add(statusCriteron);
        }
        return lookupByCriteria(Questionnaire.class, criterias).getQuestionnaires();
    }

    public List<Questionnaire> lookupQuestionnairesById(Collection<String> questionnaireIds) throws ServiceException {
        return getQuestionnairesById(questionnaireIds).getQuestionnaires();
    }

    private FhirLookupResult getQuestionnairesById(Collection<String> questionnaireIds) throws ServiceException {
        var idCriterion = Questionnaire.RES_ID.exactly().codes(questionnaireIds);
        var organizationCriterion = buildOrganizationCriterion();

        return lookupByCriteria(Questionnaire.class, List.of(idCriterion, organizationCriterion));
    }

    public void updateCarePlan(CarePlan carePlan, Patient patient) {
        var bundle = new BundleBuilder().buildUpdateCarePlanBundle(carePlan, patient);
        saveInTransaction(bundle, ResourceType.CarePlan);
    }


    public void updateCarePlan(CarePlan carePlan) {
        this.update(carePlan);
    }


    public void updateQuestionnaire(Questionnaire questionnaire) {
        this.update(questionnaire);
    }


    public String getOrganizationId() throws ServiceException {
        Organization organization = getCurrentUsersOrganization();
        return organization.getIdElement().toUnqualifiedVersionless().getValue();
    }


    public Organization getCurrentUsersOrganization() throws ServiceException {
        var context = userContextProvider.getUserContext();
        if (context == null) {
            throw new IllegalStateException("UserContext was not initialized!");
        }

        var orgId = context.getOrgId().orElseThrow(() -> new ServiceException(String.format("No Organization was present for sorCode %s!", context.getOrgId()), ErrorKind.BAD_REQUEST, ErrorDetails.MISSING_SOR_CODE));
        return lookupOrganizationBySorCode(orgId).orElseThrow();
    }


    public Optional<Practitioner> lookupPractitionerById(String practitionerId) {
        var idCriterion = Practitioner.RES_ID.exactly().code(practitionerId);

        return lookupPractitioner(idCriterion);
    }

    @Override
    public Optional<Questionnaire> lookupQuestionnaireById(String qualifiedId) throws ServiceException {
        return Optional.ofNullable(getQuestionnairesById(List.of(qualifiedId)).getQuestionnaires().getFirst());

    }

    private FhirLookupResult lookupCarePlansByCriteria(List<ICriterion<?>> criteria) throws ServiceException {
        return lookupCarePlansByCriteria(criteria, Optional.empty());
    }

    private FhirLookupResult lookupCarePlansByCriteria(List<ICriterion<?>> criteria, Optional<SortSpec> sortSpec) throws ServiceException {
        boolean withOrganizations = true;
        var carePlanResult = lookupByCriteria(CarePlan.class, criteria, List.of(CarePlan.INCLUDE_SUBJECT, CarePlan.INCLUDE_INSTANTIATES_CANONICAL), withOrganizations, sortSpec, Optional.empty(), Optional.empty());

        // The FhirLookupResult includes the patient- and plandefinition-resources that we need,
        // but due to limitations of the FHIR server, not the questionnaire-resources. Se wo look up those in a separate call.
        if (carePlanResult.getCarePlans().isEmpty()) {
            return carePlanResult;
        }

        // Get the related questionnaire-resources
        List<String> questionnaireIds = new ArrayList<>();
        questionnaireIds.addAll(getQuestionnaireIdsFromCarePlan(carePlanResult.getCarePlans()));
        questionnaireIds.addAll(getQuestionnaireIdsFromPlanDefinition(carePlanResult.getPlanDefinitions()));
        FhirLookupResult questionnaireResult = getQuestionnairesById(questionnaireIds);

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

        if (lookupResult.getPatients().isEmpty()) {
            return Optional.empty();
        }
        if (lookupResult.getPatients().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", Patient.class));
        }
        return Optional.of(lookupResult.getPatients().getFirst());
    }

    private FhirLookupResult lookupPlanDefinitionsByCriteria(List<ICriterion<?>> criteria) {
        // Includes the Questionnaire resources.
        return lookupByCriteria(PlanDefinition.class, criteria, List.of(PlanDefinition.INCLUDE_DEFINITION));
    }

    private FhirLookupResult lookupQuestionnaireResponseByCriteria(List<ICriterion<?>> criteria) {
        var questionnaireResponseResult = lookupByCriteria(QuestionnaireResponse.class, criteria, List.of(QuestionnaireResponse.INCLUDE_BASED_ON, QuestionnaireResponse.INCLUDE_QUESTIONNAIRE, QuestionnaireResponse.INCLUDE_SUBJECT));

        // We also need the planDefinitions, which are found by following the chain QuestionnaireResponse.based-on -> CarePlan.instantiates-canonical.
        // This requires a separate lookup.
        if (questionnaireResponseResult.getQuestionnaireResponses().isEmpty()) {
            return questionnaireResponseResult;
        }

        // Get the related planDefinitions
        List<String> planDefinitionIds = getPlanDefinitionIds(questionnaireResponseResult.getCarePlans());
        FhirLookupResult planDefinitionResult = getPlanDefinitionsById(planDefinitionIds);

        // Merge the results
        questionnaireResponseResult.merge(planDefinitionResult);

        // We also need to lookup the practitioner who (potentially) changed the examination status
        List<String> practitionerIds = getPractitionerIds(questionnaireResponseResult.getQuestionnaireResponses());
        if (!practitionerIds.isEmpty()) {
            FhirLookupResult practitionerResult = getPractitioners(practitionerIds);
            questionnaireResponseResult.merge(practitionerResult);
        }
        return questionnaireResponseResult;
    }


    private <T extends Resource> FhirLookupResult lookupByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria) {
        return lookupByCriteria(resourceClass, criteria, null);
    }

    private <T extends Resource> FhirLookupResult lookupByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria, List<Include> includes) {
        boolean withOrganizations = true;
        return lookupByCriteria(resourceClass, criteria, includes, withOrganizations, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private <T extends Resource> FhirLookupResult lookupByCriteria(Class<T> resourceClass, List<ICriterion<?>> criteria, List<Include> includes, boolean withOrganizations, Optional<SortSpec> sortSpec, Optional<Integer> offset, Optional<Integer> count) {

        var query = client
                .search()
                .forResource(resourceClass);

        if (criteria != null && !criteria.isEmpty()) {
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
            List<String> organizationIds = lookupResult.values()
                    .stream()
                    .map(r -> ExtensionMapper.tryExtractOrganizationId(r.getExtension()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .distinct()
                    .toList();

            lookupResult = lookupResult.merge(lookupOrganizations(organizationIds));
        }
        return lookupResult;
    }

    private FhirLookupResult lookupOrganizations(List<String> organizationIds) {
        var idCriterion = Organization.RES_ID.exactly().codes(organizationIds);

        return lookupOrganizationsByCriteria(List.of(idCriterion));
    }


    private Optional<String> saveInTransaction(Bundle transactionBundle, ResourceType resourceType) {

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

    private void addOrganizationTag(Bundle bundle) throws ServiceException {
        for (var entry : bundle.getEntry()) {
            addOrganizationTag(entry.getResource());
        }
    }

    private void addOrganizationTag(DomainResource extendable) throws ServiceException {
        if (excludeFromOrganizationTagging(extendable)) {
            return;
        }
        if (extendable.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.ORGANIZATION))) {
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


    private Optional<Practitioner> lookupPractitioner(ICriterion<?> criterion) {
        return lookupPractitioner(List.of(criterion));
    }

    private Optional<Practitioner> lookupPractitioner(List<ICriterion<?>> criterions) {
        var lookupResult = lookupByCriteria(Practitioner.class, criterions);

        if (lookupResult.getPractitioners().isEmpty()) {
            return Optional.empty();
        }
        if (lookupResult.getPractitioners().size() > 1) {
            throw new IllegalStateException(String.format("Could not lookup single resource of class %s!", Practitioner.class));
        }
        return Optional.of(lookupResult.getPractitioners().getFirst());
    }


    private <T extends Resource> String saveResource(Resource resource) throws ServiceException {
        addOrganizationTag(resource);

        MethodOutcome outcome = client
                .create()
                .resource(resource)
                .execute();

        if (!outcome.getCreated()) {
            throw new IllegalStateException(String.format("Tried to create resource of type %s, but it was not created!", resource.getResourceType().name()));
        }
        return outcome.getId().toUnqualifiedVersionless().getIdPart();
    }

    @NotNull
    private ArrayList<ICriterion<?>> createCriteria(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        var organizationCriterion = buildOrganizationCriterion();
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

    private <T extends Resource> void update(Resource resource) {
        client.update().resource(resource).execute();
    }


    private static List<String> getQuestionnaireIdsFromCarePlan(List<CarePlan> carePlans) {
        return carePlans
                .stream()
                .flatMap(cp -> cp.getActivity().stream().map(a -> getQuestionnaireId(a.getDetail())))
                .toList();
    }

    private static List<String> getQuestionnaireIdsFromPlanDefinition(List<PlanDefinition> planDefinitions) {
        return planDefinitions
                .stream()
                .flatMap(pd -> pd.getAction().stream().map(a -> a.getDefinitionCanonicalType().getValue()))
                .toList();
    }

    private static List<String> getPlanDefinitionIds(List<CarePlan> carePlans) {
        return carePlans
                .stream()
                .flatMap(cp -> cp.getInstantiatesCanonical().stream().map(PrimitiveType::getValue))
                .toList();
    }

    private static List<String> getPractitionerIds(List<QuestionnaireResponse> questionnaireResponses) {
        return questionnaireResponses
                .stream()
                .map(qr -> ExtensionMapper.tryExtractExaminationAuthorPractitionerId(qr.getExtension()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static String getQuestionnaireId(CarePlan.CarePlanActivityDetailComponent detail) {
        if (detail.getInstantiatesCanonical() == null || detail.getInstantiatesCanonical().size() != 1) {
            throw new IllegalStateException("Expected InstantiatesCanonical to be present, and to contain exactly one value!");
        }
        return detail.getInstantiatesCanonical().getFirst().getValue();
    }

}
