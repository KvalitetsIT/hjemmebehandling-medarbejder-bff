package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.UserContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class FhirClientTest {
    private static final String ORGANIZATION_ID_1 = "Organization/organization-1";
    private static final String ORGANIZATION_ID_2 = "Organization/organization-2";
    private static final String PLANDEFINITION_ID_1 = "PlanDefinition/plandefinition-1";
    private static final String QUESTIONNAIRE_RESPONSE_ID_1 = "QuestionnaireResponse/questionnaireresponse-1";
    private static final String QUESTIONNAIRE_RESPONSE_ID_2 = "QuestionnaireResponse/questionnaireresponse-2";
    private static final String SOR_CODE_1 = "123456";
    private static final String SOR_CODE_2 = "654321";
    private final String endpoint = "http://foo";
    private FhirClient subject;
    @Mock
    private FhirContext context;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IGenericClient client;
    @Mock
    private UserContextProvider userContextProvider;

    @BeforeEach
    public void setup() {
        Mockito.when(context.newRestfulGenericClient(endpoint)).thenReturn(client);
        subject = new FhirClient(context, endpoint, userContextProvider);

    }

    @Test
    public void lookupCarePlanById_carePlanPresent_success() throws ServiceException {
        String carePlanId = "careplan-1";
        CarePlan carePlan = new CarePlan();
        carePlan.setId(carePlanId);
        setupSearchCarePlanByIdClient(carePlan);
        setupUserContext(SOR_CODE_1);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        Optional<CarePlan> result = subject.lookupCarePlanById(carePlanId);
        assertTrue(result.isPresent());
        assertEquals(carePlan, result.get());
    }

    @Test
    public void lookupCarePlanById_carePlanMissing_empty() throws ServiceException {
        String carePlanId = "careplan-1";
        CarePlan carePlan = new CarePlan();
        setupSearchCarePlanByIdClient(carePlan);
        setupUserContext(SOR_CODE_1);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        Optional<CarePlan> result = subject.lookupCarePlanById(carePlanId);
        assertFalse(result.isPresent());
    }

    @Test
    public void lookupCarePlanById_resultIncludesOrganization() throws ServiceException {
        String carePlanId = "careplan-1";
        CarePlan carePlan = new CarePlan();
        carePlan.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1));
        carePlan.setId(carePlanId);
        setupSearchCarePlanByIdClient(carePlan);
        setupUserContext(SOR_CODE_1);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        Optional<CarePlan> result = subject.lookupCarePlanById(carePlanId);
        assertTrue(result.isPresent());
    }

    @Test
    public void lookupCarePlanByPatientId_carePlanPresent_success() throws ServiceException {
        String patientId = "patient-1";
        boolean onlyActiveCarePlans = true;
        CarePlan carePlan = new CarePlan();
        setupSearchCarePlanClient(onlyActiveCarePlans, carePlan);
        setupUserContext(SOR_CODE_1);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        List<CarePlan> result = subject.lookupCarePlansByPatientId(patientId, onlyActiveCarePlans);
        assertEquals(1, result.size());
        assertEquals(carePlan, result.getFirst());
    }

    @Test
    public void lookupCarePlanByPatientId_carePlanMissing_empty() throws ServiceException {
        String patientId = "patient-1";
        boolean onlyActiveCarePlans = false;
        setupSearchCarePlanClient();
        setupUserContext(SOR_CODE_1);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        List<CarePlan> result = subject.lookupCarePlansByPatientId(patientId, onlyActiveCarePlans);
        assertEquals(0, result.size());
    }

    @Test
    public void lookupCarePlansUnsatisfiedAt_success() throws ServiceException {
        Instant pointInTime = Instant.parse("2021-11-07T10:11:12.124Z");
        boolean onlyActiveCarePlans = true;
        boolean useUnsatisfied = true;
        CarePlan carePlan = new CarePlan();
        setupSearchCarePlanClient(true, false, false, onlyActiveCarePlans, carePlan);
        setupUserContext(SOR_CODE_1);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        List<CarePlan> result = subject.lookupCarePlans(pointInTime, onlyActiveCarePlans, useUnsatisfied);
        assertEquals(1, result.size());
        assertEquals(carePlan, result.getFirst());
    }

    @Test
    public void lookupCarePlansUnsatisfiedAt_noCarePlans_returnsEmpty() throws ServiceException {
        Instant pointInTime = Instant.parse("2021-11-07T10:11:12.124Z");
        boolean onlyActiveCarePlans = true;
        boolean useUnsatisfied = true;
        setupSearchCarePlanClient(true, false, false, onlyActiveCarePlans);
        setupUserContext(SOR_CODE_1);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        List<CarePlan> result = subject.lookupCarePlans(pointInTime, onlyActiveCarePlans, useUnsatisfied);
        assertEquals(0, result.size());
    }

    @Test
    public void lookupPatientById_patientPresent_success() {
        String id = "patient-1";
        Patient patient = new Patient();
        patient.setId(id);
        setupSearchPatientClient(patient);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        Optional<Patient> result = subject.lookupPatientById(id);
        assertTrue(result.isPresent());
        assertEquals(patient, result.get());
    }

    @Test
    public void lookupPatientById_patientMissing_empty() {
        String id = "patient-1";
        setupSearchPatientClient();
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        Optional<Patient> result = subject.lookupPatientById(id);
        assertFalse(result.isPresent());
    }

    @Test
    public void lookupPlanDefinitionById_planDefinitionPresent_success() {
        String plandefinitionId = "plandefinition-1";
        PlanDefinition planDefinition = new PlanDefinition();
        planDefinition.setId(plandefinitionId);
        setupSearchPlanDefinitionClient(planDefinition);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);

        Optional<PlanDefinition> result = subject.lookupPlanDefinition(plandefinitionId);

        assertTrue(result.isPresent());
        assertEquals(planDefinition, result.get());
    }

    @Test
    public void lookupPlanDefinitionById_planDefinitionMissing_empty() {
        String plandefinitionId = "plandefinition-1";
        setupSearchPlanDefinitionClient();
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        Optional<PlanDefinition> result = subject.lookupPlanDefinition(plandefinitionId);
        assertFalse(result.isPresent());
    }

    @Test
    public void lookupPlanDefinitionById_resultIncludesOrganization() {
        String plandefinitionId = "plandefinition-1";
        PlanDefinition planDefinition = new PlanDefinition();
        planDefinition.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1));
        planDefinition.setId(plandefinitionId);
        setupSearchPlanDefinitionClient(planDefinition);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        Optional<PlanDefinition> result = subject.lookupPlanDefinition(plandefinitionId);
        assertTrue(result.isPresent());
    }

    @Test
    public void lookupPlanDefinitions_success() throws ServiceException {
        PlanDefinition planDefinition = new PlanDefinition();
        setupUserContext(SOR_CODE_1);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        setupSearchPlanDefinitionClient(planDefinition);
        List<PlanDefinition> result = subject.lookupPlanDefinitions();
        assertEquals(1, result.size());
        assertEquals(planDefinition, result.getFirst());
    }

    @Test
    public void lookupQuestionnaireResponses_carePlanAndQuestionnairesPresent_success() {
        String carePlanId = "careplan-1";
        String questionnaireId = "questionnaire-1";

        QuestionnaireResponse questionnaireResponse1 = new QuestionnaireResponse();
        questionnaireResponse1.setId(QUESTIONNAIRE_RESPONSE_ID_1);
        QuestionnaireResponse questionnaireResponse2 = new QuestionnaireResponse();
        questionnaireResponse2.setId(QUESTIONNAIRE_RESPONSE_ID_2);
        setupSearchQuestionnaireResponseClient(2, questionnaireResponse1, questionnaireResponse2);

        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);

        List<QuestionnaireResponse> result = subject.lookupQuestionnaireResponses(carePlanId, List.of(questionnaireId));

        assertEquals(2, result.size());
        assertTrue(result.contains(questionnaireResponse1));
        assertTrue(result.contains(questionnaireResponse2));
    }

    @Test
    public void lookupQuestionnaireResponses_includesPlanDefinition() {
        String carePlanId = "careplan-1";
        String questionnaireId = "questionnaire-1";
        QuestionnaireResponse questionnaireResponse1 = new QuestionnaireResponse();
        questionnaireResponse1.setId(QUESTIONNAIRE_RESPONSE_ID_1);
        QuestionnaireResponse questionnaireResponse2 = new QuestionnaireResponse();
        questionnaireResponse2.setId(QUESTIONNAIRE_RESPONSE_ID_2);
        setupSearchQuestionnaireResponseClient(2, questionnaireResponse1, questionnaireResponse2);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        List<QuestionnaireResponse> result = subject.lookupQuestionnaireResponses(carePlanId, List.of(questionnaireId));
        assertEquals(2, result.size());
        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(x -> x.getId().equals(PLANDEFINITION_ID_1)));
    }

    @Test
    public void lookupQuestionnaireResponsesByStatus_oneStatus_success() throws ServiceException {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);
        setupUserContext(SOR_CODE_1);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        questionnaireResponse.setId(QUESTIONNAIRE_RESPONSE_ID_1);
        setupSearchQuestionnaireResponseClient(2, questionnaireResponse);
        List<QuestionnaireResponse> result = subject.lookupQuestionnaireResponsesByStatus(statuses);
        assertEquals(1, result.size());
        assertTrue(result.contains(questionnaireResponse));
    }

    @Test
    public void lookupQuestionnaireResponsesByStatus_twoStatuses_success() throws ServiceException {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED, ExaminationStatus.UNDER_EXAMINATION);
        setupUserContext(SOR_CODE_1);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        questionnaireResponse.setId(QUESTIONNAIRE_RESPONSE_ID_1);
        setupSearchQuestionnaireResponseClient(2, questionnaireResponse);
        List<QuestionnaireResponse> result = subject.lookupQuestionnaireResponsesByStatus(statuses);
        assertEquals(1, result.size());
        assertTrue(result.contains(questionnaireResponse));
    }

    @Test
    public void lookupQuestionnaireResponsesByStatus_duplicateStatuses_success() throws ServiceException {
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED, ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.EXAMINED, ExaminationStatus.EXAMINED);
        setupUserContext(SOR_CODE_1);
        setupOrganization(SOR_CODE_1, ORGANIZATION_ID_1);
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        questionnaireResponse.setId(QUESTIONNAIRE_RESPONSE_ID_1);
        setupSearchQuestionnaireResponseClient(2, questionnaireResponse);
        List<QuestionnaireResponse> result = subject.lookupQuestionnaireResponsesByStatus(statuses);
        assertEquals(1, result.size());
        assertTrue(result.contains(questionnaireResponse));
    }

    @Test
    public void saveCarePlan_created_returnsId() throws ServiceException {
        CarePlan carePlan = new CarePlan();
        carePlan.setId("1");
        setupSaveClient(carePlan, true);
        String result = subject.saveCarePlan(carePlan);
        assertEquals("1",result);
    }

    @Test
    public void saveCarePlan_addsOrganizationTag() throws ServiceException {
        CarePlan carePlan = new CarePlan();
        carePlan.setId("1");
        setupSaveClient(carePlan, true);
        subject.saveCarePlan(carePlan);
        assertTrue(isTaggedWithId(carePlan, ORGANIZATION_ID_1));
    }

    @Test
    public void saveCarePlan_notCreated_throwsException() {
        CarePlan carePlan = new CarePlan();
        carePlan.setId("1");
        setupSaveClient(carePlan, false);
        assertThrows(IllegalStateException.class, () -> subject.saveCarePlan(carePlan));
    }

    @Test
    public void saveCarePlanWithPatient_returnsCarePlanId() throws ServiceException {
        CarePlan carePlan = new CarePlan();
        Patient patient = new Patient();
        Bundle responseBundle = buildResponseBundle("201", "CarePlan/2", "201", "Patient/3");
        setupTransactionClient(responseBundle);
        String result = subject.saveCarePlan(carePlan, patient);
        assertEquals("CarePlan/2", result);
    }

    @Test
    public void saveCarePlanWithPatient_carePlanLocationMissing_throwsException() {
        CarePlan carePlan = new CarePlan();
        Patient patient = new Patient();
        Bundle responseBundle = buildResponseBundle("201", "Questionnaire/4", "201", "Patient/3");
        setupTransactionClient(responseBundle);
        assertThrows(IllegalStateException.class, () -> subject.saveCarePlan(carePlan, patient));
    }

    @Test
    public void saveCarePlanWithPatient_unwantedHttpStatus_throwsException() {
        CarePlan carePlan = new CarePlan();
        Patient patient = new Patient();
        Bundle responseBundle = buildResponseBundle("400", null, "400", null);
        setupTransactionClient(responseBundle);
        assertThrows(IllegalStateException.class, () -> subject.saveCarePlan(carePlan, patient));
    }

    @Test
    public void saveCarePlanWithPatient_addsOrganizationTag() throws ServiceException {
        CarePlan carePlan = new CarePlan();
        Patient patient = new Patient();
        Bundle responseBundle = buildResponseBundle("201", "CarePlan/2", "201", "Patient/3");
        setupTransactionClient(responseBundle, SOR_CODE_2, ORGANIZATION_ID_2);
        subject.saveCarePlan(carePlan, patient);
        assertTrue(isTaggedWithId(carePlan, ORGANIZATION_ID_2));
        assertFalse(isTagged(patient));
    }

    @Test
    public void savePatient_organizationTagIsOmitted() throws ServiceException {
        Patient patient = new Patient();
        setupSaveClient(patient, true, null, null);
        subject.savePatient(patient);
        assertFalse(isTagged(patient));
    }

    @Test
    public void saveQuestionnaireResponse_created_returnsId() throws ServiceException {
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        questionnaireResponse.setId("1");
        setupSaveClient(questionnaireResponse, true);
        String result = subject.saveQuestionnaireResponse(questionnaireResponse);
        assertEquals("1", result);
    }

    @Test
    public void saveQuestionnaireResponse_notCreated_throwsException() {
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        setupSaveClient(questionnaireResponse, false);
        assertThrows(IllegalStateException.class, () -> subject.saveQuestionnaireResponse(questionnaireResponse));
    }


    private void setupReadCarePlanClient(String carePlanId, CarePlan carePlan) {
        setupReadClient(carePlanId, carePlan, CarePlan.class);
    }

    private void setupReadPatientClient(String patientId, Patient patient) {
        setupReadClient(patientId, patient, Patient.class);
    }

    private void setupReadPlanDefinitionClient(String planDefinitionId, PlanDefinition planDefinition) {
        setupReadClient(planDefinitionId, planDefinition, PlanDefinition.class);
    }

    private void setupReadClient(String id, Resource resource, Class<? extends Resource> resourceClass) {
        Mockito.when(client
                        .read()
                        .resource(resourceClass)
                        .withId(Mockito.anyString())
                        .execute())
                .then((a) -> {
                    if (resource == null) {
                        throw new ResourceNotFoundException("error");
                    }
                    return resource;
                });
    }

    private void setupSearchCarePlanByIdClient(CarePlan carePlan) {
        setupSearchCarePlanClient(1, false, false, false, false, carePlan);
    }

    private void setupSearchCarePlanClient(CarePlan... carePlans) {
        setupSearchCarePlanClient(2, false, false, false, false, carePlans);
    }

    private void setupSearchCarePlanClient(boolean onlyActiveCarePlans, CarePlan... carePlans) {
        setupSearchCarePlanClient(2, false, false, false, onlyActiveCarePlans, carePlans);
    }

    private void setupSearchCarePlanClient(boolean withSort, boolean withOffset, boolean withCount, boolean onlyActiveCarePlans, CarePlan... carePlans) {
        setupSearchCarePlanClient(2, withSort, withOffset, withCount, onlyActiveCarePlans, carePlans);
    }

    private void setupSearchCarePlanClient_with3criterions(boolean withSort, boolean withOffset, boolean withCount, boolean onlyActiveCarePlans, CarePlan... carePlans) {
        setupSearchCarePlanClient(3, withSort, withOffset, withCount, onlyActiveCarePlans, carePlans);
    }

    private void setupSearchCarePlanClient(int criteriaCount, boolean withSort, boolean withOffset, boolean withCount, boolean onlyActiveCarePlans, CarePlan... carePlans) {
        //This creates a mock which will return the careplans you say, as long as the correct amount of criterias are specified.
        //It does not matter what the criterias are, we only look at the amount of them
        //TODO: For each test we should verify that it is in fact, the correct criterias, insted of only counting them

        if (onlyActiveCarePlans) {
            criteriaCount++;
        }
        setupSearchClient(criteriaCount, 2, withSort, withOffset, withCount, CarePlan.class, carePlans);

        if (carePlans.length > 0) {
            setupSearchQuestionnaireClient();
        }
    }

    private void setupSearchOrganizationClient(Organization... organizations) {
        setupSearchClient(Organization.class, organizations);
    }

    private void setupSearchPatientClient(Patient... patients) {
        setupSearchClient(Patient.class, patients);
    }

    private void setupSearchQuestionnaireClient(Questionnaire... questionnaires) {
        setupSearchClient(2, 0, Questionnaire.class, questionnaires);
    }

    private void setupSearchPlanDefinitionClient(PlanDefinition... planDefinitions) {
        setupSearchClient(1, 1, PlanDefinition.class, planDefinitions);
    }

    private void setupSearchQuestionnaireResponseClient(int criteriaCount, QuestionnaireResponse... questionnaireResponses) {
        setupSearchClient(criteriaCount, 3, QuestionnaireResponse.class, questionnaireResponses);

        if (questionnaireResponses.length > 0) {
            PlanDefinition planDefinition = new PlanDefinition();
            planDefinition.setId(PLANDEFINITION_ID_1);
            setupSearchPlanDefinitionClient(planDefinition);
        }
    }

    private void setupSearchClient(Class<? extends Resource> resourceClass, Resource... resources) {
        setupSearchClient(1, 0, resourceClass, resources);
    }


    private void setupSearchClient(int criteriaCount, int includeCount, Class<? extends Resource> resourceClass, Resource... resources) {
        setupSearchClient(criteriaCount, includeCount, false, false, false, resourceClass, resources);
    }

    private void setupSearchClient(int criteriaCount, int includeCount, boolean withSort, boolean withOffset, boolean withCount, Class<? extends Resource> resourceClass, Resource... resources) {
        Bundle bundle = new Bundle();

        for (Resource resource : resources) {
            Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
            component.setResource(resource);
            bundle.addEntry(component);
        }
        bundle.setTotal(resources.length);

        var query = client.search().forResource(resourceClass);
        if (criteriaCount > 0) {
            query = query.where(Mockito.any(ICriterion.class));
        }
        for (var i = 1; i < criteriaCount; i++) {
            query = query.and(Mockito.any(ICriterion.class));
        }
        for (var i = 0; i < includeCount; i++) {
            query = query.include(Mockito.any(Include.class));
        }
        if (withSort) {
            query = query.sort(Mockito.any(SortSpec.class));
        }
        if (withOffset) {
            query = query.offset(Mockito.anyInt());
        }
        if (withCount) {
            query = query.count(Mockito.anyInt());
        }

        Mockito.when(query
                        .execute())
                .thenReturn(bundle);
    }

    private void setupSaveClient(Resource resource, boolean shouldSucceed) {
        setupSaveClient(resource, shouldSucceed, SOR_CODE_1, ORGANIZATION_ID_1);
    }

    private void setupSaveClient(Resource resource, boolean shouldSucceed, String sorCode, String organizationId) {
        if (sorCode != null && organizationId != null) {
            setupUserContext(sorCode);
            setupOrganization(sorCode, organizationId);
        }

        MethodOutcome outcome = new MethodOutcome();
        if (shouldSucceed) {
            outcome.setCreated(true);
            outcome.setId(new IdType(resource.getResourceType().name(), resource.getId()));
            Mockito.when(client.create().resource(resource).execute()).thenReturn(outcome);
        } else {
            outcome.setCreated(false);
        }
    }

    private Bundle buildResponseBundle(String carePlanStatus, String careplanLocation, String patientStatus, String patientLocaton) {
        Bundle responseBundle = new Bundle();

        var carePlanEntry = responseBundle.addEntry();
        carePlanEntry.setResponse(new Bundle.BundleEntryResponseComponent());
        carePlanEntry.getResponse().setStatus(carePlanStatus);
        carePlanEntry.getResponse().setLocation(careplanLocation);

        var patientEntry = responseBundle.addEntry();
        patientEntry.setResponse(new Bundle.BundleEntryResponseComponent());
        patientEntry.getResponse().setStatus(patientStatus);
        patientEntry.getResponse().setLocation(patientLocaton);

        return responseBundle;
    }

    private void setupTransactionClient(Bundle responseBundle) {
        setupTransactionClient(responseBundle, SOR_CODE_1, ORGANIZATION_ID_1);
    }

    private void setupTransactionClient(Bundle responseBundle, String sorCode, String organizationId) {
        setupUserContext(sorCode);
        setupOrganization(sorCode, organizationId);
        Mockito.when(client.transaction().withBundle(Mockito.any(Bundle.class)).execute()).thenReturn(responseBundle);
    }

    private void setupUserContext(String sorCode) {
        Mockito.when(userContextProvider.getUserContext()).thenReturn(new UserContext().orgId(sorCode));
    }

    private void setupOrganization(String sorCode, String organizationId) {
        var organization = new Organization();
        organization.setId(organizationId);
        organization.addIdentifier().setSystem(Systems.SOR).setValue(sorCode);
        setupSearchOrganizationClient(organization);
    }

    private boolean isTagged(DomainResource resource) {
        return resource.getExtension().stream().anyMatch(e -> isOrganizationTag(e));
    }

    private boolean isTaggedWithId(DomainResource resource, String organizationId) {
        return resource.getExtension().stream().anyMatch(e -> isOrganizationTag(e) && isTagForOrganization(e, organizationId));
    }

    private boolean isOrganizationTag(Extension e) {
        return e.getUrl().equals(Systems.ORGANIZATION);
    }

    private boolean isTagForOrganization(Extension e, String organizationId) {
        return e.getValue() instanceof Reference && ((Reference) e.getValue()).getReference().equals(organizationId);
    }
}