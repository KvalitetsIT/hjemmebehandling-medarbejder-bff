package dk.kvalitetsit.hjemmebehandling.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.fhir.ExtensionMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class CarePlanServiceTest {
    private static final String ORGANISATION_ID_1 = "";
    private static final String ORGANISATION_ID_2 = "";
    private static final String CPR_1 = "0101010101";
    private static final String CPR_2 = "0202020202";
    private static final String ORGANIZATION_ID_1 = "Infektionsmedicinsk";
    private static final String CAREPLAN_ID_1 = "CarePlan/careplan-1";
    private static final String CAREPLAN_ID_2 = "CarePlan/careplan-2";
    private static final String PATIENT_ID_1 = "Patient/patient-1";
    private static final String PATIENT_ID_2 = "Patient/patient-2";
    private static final String PLANDEFINITION_ID_1 = "PlanDefinition/plandefinition-1";
    private static final String PLANDEFINITION_ID_2 = "PlanDefinition/plandefinition-2";
    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";
    private static final String QUESTIONNAIRE_ID_2 = "Questionnaire/questionnaire-2";
    private static final Instant POINT_IN_TIME = Instant.parse("2021-11-23T10:00:00.000Z");

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
    private CustomUserClient customUserService;

    private static Stream<Arguments> getCarePlansWithUnsatisfiedSchedules_sortCareplans_byPatientName() {
        HumanName a_a = new HumanName().addGiven("a").setFamily("a");
        HumanName a_b = new HumanName().addGiven("a").setFamily("b");
        HumanName b_a = new HumanName().addGiven("b").setFamily("a");
        HumanName b_b = new HumanName().addGiven("b").setFamily("b");
        return Stream.of(
                //getPointInTime_initializedWithSeed
                Arguments.of(a_a, a_b, List.of(a_a, a_b)),
                Arguments.of(a_b, a_a, List.of(a_a, a_b)),
                Arguments.of(b_a, b_b, List.of(b_a, b_b)),
                Arguments.of(b_b, b_a, List.of(b_a, b_b))
        );
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void createCareplan_ThrowsBadGateway_WhenCustomloginFails() throws ServiceException, AccessValidationException, JsonProcessingException {
        ReflectionTestUtils.setField(subject, "patientidpApiUrl", "http://foo");
        Mockito.when(customUserService.createUser(any())).thenThrow(JsonProcessingException.class);

        try {
            CarePlanModel carePlanModel = buildCarePlanModel();

            CarePlan carePlan = new CarePlan();
            Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

            Patient patient = new Patient();
            patient.setId(PATIENT_ID_1);
            Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.empty());

            FhirLookupResult lookupResult = FhirLookupResult.fromResources();
            boolean onlyActiveCarePlans = true;
            Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(lookupResult);

            Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

            Mockito.when(fhirClient.saveCarePlan(any())).thenReturn("1");

            String result = subject.createCarePlan(carePlanModel);
            fail("No error was thrown");
        } catch (ServiceException e) {
            assertEquals(ErrorDetails.CUSTOMLOGIN_UNKNOWN_ERROR, e.getErrorDetails());
            assertEquals(ErrorKind.BAD_GATEWAY, e.getErrorKind());
        }
    }

    @Test
    public void createCarePlan_patientDoesNotExist_patientIsCreated() throws Exception {
        CarePlanModel carePlanModel = buildCarePlanModel();

        CarePlan carePlan = new CarePlan();
        Patient patient = new Patient();

        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);
        Mockito.when(fhirMapper.mapPatientModel(carePlanModel.patient())).thenReturn(patient);
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.empty());
        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

        subject.createCarePlan(carePlanModel);
        Mockito.verify(fhirClient).saveCarePlan(carePlan, patient);
    }

    @Test
    public void createCarePlan_activePlanExists_throwsException() throws Exception {
        CarePlanModel carePlanModel = buildCarePlanModel();

        Patient patient = new Patient();
        patient.setId(PATIENT_ID_1);
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        CarePlan existingCareplan = new CarePlan();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(existingCareplan);
        boolean onlyActiveCarePlans = true;
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(lookupResult);

        assertThrows(ServiceException.class, () -> subject.createCarePlan(carePlanModel));
    }

    @Test
    public void createCarePlan_questionnaireAccessViolation_throwsException() throws Exception {
        CarePlanModel carePlanModel = buildCarePlanModel(List.of(), List.of(QUESTIONNAIRE_ID_1));
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.empty());
        Questionnaire questionnaire = new Questionnaire();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(questionnaire);
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(lookupResult);
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(questionnaire));
        assertThrows(AccessValidationException.class, () -> subject.createCarePlan(carePlanModel));
    }

    /*
    @Test
    public void createCarePlan_success() throws Exception {
        CarePlanModel carePlanModel = buildCarePlanModel();

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Patient patient = new Patient();
        patient.setId(PATIENT_ID_1);
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources();
        boolean onlyActiveCarePlans = true;
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(lookupResult);

        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));
        Mockito.when(fhirClient.saveCarePlan(any())).thenReturn("1");

        String result = subject.createCarePlan(carePlanModel);
        
        assertEquals("1", result);
    }

    @Test
    public void createCarePlan_persistingFails_throwsException() throws Exception {
        
        CarePlanModel carePlanModel = buildCarePlanModel();

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Patient patient = new Patient();
        patient.setId(PATIENT_ID_1);
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        boolean onlyActiveCarePlans = true;
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(FhirLookupResult.fromResources());
        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));
        Mockito.when(fhirClient.saveCarePlan(carePlan)).thenThrow(IllegalStateException.class);

        assertThrows(ServiceException.class, () -> subject.createCarePlan(carePlanModel));
    }

    @Test
    public void createCarePlan_populatesId() throws Exception {
        
        CarePlanModel carePlanModel = buildCarePlanModel();
        carePlanModel.setId(new QualifiedId(CAREPLAN_ID_1));

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Patient patient = new Patient();
        patient.setId(PATIENT_ID_1);
        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));

        FhirLookupResult lookupResult = FhirLookupResult.fromResources();
        boolean onlyActiveCarePlans = true;
        Mockito.when(fhirClient.lookupCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(lookupResult);

        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

        Mockito.when(fhirClient.saveCarePlan(any())).thenReturn("1");
        
        subject.createCarePlan(carePlanModel);
        
        assertNull(carePlanModel.getId());
    }
*/

    @Test
    public void createCarePlan_populatesSatisfiedUntil() throws Exception {

        CarePlanModel carePlanModel = buildCarePlanModel(List.of(PLANDEFINITION_ID_1), List.of(QUESTIONNAIRE_ID_1));

        CarePlan carePlan = new CarePlan();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.empty());

        Mockito.when(fhirClient.lookupQuestionnairesById(List.of(QUESTIONNAIRE_ID_1))).thenReturn(FhirLookupResult.fromResource(buildQuestionnaire()));

        PlanDefinition planDefinition = buildPlanDefinition();
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitionsById(List.of(PLANDEFINITION_ID_1))).thenReturn(lookupResult);

        var questionnaireThreshold = new ThresholdModel(null, null, null, null, null, null);
        buildPlanDefinitionModel(QUESTIONNAIRE_ID_1, questionnaireThreshold);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

        subject.createCarePlan(carePlanModel);

        var wrapper = carePlanModel.questionnaires().getFirst();
        var expectedPointInTime = new FrequencyEnumerator(wrapper.frequency()).getSatisfiedUntilForInitialization(dateProvider.now());
        assertEquals(expectedPointInTime, wrapper.satisfiedUntil());
        assertEquals(expectedPointInTime, carePlanModel.satisfiedUntil());
    }


    @Test
    public void getCarePlanByCpr_carePlansPresent_returnsCarePlans() throws Exception {

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

        boolean onlyActiveCarePlans = true;
        boolean onlyUnSatisfied = false;

        //Mockito.when(fhirClient.lookupPatientByCpr(CPR_1)).thenReturn(Optional.of(patient));
        var unstaisfiedAt = Instant.now();
        Mockito.when(dateProvider.now()).thenReturn(unstaisfiedAt);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient);
        Mockito.when(fhirClient.lookupCarePlans(CPR_1, unstaisfiedAt, onlyActiveCarePlans, onlyUnSatisfied)).thenReturn(lookupResult);

        CarePlanModel carePlanModel = CarePlanModel.builder().build();
        Mockito.when(fhirMapper.mapCarePlan(carePlan, lookupResult, ORGANISATION_ID_1)).thenReturn(carePlanModel);
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);


        List<CarePlanModel> result = subject.getCarePlansWithFilters(CPR_1, onlyActiveCarePlans, onlyUnSatisfied, new Pagination(1, 10));


        assertEquals(1, result.size());
        assertEquals(carePlanModel, result.getFirst());
    }

    @Test
    public void getCarePlanByCpr_carePlansMissing_returnsEmptyList() throws Exception {

        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);
        boolean onlyActiveCarePlans = true;
        boolean onlyUnSatisfied = false;

        var unstaisfiedAt = Instant.now();
        Mockito.when(dateProvider.now()).thenReturn(unstaisfiedAt);
        Mockito.when(fhirClient.lookupCarePlans(CPR_1, unstaisfiedAt, onlyActiveCarePlans, onlyUnSatisfied)).thenReturn(FhirLookupResult.fromResources());


        List<CarePlanModel> result = subject.getCarePlansWithFilters(CPR_1, onlyActiveCarePlans, onlyUnSatisfied, new Pagination(1, 10));


        assertEquals(0, result.size());
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_carePlansPresent_returnsCarePlans() throws Exception {

        boolean onlyActiveCarePlans = true;
        boolean unsatisfied = true;
        int pageNumber = 1;
        int pageSize = 4;
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResource(carePlan);
        Mockito.when(fhirClient.lookupCarePlans(POINT_IN_TIME, onlyActiveCarePlans, unsatisfied)).thenReturn(lookupResult);

        CarePlanModel carePlanModel = CarePlanModel.builder().build();
        Mockito.when(fhirMapper.mapCarePlan(carePlan, lookupResult, ORGANISATION_ID_1)).thenReturn(carePlanModel);
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);


        List<CarePlanModel> result = subject.getCarePlansWithFilters(onlyActiveCarePlans, unsatisfied, new Pagination(pageNumber, pageSize));


        assertEquals(1, result.size());
        assertEquals(carePlanModel, result.getFirst());
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_carePlansMissing_returnsEmptyList() throws Exception {
        boolean onlyActiveCarePlans = true;
        boolean unsatisfied = true;

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(fhirClient.lookupCarePlans(POINT_IN_TIME, onlyActiveCarePlans, unsatisfied)).thenReturn(FhirLookupResult.fromResources());

        List<CarePlanModel> result = subject.getCarePlansWithFilters(onlyActiveCarePlans, unsatisfied, new Pagination(1, 4));

        assertEquals(0, result.size());
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_translatesPagingParameters() throws Exception {
        boolean onlyActiveCarePlans = true;
        boolean unsatisfied = true;

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(fhirClient.lookupCarePlans(POINT_IN_TIME, onlyActiveCarePlans, unsatisfied)).thenReturn(FhirLookupResult.fromResources());

        List<CarePlanModel> result = subject.getCarePlansWithFilters(onlyActiveCarePlans, unsatisfied, new Pagination(3, 4));
    }

    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void getCarePlansWithUnsatisfiedSchedules_sortCareplans_byPatientName(HumanName name1, HumanName name2, List<HumanName> expectedOrder) throws Exception {
        boolean onlyActiveCarePlans = true;
        boolean unsatisfied = true;

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        CarePlan carePlan1 = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        CarePlan carePlan2 = buildCarePlan(CAREPLAN_ID_2, PATIENT_ID_2);
        Patient patient1 = buildPatient(PATIENT_ID_1, CPR_1, name1.getGivenAsSingleString(), name1.getFamily());
        Patient patient2 = buildPatient(PATIENT_ID_2, CPR_2, name2.getGivenAsSingleString(), name2.getFamily());

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan1, carePlan2, patient1, patient2);
        Mockito.when(fhirClient.lookupCarePlans(POINT_IN_TIME, onlyActiveCarePlans, unsatisfied)).thenReturn(lookupResult);

        CarePlanModel carePlanModel1 = CarePlanModel.builder().patient(buildPatientModel(PATIENT_ID_1, name1.getGivenAsSingleString(), name1.getFamily())).build();
        CarePlanModel carePlanModel2 = CarePlanModel.builder().patient(buildPatientModel(PATIENT_ID_2, name2.getGivenAsSingleString(), name2.getFamily())).build();

        Mockito.when(fhirMapper.mapCarePlan(carePlan1, lookupResult, ORGANISATION_ID_1)).thenReturn(carePlanModel1);
        Mockito.when(fhirMapper.mapCarePlan(carePlan2, lookupResult, ORGANISATION_ID_2)).thenReturn(carePlanModel2);
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);

        List<CarePlanModel> result = subject.getCarePlansWithFilters(onlyActiveCarePlans, unsatisfied);

        assertEquals(2, result.size());
        assertEquals(expectedOrder.get(0).getGivenAsSingleString(), result.get(0).patient().givenName());
        assertEquals(expectedOrder.get(0).getFamily(), result.get(0).patient().familyName());
        assertEquals(expectedOrder.get(1).getGivenAsSingleString(), result.get(1).patient().givenName());
        assertEquals(expectedOrder.get(1).getFamily(), result.get(1).patient().familyName());
    }

    @Test
    public void getCarePlanById_carePlanPresent_returnsCarePlan() throws Exception {
        String carePlanId = "CarePlan/careplan-1";
        String patientId = "Patient/patient-1";
        CarePlan carePlan = buildCarePlan(carePlanId, patientId);
        CarePlanModel carePlanModel = setupCarePlan(carePlan);
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);
        Optional<CarePlanModel> result = subject.getCarePlanById(carePlanId);
        assertEquals(carePlanModel, result.get());
    }

    @Test
    public void getCarePlanById_carePlanForDifferentOrganization_throwsException() throws Exception {
        String carePlanId = "CarePlan/careplan-1";
        String patientId = "Patient/patient-1";
        CarePlan carePlan = buildCarePlan(carePlanId, patientId);
        Mockito.when(fhirClient.lookupCarePlanById(carePlan.getId())).thenReturn(FhirLookupResult.fromResource(carePlan));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);
        assertThrows(AccessValidationException.class, () -> subject.getCarePlanById(carePlanId));
    }

    @Test
    public void getCarePlanById_carePlanMissing_returnsEmpty() throws Exception {
        String carePlanId = CAREPLAN_ID_1;
        Mockito.when(fhirClient.lookupCarePlanById(carePlanId)).thenReturn(FhirLookupResult.fromResources());
        Optional<CarePlanModel> result = subject.getCarePlanById(carePlanId);
        assertFalse(result.isPresent());
    }

    @Test
    public void resolveAlarm_carePlanMissing_throwsException() throws ServiceException {

        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(FhirLookupResult.fromResources());


        assertThrows(ServiceException.class, () -> subject.resolveAlarm(CAREPLAN_ID_1, QUESTIONNAIRE_ID_1));
    }

    @Test
    public void resolveAlarm_accessViolation_throwsException() throws Exception {
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(FhirLookupResult.fromResources(carePlan));

        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);

        assertThrows(AccessValidationException.class, () -> subject.resolveAlarm(CAREPLAN_ID_1, QUESTIONNAIRE_ID_1));
    }

    @Test
    public void resolveAlarm_carePlanSatisfiedIntoTheFuture_throwsException() throws Exception {
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(lookupResult);

        CarePlanModel carePlanModel = CarePlanModel.builder().satisfiedUntil(POINT_IN_TIME.plusSeconds(200)).build();
        Mockito.when(fhirMapper.mapCarePlan(carePlan, lookupResult, ORGANISATION_ID_1)).thenReturn(carePlanModel);
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        assertThrows(ServiceException.class, () -> subject.resolveAlarm(CAREPLAN_ID_1, QUESTIONNAIRE_ID_1));
    }

    @Test
    public void resolveAlarm_recomputesSatisfiedUntil_savesCarePlan() throws Exception {

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(lookupResult);

        CarePlanModel carePlanModel = CarePlanModel.builder().satisfiedUntil(POINT_IN_TIME.minusSeconds(100))
                .questionnaires(List.of(
                        buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1, POINT_IN_TIME.minusSeconds(100)),
                        buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2, POINT_IN_TIME.plusSeconds(100))
                )).build();

        Mockito.when(fhirMapper.mapCarePlan(carePlan, lookupResult, ORGANISATION_ID_1)).thenReturn(carePlanModel);
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);

        subject.resolveAlarm(CAREPLAN_ID_1, QUESTIONNAIRE_ID_1);

        // Verify that the first questionnaire has its satisfied-timestamp pushed to the next scheduled weekday,
        // the second questionnaire has its timestamp left untouched, and the careplan has its timestamp set to
        // the earliest timestamp (now that of the second questionnaire).
        assertEquals(Instant.parse("2021-11-30T03:00:00.000Z"), carePlanModel.questionnaires().get(0).satisfiedUntil());
        assertEquals(POINT_IN_TIME.plusSeconds(100), carePlanModel.questionnaires().get(1).satisfiedUntil());
        assertEquals(POINT_IN_TIME.plusSeconds(100), carePlanModel.satisfiedUntil());

        Mockito.verify(fhirClient).update(carePlan);
    }

    @Test
    public void completeCareplan_NoQuestionnaireResponses_and_satisfiedSchedule_ShouldBeCompleted() throws ServiceException {
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        FhirLookupResult careplanResult = FhirLookupResult.fromResources(carePlan);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(careplanResult);

        FhirLookupResult questionnaireResponsesResult = FhirLookupResult.fromResources();
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), CAREPLAN_ID_1)).thenReturn(questionnaireResponsesResult);

        carePlan.setExtension(List.of(ExtensionMapper.mapCarePlanSatisfiedUntil(Instant.now().plus(1, ChronoUnit.DAYS))));

        //Action and assert
        assertDoesNotThrow(() -> subject.completeCarePlan(CAREPLAN_ID_1));
    }

    @Test
    public void completeCareplan_OneQuestionnaireResponses_ShouldThrowError() throws ServiceException {

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        FhirLookupResult careplanResult = FhirLookupResult.fromResources(carePlan);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(careplanResult);

        FhirLookupResult questionnaireResponsesResult = FhirLookupResult.fromResources(new QuestionnaireResponse());
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), CAREPLAN_ID_1)).thenReturn(questionnaireResponsesResult);

        //Action and assert
        try {
            subject.completeCarePlan(CAREPLAN_ID_1);
            fail("Careplan should be failing due to questionnaireresponses on careplan");
        } catch (ServiceException serviceException) {
            assertEquals(ErrorKind.BAD_REQUEST, serviceException.getErrorKind());
            assertEquals(ErrorDetails.CAREPLAN_HAS_UNHANDLED_QUESTIONNAIRERESPONSES, serviceException.getErrorDetails());
        }
    }

    @Test
    public void completeCareplan_unsatisfiedSchedule_ShouldThrowError() throws ServiceException {
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        FhirLookupResult careplanResult = FhirLookupResult.fromResources(carePlan);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(careplanResult);

        FhirLookupResult questionnaireResponsesResult = FhirLookupResult.fromResources();
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), CAREPLAN_ID_1)).thenReturn(questionnaireResponsesResult);

        carePlan.setExtension(List.of(ExtensionMapper.mapCarePlanSatisfiedUntil(Instant.now().minus(1, ChronoUnit.DAYS))));

        try {
            subject.completeCarePlan(CAREPLAN_ID_1);
            fail("Careplan should be failing due to questionnaireresponses on careplan");
        } catch (ServiceException serviceException) {
            assertEquals(ErrorKind.BAD_REQUEST, serviceException.getErrorKind());
            assertEquals(ErrorDetails.CAREPLAN_IS_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES, serviceException.getErrorDetails());
        }
    }

    @Test
    public void updateCarePlan_questionnaireAccessViolation_throwsException() throws Exception {
        String carePlanId = "careplan-1";
        List<String> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        Map<String, FrequencyModel> frequencies = Map.of();
        PatientDetails patientDetails = buildPatientDetails();

        PlanDefinition planDefinition = new PlanDefinition();
        Mockito.when(fhirClient.lookupPlanDefinitionsById(planDefinitionIds)).thenReturn(FhirLookupResult.fromResource(planDefinition));
        Questionnaire questionnaire = new Questionnaire();
        Mockito.when(fhirClient.lookupQuestionnairesById(questionnaireIds)).thenReturn(FhirLookupResult.fromResource(questionnaire));

        Mockito.doNothing().when(accessValidator).validateAccess(List.of(planDefinition));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(questionnaire));

        assertThrows(AccessValidationException.class, () -> subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies,patientDetails));
    }

    @Test
    public void updateCarePlan_carePlanAccessViolation_throwsException() throws Exception {
        String carePlanId = "CarePlan/careplan-1";
        List<String> planDefinitionIds = List.of();
        List<String> questionnaireIds = List.of();
        Map<String, FrequencyModel> frequencies = Map.of();
        PatientDetails patientDetails = buildPatientDetails();

        Mockito.when(fhirClient.lookupPlanDefinitionsById(planDefinitionIds)).thenReturn(FhirLookupResult.fromResources());
        Mockito.when(fhirClient.lookupQuestionnairesById(questionnaireIds)).thenReturn(FhirLookupResult.fromResources());

        CarePlan carePlan = new CarePlan();
        carePlan.setId(carePlanId);

        Mockito.when(fhirClient.lookupCarePlanById(carePlanId)).thenReturn(FhirLookupResult.fromResource(carePlan));
        Mockito.when(fhirClient.lookupQuestionnairesById(List.of())).thenReturn(FhirLookupResult.fromResources());
        Mockito.doNothing().when(accessValidator).validateAccess(List.of());
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);

        assertThrows(AccessValidationException.class, () -> subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds,frequencies, patientDetails));
    }

    @Test
    public void updateCarePlan_transfersThresholdsFromPlanDefinition() throws Exception {
        String carePlanId = "careplan-1";
        List<String> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        Map<String, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.MON), "07:00"));
        PatientDetails patientDetails = buildPatientDetails();
        PatientModel patientModel = buildPatientModel();
        Questionnaire questionnaire = new Questionnaire();
        var threshold = new ThresholdModel(null, null, null, null, null, null);

        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel(QUESTIONNAIRE_ID_1, threshold);
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

        var contact = new Patient.ContactComponent();
        contact.setName((new HumanName()).setGiven(List.of(new StringType("lonnie"))));
        var affiliation = new CodeableConcept();
        affiliation.setText("Tante");
        contact.setRelationship(List.of(affiliation));
        contact.setOrganization(this.buildReference());

        patient.setContact(new ArrayList<Patient.ContactComponent>(List.of(contact)));
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        PlanDefinition planDefinition = new PlanDefinition();
        FhirLookupResult planDefinitionResult = FhirLookupResult.fromResource(planDefinition);
        CarePlanModel carePlanModel = buildCarePlanModel(planDefinitionIds, questionnaireIds, patientModel);
        FhirLookupResult carePlanResult = FhirLookupResult.fromResources(carePlan, patient, planDefinition);

        Mockito.when(fhirClient.lookupPlanDefinitionsById(planDefinitionIds)).thenReturn(planDefinitionResult);
        Mockito.when(fhirMapper.mapPlanDefinitionResult(planDefinition, planDefinitionResult)).thenReturn(planDefinitionModel);
        Mockito.when(fhirClient.lookupQuestionnairesById(questionnaireIds)).thenReturn(FhirLookupResult.fromResource(questionnaire));
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(carePlanResult);
        Mockito.when(fhirMapper.mapPatient(patient, "")).thenReturn(patientModel);
        Mockito.when(fhirMapper.mapPatientModel(patientModel)).thenReturn(patient);
        Mockito.when(fhirMapper.mapCarePlan(carePlan, carePlanResult, ORGANISATION_ID_1)).thenReturn(carePlanModel);
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId)).thenReturn(FhirLookupResult.fromResources());
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);

        subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails);

        assertEquals(1, carePlanModel.questionnaires().size());
        assertEquals(threshold, carePlanModel.questionnaires().getFirst().thresholds().getFirst());
    }

    @Test
    public void updateCarePlan_updatesPatientDetails() throws Exception {
        String carePlanId = "careplan-1";
        List<String> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        Map<String, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.MON), "07:00"));
        PatientDetails patientDetails = buildPatientDetails();

        PlanDefinition planDefinition = new PlanDefinition();
        FhirLookupResult planDefinitionResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitionsById(planDefinitionIds)).thenReturn(planDefinitionResult);

        var threshold = new ThresholdModel(null, null, null, null, null, null);
        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel(QUESTIONNAIRE_ID_1, threshold);
        Mockito.when(fhirMapper.mapPlanDefinitionResult(planDefinition, planDefinitionResult)).thenReturn(planDefinitionModel);

        Questionnaire questionnaire = new Questionnaire();
        Mockito.when(fhirClient.lookupQuestionnairesById(questionnaireIds)).thenReturn(FhirLookupResult.fromResource(questionnaire));

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);
        FhirLookupResult carePlanResult = FhirLookupResult.fromResources(carePlan, patient, planDefinition);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(carePlanResult);

        PatientModel patientModel = buildPatientModel();
        Mockito.when(fhirMapper.mapPatient(patient, "")).thenReturn(patientModel);
        Mockito.when(fhirMapper.mapPatientModel(patientModel)).thenReturn(patient);

        CarePlanModel carePlanModel = buildCarePlanModel(planDefinitionIds, questionnaireIds, patientModel);
        Mockito.when(fhirMapper.mapCarePlan(carePlan, carePlanResult, ORGANISATION_ID_1)).thenReturn(carePlanModel);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId)).thenReturn(FhirLookupResult.fromResources());
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);

        subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds,frequencies, patientDetails);

        assertEquals(patientDetails.patientPrimaryPhone(), patientModel.contactDetails().primaryPhone());
        assertEquals(patientDetails.patientSecondaryPhone(), patientModel.contactDetails().secondaryPhone());
        assertEquals(patientDetails.primaryRelativeName(), patientModel.primaryContact().name());

    }

    @Test
    public void updateCarePlan_updatesFrequency_keepsMaxOf_currentSatistiedUntil_and_newCalculated() throws Exception {
        String carePlanId = "careplan-1";
        List<String> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        PatientDetails patientDetails = buildPatientDetails();

        PlanDefinition planDefinition = new PlanDefinition();
        FhirLookupResult planDefinitionResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitionsById(planDefinitionIds)).thenReturn(planDefinitionResult);

        var threshold = new ThresholdModel(null, null, null, null, null, null);
        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel(QUESTIONNAIRE_ID_1, threshold);
        Mockito.when(fhirMapper.mapPlanDefinitionResult(planDefinition, planDefinitionResult)).thenReturn(planDefinitionModel);

        Questionnaire questionnaire = new Questionnaire();
        Mockito.when(fhirClient.lookupQuestionnairesById(questionnaireIds)).thenReturn(FhirLookupResult.fromResource(questionnaire));

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);
        FhirLookupResult carePlanResult = FhirLookupResult.fromResources(carePlan, patient, planDefinition);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(carePlanResult);

        PatientModel patientModel = PatientModel
                .builder()
                .id(new QualifiedId(CarePlanServiceTest.PATIENT_ID_1))
                .contactDetails(this.buildContactDetails())
                .primaryContact(PrimaryContactModel.builder().organisation(buildReference().getReference()).build())
                .build();


        Mockito.when(fhirMapper.mapPatient(patient, ORGANISATION_ID_1)).thenReturn(patientModel);

        CarePlanModel carePlanModel = buildCarePlanModel(new QualifiedId(CAREPLAN_ID_1), planDefinitionIds, questionnaireIds, patientModel);


        Mockito.when(fhirMapper.mapCarePlan(carePlan, carePlanResult, ORGANISATION_ID_1)).thenReturn(carePlanModel);
        Mockito.when(fhirMapper.mapPatientModel(patientModel)).thenReturn(patient);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        Map<String, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.MON, Weekday.TUE), "12:00"));
        FrequencyEnumerator fe = new FrequencyEnumerator(frequencies.get(QUESTIONNAIRE_ID_1));
        Instant nextNextSatisfiedUntilTime = fe.getSatisfiedUntilForFrequencyChange(POINT_IN_TIME);
        Mockito.lenient().when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId)).thenReturn(FhirLookupResult.fromResources());

        Mockito.lenient().when(fhirClient.lookupQuestionnaireResponses(carePlanModel.id().id(), List.of("questionnaire-1"))).thenReturn(FhirLookupResult.fromResources());
        //Mockito.doReturn(FhirLookupResult.fromResources()).when(fhirClient).lookupQuestionnaireResponses(CAREPLAN_ID_1, questionnaireIds);

        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);


        subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies,patientDetails);

        //assertTrue(nextNextSatisfiedUntilTime.isBefore(POINT_IN_TIME));
        assertEquals(nextNextSatisfiedUntilTime, carePlanModel.questionnaires().getFirst().satisfiedUntil());
        assertEquals(nextNextSatisfiedUntilTime, carePlanModel.satisfiedUntil());
    }



    @Test
    public void updateCarePlan_updatesFrequency_multipleDays_keepsSatisfiedUntil() throws Exception {
        String carePlanId = "careplan-1";
        List<String> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        PatientDetails patientDetails = buildPatientDetails();

        PlanDefinition planDefinition = new PlanDefinition();
        FhirLookupResult planDefinitionResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitionsById(planDefinitionIds)).thenReturn(planDefinitionResult);


        var threshold = new ThresholdModel(null, null, null, null, null, null);

        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel(QUESTIONNAIRE_ID_1, threshold);
        Mockito.when(fhirMapper.mapPlanDefinitionResult(planDefinition, planDefinitionResult)).thenReturn(planDefinitionModel);

        Questionnaire questionnaire = new Questionnaire();
        Mockito.when(fhirClient.lookupQuestionnairesById(questionnaireIds)).thenReturn(FhirLookupResult.fromResource(questionnaire));

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);
        FhirLookupResult carePlanResult = FhirLookupResult.fromResources(carePlan, patient, planDefinition);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(carePlanResult);

        PatientModel patientModel = buildPatientModel();
        var orgId = "";
        Mockito.when(fhirMapper.mapPatient(patient, orgId)).thenReturn(patientModel);
        Mockito.when(fhirMapper.mapPatientModel(patientModel)).thenReturn(patient);

        CarePlanModel carePlanModel = buildCarePlanModel(planDefinitionIds, questionnaireIds, patientModel);
        Mockito.when(fhirMapper.mapCarePlan(carePlan, carePlanResult, ORGANISATION_ID_1)).thenReturn(carePlanModel);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME.minus(1, ChronoUnit.HOURS).plus(2, ChronoUnit.DAYS));

        // Vi opdaterer frekvens til at indeholde torsdag, men ønsker stadig at beholde nuværende satisfiedUntil for at den blå alarm ikke 'forsvinder' indtil deadline kl. 11
        Map<String, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.TUE, Weekday.THU), "11:00"));
        FrequencyEnumerator fe = new FrequencyEnumerator(frequencies.get(QUESTIONNAIRE_ID_1));
        Instant nextNextSatisfiedUntilTime = fe.getSatisfiedUntilForFrequencyChange(POINT_IN_TIME);
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId)).thenReturn(FhirLookupResult.fromResources());
        Mockito.when(fhirClient.getOrganizationId()).thenReturn(ORGANISATION_ID_1);

        subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds,frequencies, patientDetails);

        assertEquals(nextNextSatisfiedUntilTime, carePlanModel.questionnaires().getFirst().satisfiedUntil());
        assertEquals(nextNextSatisfiedUntilTime, carePlanModel.satisfiedUntil());
    }



    @Test
    void updateCarePlan_removeQuestionnaire_withExeededDeadline_throwsError() throws Exception {
        String carePlanId = "careplan-1";
        List<String> planDefinitionIds = List.of(PLANDEFINITION_ID_2);
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_2);
        Map<String, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_2, buildFrequencyModel(List.of(Weekday.MON), "07:00"));
        PatientDetails patientDetails = buildPatientDetails();

        PlanDefinition planDefinition = new PlanDefinition();
        FhirLookupResult planDefinitionResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitionsById(planDefinitionIds)).thenReturn(planDefinitionResult);

        var threshold = new ThresholdModel(null, null, null, null, null, null);
        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel(QUESTIONNAIRE_ID_2, threshold);
        Mockito.when(fhirMapper.mapPlanDefinitionResult(planDefinition, planDefinitionResult)).thenReturn(planDefinitionModel);

        Questionnaire questionnaire = new Questionnaire();
        Mockito.when(fhirClient.lookupQuestionnairesById(questionnaireIds)).thenReturn(FhirLookupResult.fromResource(questionnaire));

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);
        FhirLookupResult carePlanResult = FhirLookupResult.fromResources(carePlan, patient, planDefinition);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(carePlanResult);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME.plusSeconds(1L));

        try {
            subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails);
            fail("No error was thrown");
        } catch (ServiceException e) {
            assertEquals(ErrorDetails.PLANDEFINITION_CONTAINS_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES, e.getErrorDetails());
        }
    }

    @Test
    void updateCarePlan_removeQuestionnaire_withUnansweredQuestionnaireResponses_throwsError() throws Exception {
        String carePlanId = "careplan-1";
        List<String> planDefinitionIds = List.of(PLANDEFINITION_ID_2);
        List<String> questionnaireIds = List.of(QUESTIONNAIRE_ID_2);
        Map<String, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_2, buildFrequencyModel(List.of(Weekday.MON), "07:00"));
        PatientDetails patientDetails = buildPatientDetails();

        PlanDefinition planDefinition = new PlanDefinition();
        FhirLookupResult planDefinitionResult = FhirLookupResult.fromResource(planDefinition);
        Mockito.when(fhirClient.lookupPlanDefinitionsById(planDefinitionIds)).thenReturn(planDefinitionResult);

        var threshold = new ThresholdModel(null, null, null, null, null, null);
        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel(QUESTIONNAIRE_ID_2, threshold);
        Mockito.when(fhirMapper.mapPlanDefinitionResult(planDefinition, planDefinitionResult)).thenReturn(planDefinitionModel);

        Questionnaire questionnaire = new Questionnaire();
        Mockito.when(fhirClient.lookupQuestionnairesById(questionnaireIds)).thenReturn(FhirLookupResult.fromResource(questionnaire));

        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);
        FhirLookupResult carePlanResult = FhirLookupResult.fromResources(carePlan, patient, planDefinition);
        Mockito.when(fhirClient.lookupCarePlanById(CAREPLAN_ID_1)).thenReturn(carePlanResult);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        questionnaireResponse.setQuestionnaire(QUESTIONNAIRE_ID_1); // unanswered response for questionnaire that is removed
        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId)).thenReturn(FhirLookupResult.fromResources(questionnaireResponse));

        try {
            subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails);
            fail("No error was thrown");
        } catch (ServiceException e) {
            assertEquals(ErrorDetails.PLANDEFINITION_CONTAINS_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES, e.getErrorDetails());
        }
    }


    private CarePlanModel buildCarePlanModel() {
        return buildCarePlanModel(null, null);
    }

    private CarePlanModel buildCarePlanModel(List<String> planDefinitionIds, List<String> questionnaireIds) {
        return buildCarePlanModel(null, planDefinitionIds, questionnaireIds, null);
    }

    private CarePlanModel buildCarePlanModel(List<String> planDefinitionIds, List<String> questionnaireIds, PatientModel patient) {
        return buildCarePlanModel(null, planDefinitionIds, questionnaireIds, patient);

    }

    private CarePlanModel buildCarePlanModel(QualifiedId id, List<String> planDefinitionIds, List<String> questionnaireIds, PatientModel patient) {
        return CarePlanModel.builder()
                .id(id)
                .patient(patient)
                .patient(PatientModel.builder().cpr(CPR_1).build())
                .planDefinitions(List.of())
                .planDefinitions(Optional.ofNullable(planDefinitionIds).map(x1 -> x1.stream().map(this::buildPlanDefinitionModel).toList()).orElse(null))
                .questionnaires(questionnaireIds != null ? questionnaireIds.stream().map(this::buildQuestionnaireWrapperModel).toList() : List.of())
                .build();
    }


    private PatientModel buildPatientModel() {
        return PatientModel
                .builder()
                .id(new QualifiedId(CarePlanServiceTest.PATIENT_ID_1))
                .contactDetails(this.buildContactDetails())
                .primaryContact(this.buildPrimaryContact())
                .build();
    }

    private PrimaryContactModel buildPrimaryContact() {
        return PrimaryContactModel.builder()
                .name("Poul")
                .organisation(this.buildReference().getReference())
                .affiliation("Onkel")
                .build();
    }

    private ContactDetailsModel buildContactDetails() {
        return ContactDetailsModel.builder().build();
    }

    private PatientModel buildPatientModel(String patientId, String givenName, String familyName) {
        return PatientModel.builder()
                .id(new QualifiedId(patientId))
                .contactDetails(ContactDetailsModel.builder().build())
                .givenName(givenName)
                .familyName(familyName)
                .build();
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel(String questionnaireId) {
        return buildQuestionnaireWrapperModel(questionnaireId, POINT_IN_TIME);
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel(String questionnaireId, Instant satisfiedUntil) {
        return QuestionnaireWrapperModel.builder()
                .questionnaire(QuestionnaireModel.builder()
                        .id(new QualifiedId(questionnaireId))
                        .build())
                .frequency(FrequencyModel.builder()
                        .weekdays(List.of(Weekday.TUE))
                        .timeOfDay(LocalTime.parse("04:00")).build())
                .satisfiedUntil(satisfiedUntil).build();
    }

    private PlanDefinitionModel buildPlanDefinitionModel(String planDefinitionId) {
        return PlanDefinitionModel.builder().id(new QualifiedId(planDefinitionId)).build();
    }

    private CarePlanModel setupCarePlan(CarePlan carePlan) throws ServiceException {
        Mockito.when(fhirClient.lookupCarePlanById(carePlan.getId())).thenReturn(FhirLookupResult.fromResource(carePlan));
        CarePlanModel carePlanModel = CarePlanModel.builder().id(new QualifiedId(carePlan.getId())).build();
        Mockito.when(fhirMapper.mapCarePlan(any(CarePlan.class), any(FhirLookupResult.class), any(String.class))).thenReturn(carePlanModel);
        return carePlanModel;
    }

    private CarePlan buildCarePlan(String carePlanId, String patientId) {
        return buildCarePlan(carePlanId, patientId, null);
    }

    private CarePlan buildCarePlan(String carePlanId, String patientId, String questionnaireId) {
        CarePlan carePlan = new CarePlan();
        carePlan.setId(carePlanId);
        carePlan.setSubject(new Reference(patientId));

        if (questionnaireId != null) {
            CarePlan.CarePlanActivityDetailComponent detail = new CarePlan.CarePlanActivityDetailComponent();
            detail.setInstantiatesCanonical(List.of(new CanonicalType(questionnaireId)));
            detail.setScheduled(new Timing());
            detail.addExtension(ExtensionMapper.mapActivitySatisfiedUntil(POINT_IN_TIME));
            carePlan.addActivity().setDetail(detail);
            carePlan.addExtension(ExtensionMapper.mapCarePlanSatisfiedUntil(POINT_IN_TIME));
        }

        return carePlan;
    }

    private FrequencyModel buildFrequencyModel(List<Weekday> weekdays, String timeOfDay) {
        return FrequencyModel.builder()
                .weekdays(weekdays)
                .timeOfDay(LocalTime.parse(timeOfDay)).build();
    }

    private Patient buildPatient(String patientId, String cpr) {
        Patient patient = new Patient();
        patient.setId(patientId);

        var identifier = new Identifier();
        identifier.setSystem(Systems.CPR);
        identifier.setValue(cpr);
        patient.setIdentifier(List.of(identifier));
        var contact = new Patient.ContactComponent();

        var name = new HumanName();
        name.setGiven(List.of(new StringType("Yvonne")));
        contact.setName(name);

        var affiliation = List.of(new CodeableConcept().setText("Moster"));
        contact.setRelationship(affiliation);
        contact.setOrganization(buildReference());

        patient.setContact(new ArrayList<>(List.of(contact)));
        return patient;
    }



    private Reference buildReference() {
        var reference = new Reference();
        reference.setReference("infektionsmedicinsk");
        return reference;
    }

    private Patient buildPatient(String patientId, String cpr, String givenName, String familyName) {
        Patient patient = buildPatient(patientId, cpr);

        patient.getNameFirstRep()
                .setGiven(List.of(new StringType(givenName)))
                .setFamily(familyName);

        return patient;
    }

    private PatientDetails buildPatientDetails() {
        return PatientDetails.builder()
                .patientPrimaryPhone("11223344")
                .patientSecondaryPhone("44332211")
                .primaryRelativeName("Dronning Margrethe")
                .primaryRelativeAffiliation("Ven")
                .primaryRelativePrimaryPhone("98798798")
                .primaryRelativeSecondaryPhone("78978978").build();
    }

    private PlanDefinition buildPlanDefinition() {
        PlanDefinition planDefinition = new PlanDefinition();
        planDefinition.setId(CarePlanServiceTest.PLANDEFINITION_ID_1);
        return planDefinition;
    }

    private PlanDefinitionModel buildPlanDefinitionModel(String questionnaireId, ThresholdModel questionnaireThreshold) {
        return PlanDefinitionModel.builder()
                .questionnaires(List.of(QuestionnaireWrapperModel
                        .builder()
                        .questionnaire(QuestionnaireModel.builder().id(new QualifiedId(questionnaireId)).build())
                        .thresholds(List.of(questionnaireThreshold))
                        .build())).build();
    }

    private Questionnaire buildQuestionnaire() {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setId(CarePlanServiceTest.QUESTIONNAIRE_ID_1);
        return questionnaire;
    }


}