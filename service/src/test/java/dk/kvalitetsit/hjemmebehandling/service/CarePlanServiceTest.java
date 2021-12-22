package dk.kvalitetsit.hjemmebehandling.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Timing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.fhir.ExtensionMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.ContactDetailsModel;
import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientDetails;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireWrapperModel;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;

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
    
    @Mock
    private CustomUserService customUserService;

    private static final String CPR_1 = "0101010101";

    private static final String CAREPLAN_ID_1 = "CarePlan/careplan-1";
    private static final String PATIENT_ID_1 = "Patient/patient-1";
    private static final String PLANDEFINITION_ID_1 = "PlanDefinition/plandefinition-1";
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

        boolean onlyActiveCarePlans = true;
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(FhirLookupResult.fromResources());

        Mockito.when(fhirClient.lookupPlanDefinitions(List.of())).thenReturn(FhirLookupResult.fromResources());

        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

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

        Mockito.when(fhirClient.lookupPlanDefinitions(List.of())).thenReturn(FhirLookupResult.fromResources());

        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

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
        boolean onlyActiveCarePlans = true;
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(lookupResult);

        // Act

        // Assert
        assertThrows(ServiceException.class, () -> subject.createCarePlan(carePlanModel));
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
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1, List.of(), List.of(PLANDEFINITION_ID_1));

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.empty());

        PlanDefinition planDefinition = new PlanDefinition();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitions(List.of(PLANDEFINITION_ID_1))).thenReturn(lookupResult);

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(planDefinition));

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.createCarePlan(carePlanModel));
    }

    @Test
    public void createCarePlan_success() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1);

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Patient patient = new Patient();
        patient.setId(PATIENT_ID_1);
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources();
        boolean onlyActiveCarePlans = true;
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(lookupResult);

        Mockito.when(fhirClient.lookupPlanDefinitions(List.of())).thenReturn(FhirLookupResult.fromResources());

        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

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

        boolean onlyActiveCarePlans = true;
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(FhirLookupResult.fromResources());

        Mockito.when(fhirClient.lookupPlanDefinitions(List.of())).thenReturn(FhirLookupResult.fromResources());

        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

        Mockito.when(fhirClient.saveCarePlan(carePlan)).thenThrow(IllegalStateException.class);

        // Act

        // Assert
        assertThrows(ServiceException.class, () -> subject.createCarePlan(carePlanModel));
    }

    @Test
    public void createCarePlan_populatesId() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1);
        carePlanModel.setId(new QualifiedId(CAREPLAN_ID_1));

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Patient patient = new Patient();
        patient.setId(PATIENT_ID_1);
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources();
        boolean onlyActiveCarePlans = true;
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(lookupResult);

        Mockito.when(fhirClient.lookupPlanDefinitions(List.of())).thenReturn(FhirLookupResult.fromResources());

        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

        Mockito.when(fhirClient.saveCarePlan(Mockito.any())).thenReturn("1");

        // Act
        String result = subject.createCarePlan(carePlanModel);

        // Assert
        assertNull(carePlanModel.getId());
    }

    @Test
    public void createCarePlan_populatesSatisfiedUntil() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1, List.of(QUESTIONNAIRE_ID_1), List.of(PLANDEFINITION_ID_1));

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.empty());

        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResource(buildQuestionnaire(QUESTIONNAIRE_ID_1)));

        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitions(List.of(PLANDEFINITION_ID_1))).thenReturn(lookupResult);

        var questionnaireThreshold = new ThresholdModel();
        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel(QUESTIONNAIRE_ID_1, questionnaireThreshold);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

        // Act
        String result = subject.createCarePlan(carePlanModel);

        // Assert
        var wrapper = carePlanModel.getQuestionnaires().get(0);
        var expectedPointInTime = new FrequencyEnumerator(dateProvider.now(), wrapper.getFrequency()).next().next().getPointInTime();
        assertEquals(expectedPointInTime, wrapper.getSatisfiedUntil());
        assertEquals(expectedPointInTime, carePlanModel.getSatisfiedUntil());
    }

    @Test
    public void createCarePlan_transfersThresholdsFromPlanDefinition() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1, List.of(QUESTIONNAIRE_ID_1), List.of(PLANDEFINITION_ID_1));

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.empty());

        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResource(buildQuestionnaire(QUESTIONNAIRE_ID_1)));

        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitions(List.of(PLANDEFINITION_ID_1))).thenReturn(lookupResult);

        var questionnaireThreshold = new ThresholdModel();
        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel(QUESTIONNAIRE_ID_1, questionnaireThreshold);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

        // Act
        String result = subject.createCarePlan(carePlanModel);

        // Assert
        var wrapper = carePlanModel.getQuestionnaires().get(0);
        assertEquals(1, wrapper.getThresholds().size());
        assertEquals(questionnaireThreshold, wrapper.getThresholds().get(0));
    }

    @Test
    public void createCarePlan_missingThresholds_throwsException() throws Exception {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1, List.of(QUESTIONNAIRE_ID_1), List.of(PLANDEFINITION_ID_1));

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.empty());

        Mockito.when(fhirClient.lookupQuestionnaires(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResource(buildQuestionnaire(QUESTIONNAIRE_ID_1)));

        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitions(List.of(PLANDEFINITION_ID_1))).thenReturn(lookupResult);

        var questionnaireThreshold = new ThresholdModel();
        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel(QUESTIONNAIRE_ID_2, questionnaireThreshold);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, lookupResult)).thenReturn(planDefinitionModel);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

        // Act
        String result = subject.createCarePlan(carePlanModel);

        // Assert
        assertThrows(IllegalStateException.class, () -> subject.createCarePlan(carePlanModel));
    }

    @Test
    public void getCarePlanByCpr_carePlansPresent_returnsCarePlans() throws Exception {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

        boolean onlyActiveCarePlans = true;

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient);
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(lookupResult);

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(fhirMapper.mapCarePlan(carePlan, lookupResult)).thenReturn(carePlanModel);

        // Act
        List<CarePlanModel> result = subject.getCarePlansByCpr(CPR_1, onlyActiveCarePlans);

        // Assert
        assertEquals(1, result.size());
        assertEquals(carePlanModel, result.get(0));
    }

    @Test
    public void getCarePlanByCpr_carePlansMissing_returnsEmptyList() throws Exception {
        // Arrange
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);
        boolean onlyActiveCarePlans = true;

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(FhirLookupResult.fromResources());

        // Act
        List<CarePlanModel> result = subject.getCarePlansByCpr(CPR_1, onlyActiveCarePlans);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_carePlansPresent_returnsCarePlans() throws Exception {
        // Arrange
        boolean onlyActiveCarePlans = true;
        int pageNumber = 1;
        int pageSize = 4;
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(carePlan);
        Mockito.when(fhirClient.lookupCarePlansUnsatisfiedAt(POINT_IN_TIME, onlyActiveCarePlans, 0, 4)).thenReturn(lookupResult);

        CarePlanModel carePlanModel = new CarePlanModel();
        Mockito.when(fhirMapper.mapCarePlan(carePlan, lookupResult)).thenReturn(carePlanModel);

        // Act
        List<CarePlanModel> result = subject.getCarePlansWithUnsatisfiedSchedules(onlyActiveCarePlans, new PageDetails(pageNumber, pageSize));

        // Assert
        assertEquals(1, result.size());
        assertEquals(carePlanModel, result.get(0));
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_carePlansMissing_returnsEmptyList() throws Exception {
        // Arrange
        boolean onlyActiveCarePlans = true;
        int pageNumber = 1;
        int pageSize = 4;
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        Mockito.when(fhirClient.lookupCarePlansUnsatisfiedAt(POINT_IN_TIME, onlyActiveCarePlans, 0, 4)).thenReturn(FhirLookupResult.fromResources());

        // Act
        List<CarePlanModel> result = subject.getCarePlansWithUnsatisfiedSchedules(onlyActiveCarePlans, new PageDetails(pageNumber, pageSize));

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_translatesPagingParameters() throws Exception {
        // Arrange
        boolean onlyActiveCarePlans = true;
        int pageNumber = 3;
        int pageSize = 4;
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        Mockito.when(fhirClient.lookupCarePlansUnsatisfiedAt(POINT_IN_TIME, onlyActiveCarePlans, 8, 4)).thenReturn(FhirLookupResult.fromResources());

        // Act
        List<CarePlanModel> result = subject.getCarePlansWithUnsatisfiedSchedules(onlyActiveCarePlans, new PageDetails(pageNumber, pageSize));

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
    public void updateCarePlan_questionnaireAccessViolation_throwsException() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";
        List<String> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        Map<String, FrequencyModel> frequencies = Map.of();
        PatientDetails patientDetails = buildPatientDetails();

        String patientPrimaryPhone = "12345678";
        String patientSecondaryPhone = "87654321";
        ContactDetailsModel patientPrimaryContactDetails = new ContactDetailsModel();

        PlanDefinition planDefinition = new PlanDefinition();
        Mockito.when(fhirClient.lookupPlanDefinitions(planDefinitionIds)).thenReturn(FhirLookupResult.fromResource(planDefinition));
        Questionnaire questionnaire = new Questionnaire();
        Mockito.when(fhirClient.lookupQuestionnaires(questionnaireIds)).thenReturn(FhirLookupResult.fromResource(questionnaire));

        Mockito.doNothing().when(accessValidator).validateAccess(List.of(planDefinition));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(questionnaire));

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails));
    }

    @Test
    public void updateCarePlan_carePlanAccessViolation_throwsException() throws Exception {
        // Arrange
        String carePlanId = "CarePlan/careplan-1";
        List<String> planDefinitionIds = List.of();
        List<String> questionnaireIds = List.of();
        Map<String, FrequencyModel> frequencies = Map.of();
        PatientDetails patientDetails = buildPatientDetails();

        Mockito.when(fhirClient.lookupPlanDefinitions(planDefinitionIds)).thenReturn(FhirLookupResult.fromResources());
        Mockito.when(fhirClient.lookupQuestionnaires(questionnaireIds)).thenReturn(FhirLookupResult.fromResources());

        CarePlan carePlan = new CarePlan();
        carePlan.setId(carePlanId);
        Mockito.when(fhirClient.lookupCarePlanById(carePlanId)).thenReturn(FhirLookupResult.fromResource(carePlan));
        Mockito.when(fhirClient.lookupQuestionnaires(List.of())).thenReturn(FhirLookupResult.fromResources());

        Mockito.doNothing().when(accessValidator).validateAccess(List.of());
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);

        // Act

        // Assert
        assertThrows(AccessValidationException.class, () -> subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails));
    }

    @Test
    public void updateCarePlan_transfersThresholdsFromPlanDefinition() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";
        List<String> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        Map<String, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.MON), "07:00"));
        PatientDetails patientDetails = buildPatientDetails();

        PlanDefinition planDefinition = new PlanDefinition();
        FhirLookupResult planDefinitionResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitions(planDefinitionIds)).thenReturn(planDefinitionResult);

        var threshold = new ThresholdModel();
        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel(QUESTIONNAIRE_ID_1, threshold);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, planDefinitionResult)).thenReturn(planDefinitionModel);

        Questionnaire questionnaire = new Questionnaire();
        Mockito.when(fhirClient.lookupQuestionnaires(questionnaireIds)).thenReturn(FhirLookupResult.fromResource(questionnaire));

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);
        FhirLookupResult carePlanResult = FhirLookupResult.fromResources(carePlan, patient, planDefinition);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(carePlanResult);

        PatientModel patientModel = buildPatientModel(PATIENT_ID_1);
        Mockito.when(fhirMapper.mapPatient(patient)).thenReturn(patientModel);

        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1, planDefinitionIds, questionnaireIds);
        carePlanModel.setPatient(patientModel);
        Mockito.when(fhirMapper.mapCarePlan(carePlan, carePlanResult)).thenReturn(carePlanModel);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        // Act
        subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails);

        // Assert
        assertEquals(1, carePlanModel.getQuestionnaires().size());
        assertEquals(threshold, carePlanModel.getQuestionnaires().get(0).getThresholds().get(0));
    }

    @Test
    public void updateCarePlan_updatesPatientDetails() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";
        List<String> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        Map<String, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.MON), "07:00"));
        PatientDetails patientDetails = buildPatientDetails();

        PlanDefinition planDefinition = new PlanDefinition();
        FhirLookupResult planDefinitionResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitions(planDefinitionIds)).thenReturn(planDefinitionResult);

        var threshold = new ThresholdModel();
        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel(QUESTIONNAIRE_ID_1, threshold);
        Mockito.when(fhirMapper.mapPlanDefinition(planDefinition, planDefinitionResult)).thenReturn(planDefinitionModel);

        Questionnaire questionnaire = new Questionnaire();
        Mockito.when(fhirClient.lookupQuestionnaires(questionnaireIds)).thenReturn(FhirLookupResult.fromResource(questionnaire));

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);
        FhirLookupResult carePlanResult = FhirLookupResult.fromResources(carePlan, patient, planDefinition);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(carePlanResult);

        PatientModel patientModel = buildPatientModel(PATIENT_ID_1);
        Mockito.when(fhirMapper.mapPatient(patient)).thenReturn(patientModel);

        CarePlanModel carePlanModel = buildCarePlanModel(CPR_1, planDefinitionIds, questionnaireIds);
        carePlanModel.setPatient(patientModel);
        Mockito.when(fhirMapper.mapCarePlan(carePlan, carePlanResult)).thenReturn(carePlanModel);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        // Act
        subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails);

        // Assert
        assertEquals(patientDetails.getPatientPrimaryPhone(), patientModel.getPatientContactDetails().getPrimaryPhone());
        assertEquals(patientDetails.getPatientSecondaryPhone(), patientModel.getPatientContactDetails().getSecondaryPhone());
        assertEquals(patientDetails.getPrimaryRelativeName(), patientModel.getPrimaryRelativeName());
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

    private PatientModel buildPatientModel(String patientId) {
        PatientModel patientModel = new PatientModel();

        patientModel.setId(new QualifiedId(patientId));
        patientModel.setPatientContactDetails(new ContactDetailsModel());

        return patientModel;
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

    private FrequencyModel buildFrequencyModel(List<Weekday> weekdays, String timeOfDay) {
        FrequencyModel frequencyModel = new FrequencyModel();

        frequencyModel.setWeekdays(weekdays);
        frequencyModel.setTimeOfDay(LocalTime.parse(timeOfDay));

        return frequencyModel;
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

    private PatientDetails buildPatientDetails() {
        PatientDetails patientDetails = new PatientDetails();

        patientDetails.setPatientPrimaryPhone("11223344");
        patientDetails.setPatientSecondaryPhone("44332211");
        patientDetails.setPrimaryRelativeName("Dronning Margrethe");
        patientDetails.setPrimaryRelativeAffiliation("Ven");
        patientDetails.setPrimaryRelativePrimaryPhone("98798798");
        patientDetails.setPrimaryRelativeSecondaryPhone("78978978");

        return patientDetails;
    }

    private PlanDefinition buildPlanDefinition(String planDefinitionId) {
        PlanDefinition planDefinition = new PlanDefinition();

        planDefinition.setId(planDefinitionId);

        return planDefinition;
    }

    private PlanDefinitionModel buildPlanDefinitionModel(String questionnaireId, ThresholdModel questionnaireThreshold) {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        QuestionnaireWrapperModel questionnaireWrapperModel = new QuestionnaireWrapperModel();
        questionnaireWrapperModel.setQuestionnaire(new QuestionnaireModel());
        questionnaireWrapperModel.getQuestionnaire().setId(new QualifiedId(QUESTIONNAIRE_ID_1));
        questionnaireWrapperModel.setThresholds(List.of(questionnaireThreshold));

        planDefinitionModel.setQuestionnaires(List.of(questionnaireWrapperModel));

        return planDefinitionModel;
    }

    private Questionnaire buildQuestionnaire(String questionnaireId) {
        Questionnaire questionnaire = new Questionnaire();

        questionnaire.setId(questionnaireId);

        return questionnaire;
    }
}