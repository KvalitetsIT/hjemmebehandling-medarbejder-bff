package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.fhir.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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

    private static final String CAREPLAN_ID_1 = "CarePlan/careplan-1";
    private static final String PATIENT_ID_1 = "Patient/patient-1";
    private static final String PLANDEFINITION__ID_1 = "PlanDefinition/plandefinition-1";
    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";
    private static final String QUESTIONNAIRE_ID_2 = "Questionnaire/questionnaire-2";

    private static final Instant POINT_IN_TIME = Instant.parse("2021-11-23T00:00:00.000Z");

    @Test
    public void createCarePlan_patientExists_patientIsNotCreated() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1);

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Patient patient = new Patient();
        patient.setId(PATIENT_ID_1);
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1)).thenReturn(FhirLookupResult.fromResources());

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

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.empty());

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
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        CarePlan existingCareplan = new CarePlan();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(existingCareplan);
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1)).thenReturn(lookupResult);

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

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.empty());

        Questionnaire questionnaire = new Questionnaire();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(questionnaire));

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.createCarePlan(carePlanModel));
    }

    @Test
    public void createCarePlan_planDefinitionAccessViolation_throwsException() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1, List.of(), List.of(PLANDEFINITION__ID_1));

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.empty());

        PlanDefinition planDefinition = new PlanDefinition();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitions(List.of(PLANDEFINITION__ID_1))).thenReturn(lookupResult);

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
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        CarePlan existingCareplan = new CarePlan();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(existingCareplan);
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1)).thenReturn(lookupResult);

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
        patient.setId(PATIENT_ID_1);
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1)).thenReturn(FhirLookupResult.fromResources());

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

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.empty());

        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResource(buildQuestionnaire(QUESTIONNAIRE_ID_1)));
        Mockito.when(fhirClient.lookupPlanDefinitions(List.of(PLANDEFINITION__ID_1))).thenReturn(FhirLookupResult.fromResource(buildPlanDefinition(PLANDEFINITION__ID_1)));

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

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient);
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1)).thenReturn(lookupResult);

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

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1)).thenReturn(FhirLookupResult.fromResources());

        // Act
        List<CarePlanModel> result = subject.getCarePlansByCpr(CPR_1);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_carePlansPresent_returnsCarePlans() throws Exception {
        // Arrange
        int pageNumber = 1;
        int pageSize = 4;
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(carePlan);
        Mockito.when(fhirClient.lookupCarePlansUnsatisfiedAt(POINT_IN_TIME, 0, 4)).thenReturn(lookupResult);

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(fhirMapper.mapCarePlan(carePlan, lookupResult)).thenReturn(carePlanModel);

        // Act
        List<CarePlanModel> result = subject.getCarePlansWithUnsatisfiedSchedules(new PageDetails(pageNumber, pageSize));

        // Assert
        assertEquals(1, result.size());
        assertEquals(carePlanModel, result.get(0));
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_carePlansMissing_returnsEmptyList() throws Exception {
        // Arrange
        int pageNumber = 1;
        int pageSize = 4;
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        Mockito.when(fhirClient.lookupCarePlansUnsatisfiedAt(POINT_IN_TIME, 0, 4)).thenReturn(FhirLookupResult.fromResources());

        // Act
        List<CarePlanModel> result = subject.getCarePlansWithUnsatisfiedSchedules(new PageDetails(pageNumber, pageSize));

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_translatesPagingParameters() throws Exception {
        // Arrange
        int pageNumber = 3;
        int pageSize = 4;
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        Mockito.when(fhirClient.lookupCarePlansUnsatisfiedAt(POINT_IN_TIME, 8, 4)).thenReturn(FhirLookupResult.fromResources());

        // Act
        List<CarePlanModel> result = subject.getCarePlansWithUnsatisfiedSchedules(new PageDetails(pageNumber, pageSize));

        // Assert
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
        Mockito.when(fhirClient.lookupCarePlanById(carePlan.getId())).thenReturn(FhirLookupResult.fromResource(carePlan));

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.getCarePlanById(carePlanId));
    }

    @Test
    public void getCarePlanById_carePlanMissing_returnsEmpty() throws Exception {
        // Arrange
        String carePlanId = CAREPLAN_ID_1;

        Mockito.when(fhirClient.lookupCarePlanById(carePlanId)).thenReturn(FhirLookupResult.fromResources());

        // Act
        Optional<CarePlanModel> result = subject.getCarePlanById(carePlanId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void resolveAlarm_carePlanMissing_throwsException() {
        // Arrange
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(FhirLookupResult.fromResources());

        // Act

        // Assert
        assertThrows(ServiceException.class, () -> subject.resolveAlarm(CAREPLAN_ID_1));
    }

    @Test
    public void resolveAlarm_accessViolation_throwsException() throws Exception {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(FhirLookupResult.fromResources(carePlan));

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.resolveAlarm(CAREPLAN_ID_1));
    }

    @Test
    public void resolveAlarm_carePlanSatisfiedIntoTheFuture_throwsException() throws Exception {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(lookupResult);

        CarePlanModel carePlanModel = new CarePlanModel();
        carePlanModel.setSatisfiedUntil(POINT_IN_TIME.plusSeconds(200));
        Mockito.when(fhirMapper.mapCarePlan(carePlan, lookupResult)).thenReturn(carePlanModel);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        // Act

        // Assert
        assertThrows(ServiceException.class, () -> subject.resolveAlarm(CAREPLAN_ID_1));
    }

    @Test
    public void resolveAlarm_recomputesSatisfiedUntil_savesCarePlan() throws Exception {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(lookupResult);

        CarePlanModel carePlanModel = new CarePlanModel();
        carePlanModel.setSatisfiedUntil(POINT_IN_TIME.minusSeconds(100));
        carePlanModel.setQuestionnaires(List.of(
                buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1, POINT_IN_TIME.minusSeconds(100)),
                buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2, POINT_IN_TIME.plusSeconds(100))
        ));
        Mockito.when(fhirMapper.mapCarePlan(carePlan, lookupResult)).thenReturn(carePlanModel);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        // Act
        subject.resolveAlarm(CAREPLAN_ID_1);

        // Assert
        // Verify that the first questionnaire has its satisfied-timestamp pushed a week into the future,
        // the second questionnaire has its timestamp left untouched, and the careplan has its timestamp set to
        // the earliest timestamp (now that of the second questionnaire).
        assertEquals(Instant.parse("2021-11-23T04:00:00.000Z"), carePlanModel.getQuestionnaires().get(0).getSatisfiedUntil());
        assertEquals(POINT_IN_TIME.plusSeconds(100), carePlanModel.getQuestionnaires().get(1).getSatisfiedUntil());
        assertEquals(POINT_IN_TIME.plusSeconds(100), carePlanModel.getSatisfiedUntil());

        Mockito.verify(fhirClient).updateCarePlan(carePlan);
    }

    @Test
    public void updateQuestionnaires_questionnaireAccessViolation_throwsException() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        Map<String, FrequencyModel> frequencies = Map.of();

        Questionnaire questionnaire = new Questionnaire();
        Mockito.when(fhirClient.lookupQuestionnaires(questionnaireIds)).thenReturn(FhirLookupResult.fromResource(questionnaire));

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

        Mockito.when(fhirClient.lookupQuestionnaires(questionnaireIds)).thenReturn(FhirLookupResult.fromResources());

        CarePlan carePlan = new CarePlan();
        carePlan.setId(carePlanId);
        Mockito.when(fhirClient.lookupCarePlanById(carePlanId)).thenReturn(FhirLookupResult.fromResource(carePlan));
        Mockito.when(fhirClient.lookupQuestionnaires(List.of())).thenReturn(FhirLookupResult.fromResources());

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
        return buildQuestionnaireWrapperModel(questionnaireId, POINT_IN_TIME);
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel(String questionnaireId, Instant satisfiedUntil) {
        var model = new QuestionnaireWrapperModel();

        model.setQuestionnaire(new QuestionnaireModel());
        model.getQuestionnaire().setId(new QualifiedId(questionnaireId));

        FrequencyModel frequencyModel = new FrequencyModel();
        frequencyModel.setWeekdays(List.of(Weekday.TUE));
        frequencyModel.setTimeOfDay(LocalTime.parse("04:00"));
        model.setFrequency(frequencyModel);
        model.setSatisfiedUntil(satisfiedUntil);

        return model;
    }

    private PlanDefinitionModel buildPlanDefinitionModel(String planDefinitionId) {
        var model = new PlanDefinitionModel();

        model.setId(new QualifiedId(planDefinitionId));

        return model;
    }

    private CarePlanModel setupCarePlan(CarePlan carePlan) {
        Mockito.when(fhirClient.lookupCarePlanById(carePlan.getId())).thenReturn(FhirLookupResult.fromResource(carePlan));

        CarePlanModel carePlanModel = new CarePlanModel();
        carePlanModel.setId(new QualifiedId(carePlan.getId()));
        Mockito.when(fhirMapper.mapCarePlan(Mockito.any(CarePlan.class), Mockito.any(FhirLookupResult.class))).thenReturn(carePlanModel);

        return carePlanModel;
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