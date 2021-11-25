package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.fhir.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.checkerframework.checker.units.qual.C;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CarePlanServiceTest {
    @InjectMocks
    private CarePlanService subject;

    @Mock
    private FhirClient fhirClient;

    @Mock
    private FhirMapper fhirMapper;

    @Mock
    private DateProvider dateProvider;

    @Mock
    private AccessValidator accessValidator;

    private static final String CPR_1 = "0101010101";

    private static final String CAREPLAN_ID_1 = "careplan-1";
    private static final String PATIENT_ID_1 = "patient-1";
    private static final String PLANDEFINITION__ID_1 = "plandefinition-1";
    private static final String QUESTIONNAIRE_ID_1 = "questionnaire-1";

    private static final Instant POINT_IN_TIME = Instant.parse("2021-11-23T00:00:00.000Z");

    @Test
    public void createCarePlan_patientExists_patientIsNotCreated() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1);

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Patient patient = new Patient();
        patient.setId(PATIENT_ID_1);
        Mockito.when(fhirClient.lookupPatientByCpr_new(CPR_1)).thenReturn(Optional.of(patient));

        Mockito.when(fhirClient.lookupCarePlansByPatientId_new(PATIENT_ID_1)).thenReturn(FhirLookupResult.fromResources());

        // Act
        String result = subject.createCarePlan(carePlanModel);

        // Assert
        Mockito.verify(fhirClient).saveCarePlan(carePlan);
    }

    @Test
    public void createCarePlan_patientDoesNotExist_patientIsCreated() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1);

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Patient patient = new Patient();
        Mockito.when(fhirMapper.mapPatientModel(carePlanModel.getPatient())).thenReturn(patient);

        Mockito.when(fhirClient.lookupPatientByCpr_new(CPR_1)).thenReturn(Optional.empty());

        // Act
        String result = subject.createCarePlan(carePlanModel);

        // Assert
        Mockito.verify(fhirClient).saveCarePlan(carePlan, patient);
    }

    @Test
    public void createCarePlan_activePlanExists_throwsException() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1);

        Patient patient = new Patient();
        patient.setId(PATIENT_ID_1);
        Mockito.when(fhirClient.lookupPatientByCpr_new(CPR_1)).thenReturn(Optional.of(patient));

        CarePlan existingCareplan = new CarePlan();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(existingCareplan);
        Mockito.when(fhirClient.lookupCarePlansByPatientId_new(PATIENT_ID_1)).thenReturn(lookupResult);

        CarePlanModel existingCareplanModel = new CarePlanModel();
        existingCareplanModel.setStartDate(Instant.parse("2021-11-09T00:00:00.000Z"));
        Mockito.when(fhirMapper.mapCarePlan(existingCareplan, lookupResult)).thenReturn(existingCareplanModel);

        // Act

        // Assert
        assertThrows(IllegalStateException.class, () -> subject.createCarePlan(carePlanModel));
    }

    @Test
    public void createCarePlan_questionnaireAccessViolation_throwsException() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1, List.of(QUESTIONNAIRE_ID_1), List.of());

        Mockito.when(fhirClient.lookupPatientByCpr_new(CPR_1)).thenReturn(Optional.empty());

        Questionnaire questionnaire = new Questionnaire();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnaires_new(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(questionnaire));

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.createCarePlan(carePlanModel));
    }

    @Test
    public void createCarePlan_planDefinitionAccessViolation_throwsException() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1, List.of(), List.of(PLANDEFINITION__ID_1));

        Mockito.when(fhirClient.lookupPatientByCpr_new(CPR_1)).thenReturn(Optional.empty());

        PlanDefinition planDefinition = new PlanDefinition();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitions_new(List.of(PLANDEFINITION__ID_1))).thenReturn(lookupResult);

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(planDefinition));

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.createCarePlan(carePlanModel));
    }

    @Test
    public void createCarePlan_inactivePlanExists_succeeds() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1);

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Patient patient = new Patient();
        patient.setId(PATIENT_ID_1);
        Mockito.when(fhirClient.lookupPatientByCpr_new(CPR_1)).thenReturn(Optional.of(patient));

        CarePlan existingCareplan = new CarePlan();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(existingCareplan);
        Mockito.when(fhirClient.lookupCarePlansByPatientId_new(PATIENT_ID_1)).thenReturn(lookupResult);

        CarePlanModel existingCareplanModel = new CarePlanModel();
        existingCareplanModel.setStartDate(Instant.parse("2021-11-09T00:00:00.000Z"));
        existingCareplanModel.setEndDate(Instant.parse("2021-11-10T00:00:00.000Z"));
        Mockito.when(fhirMapper.mapCarePlan(existingCareplan, lookupResult)).thenReturn(existingCareplanModel);

        Mockito.when(fhirClient.saveCarePlan(Mockito.any())).thenReturn("1");

        // Act
        String result = subject.createCarePlan(carePlanModel);

        // Assert
        assertEquals("1", result);
    }

    @Test
    public void createCarePlan_persistingFails_throwsException() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1);

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Patient patient = new Patient();
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        Mockito.when(fhirClient.saveCarePlan(carePlan)).thenThrow(IllegalStateException.class);

        // Act

        // Assert
        assertThrows(ServiceException.class, () -> subject.createCarePlan(carePlanModel));
    }

    @Test
    public void createCarePlan_populatesSatisfiedUntil() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1, List.of(QUESTIONNAIRE_ID_1), List.of(PLANDEFINITION__ID_1));

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Mockito.when(fhirClient.lookupPatientByCpr_new(CPR_1)).thenReturn(Optional.empty());

        Mockito.when(fhirClient.lookupQuestionnaires_new(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResource(buildQuestionnaire(QUESTIONNAIRE_ID_1)));
        Mockito.when(fhirClient.lookupPlanDefinitions_new(List.of(PLANDEFINITION__ID_1))).thenReturn(FhirLookupResult.fromResource(buildPlanDefinition(PLANDEFINITION__ID_1)));

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        // Act
        String result = subject.createCarePlan(carePlanModel);

        // Assert
        var wrapper = carePlanModel.getQuestionnaires().get(0);
        var expectedPointInTime = new FrequencyEnumerator(dateProvider.now(), wrapper.getFrequency()).next().next().getPointInTime();
        assertEquals(expectedPointInTime, wrapper.getSatisfiedUntil());
        assertEquals(expectedPointInTime, carePlanModel.getSatisfiedUntil());
    }

    @Test
    public void getCarePlanByCpr_carePlansPresent_returnsCarePlans() throws Exception {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

        Mockito.when(fhirClient.lookupPatientByCpr_new(CPR_1)).thenReturn(Optional.of(patient));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient);
        Mockito.when(fhirClient.lookupCarePlansByPatientId_new(PATIENT_ID_1)).thenReturn(lookupResult);
        Mockito.when(fhirClient.lookupQuestionnaires_new(List.of())).thenReturn(FhirLookupResult.fromResources());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(fhirMapper.mapCarePlan(carePlan, lookupResult)).thenReturn(carePlanModel);

        // Act
        List<CarePlanModel> result = subject.getCarePlansByCpr(CPR_1);

        // Assert
        assertEquals(1, result.size());
        assertEquals(carePlanModel, result.get(0));
    }

    @Test
    public void getCarePlanByCpr_carePlansMissing_returnsEmptyList() throws Exception {
        // Arrange
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

        Mockito.when(fhirClient.lookupPatientByCpr_new(CPR_1)).thenReturn(Optional.of(patient));
        Mockito.when(fhirClient.lookupCarePlansByPatientId_new(PATIENT_ID_1)).thenReturn(FhirLookupResult.fromResources());

        // Act
        List<CarePlanModel> result = subject.getCarePlansByCpr(CPR_1);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void getCarePlanByCpr_carePlansPresent_computesExceededQuestionnaires() throws Exception {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

        Mockito.when(fhirClient.lookupPatientByCpr_new(CPR_1)).thenReturn(Optional.of(patient));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient);
        Mockito.when(fhirClient.lookupCarePlansByPatientId_new(PATIENT_ID_1)).thenReturn(lookupResult);

        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        Mockito.when(fhirClient.lookupQuestionnaires_new(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResources(questionnaire));

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME.plusSeconds(4));

        CarePlanModel carePlanModel = new CarePlanModel();

        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        questionnaireModel.setId(QUESTIONNAIRE_ID_1);
        carePlanModel.setQuestionnaires(List.of(new QuestionnaireWrapperModel(questionnaireModel, new FrequencyModel(), POINT_IN_TIME)));
        Mockito.when(fhirMapper.mapCarePlan(carePlan, lookupResult)).thenReturn(carePlanModel);

        // Act
        List<CarePlanModel> result = subject.getCarePlansByCpr(CPR_1);

        // Assert
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getQuestionnairesWithUnsatisfiedSchedule().size());
        assertEquals(QUESTIONNAIRE_ID_1, result.get(0).getQuestionnairesWithUnsatisfiedSchedule().get(0));
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_carePlansPresent_returnsCarePlans() throws Exception {
        // Arrange
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(carePlan);
        Mockito.when(fhirClient.lookupCarePlansUnsatisfiedAt_new(POINT_IN_TIME)).thenReturn(lookupResult);

        Mockito.when(fhirClient.lookupQuestionnaires_new(List.of())).thenReturn(FhirLookupResult.fromResources());

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(fhirMapper.mapCarePlan(carePlan, lookupResult)).thenReturn(carePlanModel);

        // Act
        List<CarePlanModel> result = subject.getCarePlansWithUnsatisfiedSchedules();

        // Assert
        assertEquals(1, result.size());
        assertEquals(carePlanModel, result.get(0));
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_carePlansMissing_returnsEmptyList() throws Exception {
        // Arrange
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        Mockito.when(fhirClient.lookupCarePlansUnsatisfiedAt_new(POINT_IN_TIME)).thenReturn(FhirLookupResult.fromResources());

        // Act
        List<CarePlanModel> result = subject.getCarePlansWithUnsatisfiedSchedules();

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void getCarePlanById_carePlanPresent_returnsCarePlan() throws Exception {
        // Arrange
        String carePlanId = "CarePlan/careplan-1";
        String patientId = "Patient/patient-1";

        CarePlan carePlan = buildCarePlan(carePlanId, patientId);
        CarePlanModel carePlanModel = setupCarePlan(carePlan);

        // Act
        Optional<CarePlanModel> result = subject.getCarePlanById(carePlanId);

        // Assert
        assertEquals(carePlanModel, result.get());
    }

    @Test
    public void getCarePlanById_carePlanForDifferentOrganization_throwsException() throws Exception {
        // Arrange
        String carePlanId = "CarePlan/careplan-1";
        String patientId = "Patient/patient-1";

        CarePlan carePlan = buildCarePlan(carePlanId, patientId);
        Mockito.when(fhirClient.lookupCarePlanById_new(carePlan.getId())).thenReturn(FhirLookupResult.fromResource(carePlan));
        Mockito.when(fhirClient.lookupQuestionnaires_new(List.of())).thenReturn(FhirLookupResult.fromResources());

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.getCarePlanById(carePlanId));
    }

    @Test
    public void getCarePlanById_carePlanMissing_returnsEmpty() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        Mockito.when(fhirClient.lookupCarePlanById_new(carePlanId)).thenReturn(FhirLookupResult.fromResources());

        // Act
        Optional<CarePlanModel> result = subject.getCarePlanById(carePlanId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void updateQuestionnaires_questionnaireAccessViolation_throwsException() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        Map<String, FrequencyModel> frequencies = Map.of();

        Questionnaire questionnaire = new Questionnaire();
        Mockito.when(fhirClient.lookupQuestionnaires_new(questionnaireIds)).thenReturn(FhirLookupResult.fromResource(questionnaire));

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(questionnaire));

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.updateQuestionnaires(carePlanId, questionnaireIds, frequencies));
    }

    @Test
    public void updateQuestionnaires_carePlanAccessViolation_throwsException() throws Exception {
        // Arrange
        String carePlanId = "CarePlan/careplan-1";
        List<String> questionnaireIds = List.of();
        Map<String, FrequencyModel> frequencies = Map.of();

        Mockito.when(fhirClient.lookupQuestionnaires_new(questionnaireIds)).thenReturn(FhirLookupResult.fromResources());

        CarePlan carePlan = new CarePlan();
        carePlan.setId(carePlanId);
        Mockito.when(fhirClient.lookupCarePlanById_new(carePlanId)).thenReturn(FhirLookupResult.fromResource(carePlan));
        Mockito.when(fhirClient.lookupQuestionnaires_new(List.of())).thenReturn(FhirLookupResult.fromResources());

        Mockito.doNothing().when(accessValidator).validateAccess(List.of());
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.updateQuestionnaires(carePlanId, questionnaireIds, frequencies));
    }

    private CarePlanModel buildCarePlanModel(String cpr) {
        return buildCarePlanModel(cpr, null, null);
    }

    private CarePlanModel buildCarePlanModel(String cpr, List<String> questionnaireIds, List<String> planDefinitionIds) {
        CarePlanModel carePlanModel = new CarePlanModel();

        carePlanModel.setPatient(new PatientModel());
        carePlanModel.getPatient().setCpr(CPR_1);
        carePlanModel.setQuestionnaires(List.of());
        if(questionnaireIds != null) {
            carePlanModel.setQuestionnaires(questionnaireIds.stream().map(id -> buildQuestionnaireWrapperModel(id)).collect(Collectors.toList()));
        }
        carePlanModel.setPlanDefinitions(List.of());
        if(planDefinitionIds != null) {
            carePlanModel.setPlanDefinitions(planDefinitionIds.stream().map(id -> buildPlanDefinitionModel(id)).collect(Collectors.toList()));
        }

        return carePlanModel;
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel(String questionnaireId) {
        var model = new QuestionnaireWrapperModel();

        model.setQuestionnaire(new QuestionnaireModel());
        model.getQuestionnaire().setId(questionnaireId);

        FrequencyModel frequencyModel = new FrequencyModel();
        frequencyModel.setWeekdays(List.of(Weekday.TUE));
        frequencyModel.setTimeOfDay(LocalTime.parse("04:00"));
        model.setFrequency(frequencyModel);

        return model;
    }

    private PlanDefinitionModel buildPlanDefinitionModel(String planDefinitionId) {
        var model = new PlanDefinitionModel();

        model.setId(planDefinitionId);

        return model;
    }

    private void setupLookupResultForPatient(String carePlanId, String patientId) {
        CarePlan carePlan = buildCarePlan(carePlanId, patientId, null);



        Mockito.when(fhirClient.lookupCarePlansByPatientId(patientId)).thenReturn(List.of(carePlan));

        CarePlanModel carePlanModel = new CarePlanModel();
        carePlanModel.setId(carePlanId);
        Mockito.when(fhirMapper.mapCarePlan(carePlan)).thenReturn(carePlanModel);


        setupCarePlanForPatient(carePlanId, patientId, null);
    }

    private void setupCarePlanForPatient(String carePlanId, String patientId) {
        setupCarePlanForPatient(carePlanId, patientId, null);
    }

    private void setupCarePlanForPatient(String carePlanId, String patientId, String questionnaireId) {
        CarePlan carePlan = buildCarePlan(carePlanId, patientId, questionnaireId);
        Mockito.when(fhirClient.lookupCarePlansByPatientId(patientId)).thenReturn(List.of(carePlan));

        CarePlanModel carePlanModel = new CarePlanModel();
        carePlanModel.setId(carePlanId);
        Mockito.when(fhirMapper.mapCarePlan(carePlan)).thenReturn(carePlanModel);
    }

    private CarePlanModel setupCarePlan(CarePlan carePlan) {
        Mockito.when(fhirClient.lookupCarePlanById_new(carePlan.getId())).thenReturn(FhirLookupResult.fromResource(carePlan));
        Mockito.when(fhirClient.lookupQuestionnaires_new(List.of())).thenReturn(FhirLookupResult.fromResources());

        CarePlanModel carePlanModel = new CarePlanModel();
        carePlanModel.setId(carePlan.getId());
        Mockito.when(fhirMapper.mapCarePlan(Mockito.any(CarePlan.class), Mockito.any(FhirLookupResult.class))).thenReturn(carePlanModel);

        return carePlanModel;
    }

    private void setupPatientForCpr(String cpr, String patientId) {
        Patient patient = new Patient();
        patient.setId(patientId);
        Mockito.when(fhirClient.lookupPatientByCpr(cpr)).thenReturn(Optional.of(patient));

        PatientModel patientModel = new PatientModel();
        patientModel.setCpr(cpr);
        Mockito.when(fhirMapper.mapPatient(patient)).thenReturn(patientModel);
    }

    private PatientModel setupPatient(String patientId) {
        Patient patient = new Patient();
        Mockito.when(fhirClient.lookupPatientById(patientId)).thenReturn(Optional.of(patient));

        PatientModel patientModel = new PatientModel();
        Mockito.when(fhirMapper.mapPatient(patient)).thenReturn(patientModel);

        return patientModel;
    }

    private QuestionnaireModel setupQuestionnaire(String questionnaireId) {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setIdElement(new IdType(questionnaireId));
        Mockito.when(fhirClient.lookupQuestionnaires(List.of(questionnaireId))).thenReturn(List.of(questionnaire));

        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        questionnaireModel.setId(questionnaireId);
        Mockito.when(fhirMapper.mapQuestionnaire(questionnaire)).thenReturn(questionnaireModel);

        FrequencyModel frequencyModel = new FrequencyModel();
        Mockito.when(fhirMapper.mapTiming(Mockito.any())).thenReturn(frequencyModel);

        return questionnaireModel;
    }

    private CarePlan buildCarePlan(String carePlanId, String patientId) {
        return buildCarePlan(carePlanId, patientId, null);
    }

    private CarePlan buildCarePlan(String carePlanId, String patientId, String questionnaireId) {
        CarePlan carePlan = new CarePlan();

        carePlan.setId(carePlanId);
        carePlan.setSubject(new Reference(patientId));

        if(questionnaireId != null) {
            CarePlan.CarePlanActivityDetailComponent detail = new CarePlan.CarePlanActivityDetailComponent();
            detail.setInstantiatesCanonical(List.of(new CanonicalType(questionnaireId)));
            detail.setScheduled(new Timing());
            detail.addExtension(ExtensionMapper.mapActivitySatisfiedUntil(POINT_IN_TIME));
            carePlan.addActivity().setDetail(detail);
        }

        return carePlan;
    }

    private Patient buildPatient(String patientId, String cpr) {
        Patient patient = new Patient();

        patient.setId(patientId);

        var identifier = new Identifier();
        identifier.setSystem(Systems.CPR);
        identifier.setValue(cpr);
        patient.setIdentifier(List.of(identifier));

        return patient;
    }

    private PlanDefinition buildPlanDefinition(String planDefinitionId) {
        PlanDefinition planDefinition = new PlanDefinition();

        planDefinition.setId(planDefinitionId);

        return planDefinition;
    }

    private Questionnaire buildQuestionnaire(String questionnaireId) {
        Questionnaire questionnaire = new Questionnaire();

        questionnaire.setId(questionnaireId);

        return questionnaire;
    }
}