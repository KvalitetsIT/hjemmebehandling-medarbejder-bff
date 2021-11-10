package dk.kvalitetsit.hjemmebehandling.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class FhirClientTest {
    private FhirClient subject;

    @Mock
    private FhirContext context;

    private String endpoint = "http://foo";

    @BeforeEach
    public void setup() {
        subject = new FhirClient(context, endpoint);
    }

    @Test
    public void lookupCarePlanById_carePlanPresent_success() {
        // Arrange
        String carePlanId = "careplan-1";
        CarePlan carePlan = new CarePlan();
        setupReadCarePlanClient(carePlanId, carePlan);

        // Act
        Optional<CarePlan> result = subject.lookupCarePlanById(carePlanId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(carePlan, result.get());
    }

    @Test
    public void lookupCarePlanById_carePlanMissing_empty() {
        // Arrange
        String carePlanId = "careplan-1";
        setupReadCarePlanClient(carePlanId, null);

        // Act
        Optional<CarePlan> result = subject.lookupCarePlanById(carePlanId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void lookupCarePlanByPatientId_carePlanPresent_success() {
        // Arrange
        String patientId = "patient-1";
        CarePlan carePlan = new CarePlan();
        setupSearchCarePlanClient(carePlan);

        // Act
        List<CarePlan> result = subject.lookupCarePlansByPatientId(patientId);

        // Assert
        assertEquals(1, result.size());
        assertEquals(carePlan, result.get(0));
    }

    @Test
    public void lookupCarePlanByPatientId_carePlanMissing_empty() {
        // Arrange
        String patientId = "patient-1";
        setupSearchCarePlanClient();

        // Act
        List<CarePlan> result = subject.lookupCarePlansByPatientId(patientId);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void lookupPatientByCpr_patientPresent_success() {
        // Arrange
        String cpr = "0101010101";
        Patient patient = new Patient();
        setupSearchPatientClient(patient);

        // Act
        Optional<Patient> result = subject.lookupPatientByCpr(cpr);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(patient, result.get());
    }

    @Test
    public void lookupPatientByCpr_patientMissing_empty() {
        // Arrange
        String cpr = "0101010101";
        setupSearchPatientClient();

        // Act
        Optional<Patient> result = subject.lookupPatientByCpr(cpr);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void lookupPatientById_patientPresent_success() {
        // Arrange
        String id = "patient-1";
        Patient patient = new Patient();
        setupReadPatientClient(id, patient);

        // Act
        Optional<Patient> result = subject.lookupPatientById(id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(patient, result.get());
    }

    @Test
    public void lookupPatientById_patientMissing_empty() {
        // Arrange
        String id = "patient-1";
        setupReadPatientClient(id, null);

        // Act
        Optional<Patient> result = subject.lookupPatientById(id);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void lookupPlanDefinitionById_planDefinitionPresent_success() {
        // Arrange
        String plandefinitionId = "plandefinition-1";
        PlanDefinition planDefinition = new PlanDefinition();
        setupReadPlanDefinitionClient(plandefinitionId, planDefinition);

        // Act
        Optional<PlanDefinition> result = subject.lookupPlanDefinition(plandefinitionId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(planDefinition, result.get());
    }

    @Test
    public void lookupPlanDefinitionById_planDefinitionMissing_empty() {
        // Arrange
        String plandefinitionId = "plandefinition-1";
        setupReadPlanDefinitionClient(plandefinitionId, null);

        // Act
        Optional<PlanDefinition> result = subject.lookupPlanDefinition(plandefinitionId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void lookupPlanDefinitions_success() {
        // Arrange
        PlanDefinition planDefinition = new PlanDefinition();
        setupSearchPlanDefinitionClient(planDefinition);

        // Act
        List<PlanDefinition> result = subject.lookupPlanDefinitions();

        // Assert
        assertEquals(1, result.size());
        assertEquals(planDefinition, result.get(0));
    }

    @Test
    public void lookupQuestionnaireResponses_patientAndQuestionnairesPresent_success() {
        // Arrange
        String cpr = "0101010101";
        String questionnaireId = "questionnaire-1";

        QuestionnaireResponse questionnaireResponse1 = new QuestionnaireResponse();
        QuestionnaireResponse questionnaireResponse2 = new QuestionnaireResponse();
        setupSearchQuestionnaireResponseClient(2, questionnaireResponse1, questionnaireResponse2);

        // Act
        List<QuestionnaireResponse> result = subject.lookupQuestionnaireResponses(cpr, List.of(questionnaireId));

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(questionnaireResponse1));
        assertTrue(result.contains(questionnaireResponse2));
    }

    @Test
    public void lookupQuestionnaireResponsesByStatus_oneStatus_success() {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED);

        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        setupSearchQuestionnaireResponseClient(1, questionnaireResponse);

        // Act
        List<QuestionnaireResponse> result = subject.lookupQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(questionnaireResponse));
    }

    @Test
    public void lookupQuestionnaireResponsesByStatus_twoStatuses_success() {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED, ExaminationStatus.UNDER_EXAMINATION);

        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        setupSearchQuestionnaireResponseClient(1, questionnaireResponse);

        // Act
        List<QuestionnaireResponse> result = subject.lookupQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(questionnaireResponse));
    }

    @Test
    public void lookupQuestionnaireResponsesByStatus_duplicateStatuses_success() {
        // Arrange
        List<ExaminationStatus> statuses = List.of(ExaminationStatus.NOT_EXAMINED, ExaminationStatus.UNDER_EXAMINATION, ExaminationStatus.EXAMINED, ExaminationStatus.EXAMINED);

        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        setupSearchQuestionnaireResponseClient(1, questionnaireResponse);

        // Act
        List<QuestionnaireResponse> result = subject.lookupQuestionnaireResponsesByStatus(statuses);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(questionnaireResponse));
    }

    @Test
    public void saveCarePlan_created_returnsId() {
        //Arrange
        CarePlan carePlan = new CarePlan();
        carePlan.setId("1");

        setupSaveClient(carePlan, true);

        // Act
        String result = subject.saveCarePlan(carePlan);

        // Assert
        assertEquals("1", result);
    }

    @Test
    public void saveCarePlan_notCreated_throwsException() {
        //Arrange
        CarePlan carePlan = new CarePlan();
        carePlan.setId("1");

        setupSaveClient(carePlan, false);

        // Act

        // Assert
        assertThrows(IllegalStateException.class, () -> subject.saveCarePlan(carePlan));
    }

    @Test
    public void saveQuestionnaireResponse_created_returnsId() {
        //Arrange
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        questionnaireResponse.setId("1");

        setupSaveClient(questionnaireResponse, true);

        // Act
        String result = subject.saveQuestionnaireResponse(questionnaireResponse);

        // Assert
        assertEquals("1", result);
    }

    @Test
    public void saveQuestionnaireResponse_notCreated_throwsException() {
        //Arrange
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();

        setupSaveClient(questionnaireResponse, false);

        // Act

        // Assert
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
        IGenericClient client = Mockito.mock(IGenericClient.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(client
            .read()
            .resource(resourceClass)
            .withId(Mockito.anyString())
            .execute())
            .then((a) -> {
                if(resource == null) {
                    throw new ResourceNotFoundException("error");
                }
                return resource;
            });

        Mockito.when(context.newRestfulGenericClient(endpoint)).thenReturn(client);
    }

    private void setupSearchCarePlanClient(CarePlan... carePlans) {
        setupSearchClient(CarePlan.class, carePlans);
    }

    private void setupSearchPatientClient(Patient... patients) {
        setupSearchClient(Patient.class, patients);
    }

    private void setupSearchPlanDefinitionClient(PlanDefinition... planDefinitions) {
        setupSearchClient(0, PlanDefinition.class, planDefinitions);
    }

    private void setupSearchQuestionnaireResponseClient(int criteriaCount, QuestionnaireResponse... questionnaireResponses) {
        setupSearchClient(criteriaCount, QuestionnaireResponse.class, questionnaireResponses);
    }

    private void setupSearchClient(Class<? extends Resource> resourceClass, Resource... resources) {
        setupSearchClient(1, resourceClass, resources);
    }

    private void setupSearchClient(int criteriaCount, Class<? extends Resource> resourceClass, Resource... resources) {
        IGenericClient client = Mockito.mock(IGenericClient.class, Mockito.RETURNS_DEEP_STUBS);

        Bundle bundle = new Bundle();

        for(Resource resource : resources) {
            Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
            component.setResource(resource);
            bundle.addEntry(component);
        }
        bundle.setTotal(resources.length);

        var query = client.search().forResource(resourceClass);
        if(criteriaCount > 0) {
            query = query.where(Mockito.any(ICriterion.class));
        }
        for(var i = 1; i < criteriaCount; i++) {
            query = query.and(Mockito.any(ICriterion.class));
        }
        Mockito.when(query
                .execute())
                .thenReturn(bundle);

        Mockito.when(context.newRestfulGenericClient(endpoint)).thenReturn(client);
    }

    private void setupSaveClient(Resource resource, boolean shouldSucceed) {
        IGenericClient client = Mockito.mock(IGenericClient.class, Mockito.RETURNS_DEEP_STUBS);

        MethodOutcome outcome = new MethodOutcome();
        if(shouldSucceed) {
            outcome.setCreated(true);
            outcome.setId(new IdType(resource.getResourceType().name(), resource.getId()));
            Mockito.when(client.create().resource(resource).execute()).thenReturn(outcome);
        }
        else {
            outcome.setCreated(false);
        }

        Mockito.when(context.newRestfulGenericClient(endpoint)).thenReturn(client);
    }
}