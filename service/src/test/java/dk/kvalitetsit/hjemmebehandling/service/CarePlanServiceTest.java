package dk.kvalitetsit.hjemmebehandling.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.fhir.ExtensionMapper;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.repository.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteCarePlanService;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class CarePlanServiceTest {
    private static final QualifiedId.OrganizationId ORGANISATION_ID_1 = new QualifiedId.OrganizationId("");
    private static final QualifiedId.OrganizationId ORGANISATION_ID_2 = new QualifiedId.OrganizationId("");
    private static final CPR CPR_1 = new CPR("0101010101");
    private static final CPR CPR_2 = new CPR("0202020202");
    private static final QualifiedId.OrganizationId ORGANIZATION_ID_1 = new QualifiedId.OrganizationId("Infektionsmedicinsk");
    private static final QualifiedId.CarePlanId CAREPLAN_ID_1 = new QualifiedId.CarePlanId("careplan-1");
    private static final QualifiedId.CarePlanId CAREPLAN_ID_2 = new QualifiedId.CarePlanId("careplan-2");
    private static final QualifiedId.PatientId PATIENT_ID_1 = new QualifiedId.PatientId("patient-1");
    private static final QualifiedId.PatientId PATIENT_ID_2 = new QualifiedId.PatientId("patient-2");
    private static final QualifiedId.PlanDefinitionId PLANDEFINITION_ID_1 = new QualifiedId.PlanDefinitionId("plandefinition-1");
    private static final QualifiedId.PlanDefinitionId PLANDEFINITION_ID_2 = new QualifiedId.PlanDefinitionId("plandefinition-2");
    private static final QualifiedId.QuestionnaireId QUESTIONNAIRE_ID_1 = new QualifiedId.QuestionnaireId("questionnaire-1");
    private static final QualifiedId.QuestionnaireId QUESTIONNAIRE_ID_2 = new QualifiedId.QuestionnaireId("questionnaire-2");
    private static final Instant POINT_IN_TIME = Instant.parse("2021-11-23T10:00:00.000Z");

    @InjectMocks
    private ConcreteCarePlanService subject;

    @Mock
    private CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository;
    @Mock
    private PatientRepository<PatientModel, CarePlanStatus> patientRepository;
    @Mock
    private QuestionnaireRepository<QuestionnaireModel> questionnaireRepository;
    @Mock
    private PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository;
    @Mock
    private QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository;
    @Mock
    private OrganizationRepository<Organization> organizationRepository;


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
            boolean onlyActiveCarePlans = true;

            Mockito.when(patientRepository.fetch(CPR_1)).thenReturn(Optional.empty());
            Mockito.when(carePlanRepository.fetchCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(List.of());
            Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));
            Mockito.when(carePlanRepository.save(Mockito.any(CarePlanModel.class))).thenReturn(new QualifiedId.CarePlanId("1"));

            QualifiedId.CarePlanId result = subject.createCarePlan(carePlanModel);
            fail("No error was thrown");
        } catch (ServiceException e) {
            assertEquals(ErrorDetails.CUSTOMLOGIN_UNKNOWN_ERROR, e.getErrorDetails());
            assertEquals(ErrorKind.BAD_GATEWAY, e.getErrorKind());
        }
    }

    @Test
    public void createCarePlan_patientDoesNotExist_patientIsCreated() throws Exception {
        PatientModel patient = PatientModel.builder().cpr(CPR_1).build();
        CarePlanModel carePlan = CarePlanModel.builder()
                .patient(patient)
                .build();

        Mockito.when(patientRepository.fetch(CPR_1)).thenReturn(Optional.empty());
        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

        subject.createCarePlan(carePlan);
        Mockito.verify(carePlanRepository).save(carePlan, patient);
    }

    @Test
    public void createCarePlan_activePlanExists_throwsException() throws Exception {
        CarePlanModel carePlanModel = buildCarePlanModel();
        PatientModel patient = PatientModel.builder()
                .id(PATIENT_ID_1)
                .build();

        CarePlanModel existingCareplan = CarePlanModel.builder().build();

        boolean onlyActiveCarePlans = true;

        Mockito.when(patientRepository.fetch(CPR_1)).thenReturn(Optional.of(patient));
        Mockito.when(carePlanRepository.fetchCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(List.of(existingCareplan));

        assertThrows(ServiceException.class, () -> subject.createCarePlan(carePlanModel));
    }

    @Test
    public void createCarePlan_questionnaireAccessViolation_throwsException() throws Exception {
        CarePlanModel carePlanModel = buildCarePlanModel(List.of(), List.of(QUESTIONNAIRE_ID_1));
        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();
        Mockito.when(patientRepository.fetch(CPR_1)).thenReturn(Optional.empty());
        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(questionnaire));
        assertThrows(AccessValidationException.class, () -> subject.createCarePlan(carePlanModel));
    }

    /*
    @Test
    public void createCarePlan_success() throws Exception {
        CarePlanModel carePlanModel = buildCarePlanModel();

        CarePlanModel carePlan = CarePlanModel.builder().build();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        PatientModel patient = PatientModel.builder().build();
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

        CarePlanModel carePlan = CarePlanModel.builder().build();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        PatientModel patient = PatientModel.builder().build();
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

        CarePlanModel carePlan = CarePlanModel.builder().build();
        Mockito.when(fhirMapper.mapCarePlanModel(carePlanModel)).thenReturn(carePlan);

        PatientModel patient = PatientModel.builder().build();
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
        PlanDefinitionModel planDefinition = buildPlanDefinition();
        var wrapper = carePlanModel.questionnaires().getFirst();
        var expectedPointInTime = new FrequencyEnumerator(wrapper.frequency()).getSatisfiedUntilForInitialization(dateProvider.now());

        Mockito.when(patientRepository.fetch(CPR_1)).thenReturn(Optional.empty());
        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(buildQuestionnaire()));
        Mockito.when(planDefinitionRepository.fetch(List.of(PLANDEFINITION_ID_1))).thenReturn(List.of(planDefinition));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

        subject.createCarePlan(carePlanModel);

        assertEquals(expectedPointInTime, wrapper.satisfiedUntil());
        assertEquals(expectedPointInTime, carePlanModel.satisfiedUntil());
    }


    @Test
    public void getCarePlanByCpr_carePlansPresent_returnsCarePlans() throws Exception {
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        boolean onlyActiveCarePlans = true;
        boolean onlyUnSatisfied = false;
        CarePlanModel carePlanModel = CarePlanModel.builder().build();
        var unsatisfiedAt = Instant.now();

        Mockito.when(dateProvider.now()).thenReturn(unsatisfiedAt);
        Mockito.when(carePlanRepository.fetch(CPR_1, unsatisfiedAt, onlyActiveCarePlans, onlyUnSatisfied)).thenReturn(List.of(carePlan));

        List<CarePlanModel> result = subject.getCarePlansWithFilters(CPR_1, onlyActiveCarePlans, onlyUnSatisfied, new Pagination(1, 10));

        assertEquals(1, result.size());
        assertEquals(carePlanModel, result.getFirst());
    }

    @Test
    public void getCarePlanByCpr_carePlansMissing_returnsEmptyList() throws Exception {
        boolean onlyActiveCarePlans = true;
        boolean onlyUnSatisfied = false;
        var unsatisfiedAt = Instant.now();

        Mockito.when(dateProvider.now()).thenReturn(unsatisfiedAt);
        Mockito.when(carePlanRepository.fetch(CPR_1, unsatisfiedAt, onlyActiveCarePlans, onlyUnSatisfied)).thenReturn(List.of());

        List<CarePlanModel> result = subject.getCarePlansWithFilters(CPR_1, onlyActiveCarePlans, onlyUnSatisfied, new Pagination(1, 10));

        assertEquals(0, result.size());
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_carePlansPresent_returnsCarePlans() throws Exception {
        boolean onlyActiveCarePlans = true;
        boolean unsatisfied = true;
        int pageNumber = 1;
        int pageSize = 4;

        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        CarePlanModel carePlanModel = CarePlanModel.builder().build();

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(carePlanRepository.fetch(POINT_IN_TIME, onlyActiveCarePlans, unsatisfied)).thenReturn(List.of(carePlan));

        List<CarePlanModel> result = subject.getCarePlansWithFilters(onlyActiveCarePlans, unsatisfied, new Pagination(pageNumber, pageSize));

        assertEquals(1, result.size());
        assertEquals(carePlanModel, result.getFirst());
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_carePlansMissing_returnsEmptyList() throws Exception {
        boolean onlyActiveCarePlans = true;
        boolean unsatisfied = true;

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(carePlanRepository.fetch(POINT_IN_TIME, onlyActiveCarePlans, unsatisfied)).thenReturn(List.of());

        List<CarePlanModel> result = subject.getCarePlansWithFilters(onlyActiveCarePlans, unsatisfied, new Pagination(1, 4));

        assertEquals(0, result.size());
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_translatesPagingParameters() throws Exception {
        boolean onlyActiveCarePlans = true;
        boolean unsatisfied = true;

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(carePlanRepository.fetch(POINT_IN_TIME, onlyActiveCarePlans, unsatisfied)).thenReturn(List.of());

        subject.getCarePlansWithFilters(onlyActiveCarePlans, unsatisfied, new Pagination(3, 4));
    }

    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void getCarePlansWithUnsatisfiedSchedules_sortCareplans_byPatientName(HumanName name1, HumanName name2, List<HumanName> expectedOrder) throws Exception {
        boolean onlyActiveCarePlans = true;
        boolean unsatisfied = true;
        CarePlanModel carePlan1 = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        CarePlanModel carePlan2 = buildCarePlan(CAREPLAN_ID_2, PATIENT_ID_2);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(carePlanRepository.fetch(POINT_IN_TIME, onlyActiveCarePlans, unsatisfied)).thenReturn(List.of(carePlan1, carePlan2));

        List<CarePlanModel> result = subject.getCarePlansWithFilters(onlyActiveCarePlans, unsatisfied);

        assertEquals(2, result.size());
        assertEquals(expectedOrder.get(0).getGivenAsSingleString(), result.get(0).patient().name().given().getFirst());
        assertEquals(expectedOrder.get(0).getFamily(), result.get(0).patient().name().family());
        assertEquals(expectedOrder.get(1).getGivenAsSingleString(), result.get(1).patient().name().given().getFirst());
        assertEquals(expectedOrder.get(1).getFamily(), result.get(1).patient().name().family());
    }

    @Test
    public void getCarePlanById_carePlanPresent_returnsCarePlan() throws Exception {
        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
        CarePlanModel carePlanModel = CarePlanModel.builder().build();
        Optional<CarePlanModel> result = subject.getCarePlanById(carePlanId);
        assertEquals(carePlanModel, result.get());
    }

    @Test
    public void getCarePlanById_carePlanForDifferentOrganization_throwsException() throws Exception {
        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
        QualifiedId.PatientId patientId = new QualifiedId.PatientId("patient-1");
        CarePlanModel carePlan = buildCarePlan(carePlanId, patientId);
        Mockito.when(carePlanRepository.fetch(carePlan.id())).thenReturn(Optional.of(carePlan));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);
        assertThrows(AccessValidationException.class, () -> subject.getCarePlanById(carePlanId));
    }

    @Test
    public void getCarePlanById_carePlanMissing_returnsEmpty() throws Exception {
        QualifiedId.CarePlanId carePlanId = CAREPLAN_ID_1;
        Mockito.when(carePlanRepository.fetch(carePlanId)).thenReturn(Optional.empty());
        Optional<CarePlanModel> result = subject.getCarePlanById(carePlanId);
        assertFalse(result.isPresent());
    }

    @Test
    public void resolveAlarm_carePlanMissing_throwsException() throws ServiceException {
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.empty());
        assertThrows(ServiceException.class, () -> subject.resolveAlarm(CAREPLAN_ID_1, QUESTIONNAIRE_ID_1));
    }

    @Test
    public void resolveAlarm_accessViolation_throwsException() throws Exception {
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);
        assertThrows(AccessValidationException.class, () -> subject.resolveAlarm(CAREPLAN_ID_1, QUESTIONNAIRE_ID_1));
    }

    @Test
    public void resolveAlarm_carePlanSatisfiedIntoTheFuture_throwsException() throws Exception {
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        assertThrows(ServiceException.class, () -> subject.resolveAlarm(CAREPLAN_ID_1, QUESTIONNAIRE_ID_1));
    }

    @Test
    public void resolveAlarm_recomputesSatisfiedUntil_savesCarePlan() throws Exception {
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        CarePlanModel carePlanModel = CarePlanModel.builder().satisfiedUntil(POINT_IN_TIME.minusSeconds(100))
                .questionnaires(List.of(
                        buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1, POINT_IN_TIME.minusSeconds(100)),
                        buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2, POINT_IN_TIME.plusSeconds(100))
                )).build();

        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        subject.resolveAlarm(CAREPLAN_ID_1, QUESTIONNAIRE_ID_1);

        // Verify that the first questionnaire has its satisfied-timestamp pushed to the next scheduled weekday,
        // the second questionnaire has its timestamp left untouched, and the careplan has its timestamp set to
        // the earliest timestamp (now that of the second questionnaire).
        assertEquals(Instant.parse("2021-11-30T03:00:00.000Z"), carePlanModel.questionnaires().get(0).satisfiedUntil());
        assertEquals(POINT_IN_TIME.plusSeconds(100), carePlanModel.questionnaires().get(1).satisfiedUntil());
        assertEquals(POINT_IN_TIME.plusSeconds(100), carePlanModel.satisfiedUntil());

        Mockito.verify(carePlanRepository).update(carePlan);
    }

    @Test
    public void completeCareplan_NoQuestionnaireResponses_and_satisfiedSchedule_ShouldBeCompleted() throws ServiceException {
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);

        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), CAREPLAN_ID_1)).thenReturn(List.of());

        assertDoesNotThrow(() -> subject.completeCarePlan(CAREPLAN_ID_1));
    }

    @Test
    public void completeCareplan_OneQuestionnaireResponses_ShouldThrowError() throws ServiceException {
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);

        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), CAREPLAN_ID_1)).thenReturn(List.of(QuestionnaireResponseModel.builder().build()));

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
        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        Map<String, FrequencyModel> frequencies = Map.of();
        PatientDetails patientDetails = buildPatientDetails();

        PlanDefinitionModel planDefinition = PlanDefinitionModel.builder().build();
        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();

        Mockito.when(planDefinitionRepository.fetch(planDefinitionIds)).thenReturn(List.of(planDefinition));
        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(List.of(questionnaire));
        Mockito.doNothing().when(accessValidator).validateAccess(List.of(planDefinition));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(questionnaire));

        assertThrows(AccessValidationException.class, () -> subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails));
    }

    @Test
    public void updateCarePlan_carePlanAccessViolation_throwsException() throws Exception {
        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of();
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of();
        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of();
        PatientDetails patientDetails = buildPatientDetails();

        Mockito.when(planDefinitionRepository.fetch(planDefinitionIds)).thenReturn(List.of());
        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(List.of());

        CarePlanModel carePlan = CarePlanModel.builder()
                .id(carePlanId)
                .build();

        Mockito.when(carePlanRepository.fetch(carePlanId)).thenReturn(Optional.of(carePlan));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);

        assertThrows(AccessValidationException.class, () -> subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails));
    }

    // TODO: May be moved since it depends on fhir resources and not only models
    @Test
    public void updateCarePlan_transfersThresholdsFromPlanDefinition() throws Exception {
        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.MON), "07:00"));
        PatientDetails patientDetails = buildPatientDetails();
        PatientModel patientModel = buildPatientModel();
        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();
        var threshold = new ThresholdModel(null, null, null, null, null, null);
        PlanDefinitionModel planDefinition = PlanDefinitionModel.builder().build();
        CarePlanModel carePlanModel = buildCarePlanModel(planDefinitionIds, questionnaireIds, patientModel);

        Mockito.when(planDefinitionRepository.fetch(planDefinitionIds)).thenReturn(List.of(planDefinition));
        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(List.of(questionnaire));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId)).thenReturn(List.of());

        subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails);

        assertEquals(1, carePlanModel.questionnaires().size());
        assertEquals(threshold, carePlanModel.questionnaires().getFirst().thresholds().getFirst());
    }

    @Test
    public void updateCarePlan_updatesPatientDetails() throws Exception {
        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.MON), "07:00"));
        PatientDetails patientDetails = buildPatientDetails();

        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        PatientModel patientModel = buildPatientModel();

        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(List.of(questionnaire));
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId)).thenReturn(List.of());

        subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails);

        assertEquals(patientDetails.patientPrimaryPhone(), patientModel.contactDetails().primaryPhone());
        assertEquals(patientDetails.patientSecondaryPhone(), patientModel.contactDetails().secondaryPhone());
        assertEquals(patientDetails.primaryRelativeName(), patientModel.primaryContact().name());

    }

    @Test
    public void updateCarePlan_updatesFrequency_keepsMaxOf_currentSatistiedUntil_and_newCalculated() throws Exception {
        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        PatientDetails patientDetails = buildPatientDetails();
        PatientModel patientModel = PatientModel
                .builder()
                .id(CarePlanServiceTest.PATIENT_ID_1)
                .contactDetails(this.buildContactDetails())
                .primaryContact(PrimaryContactModel.builder().organisation(new QualifiedId.OrganizationId("infektionsmedicinsk")).build())
                .build();

        CarePlanModel carePlanModel = buildCarePlanModel(CAREPLAN_ID_1, planDefinitionIds, questionnaireIds, patientModel);
        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.MON, Weekday.TUE), "12:00"));
        FrequencyEnumerator fe = new FrequencyEnumerator(frequencies.get(QUESTIONNAIRE_ID_1));
        Instant nextNextSatisfiedUntilTime = fe.getSatisfiedUntilForFrequencyChange(POINT_IN_TIME);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        Mockito.lenient().when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId)).thenReturn(List.of());
        Mockito.lenient().when(questionnaireResponseRepository.fetch(carePlanModel.id(), List.of(new QualifiedId.QuestionnaireId("questionnaire-1")))).thenReturn(List.of());

        subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails);

        assertEquals(nextNextSatisfiedUntilTime, carePlanModel.questionnaires().getFirst().satisfiedUntil());
        assertEquals(nextNextSatisfiedUntilTime, carePlanModel.satisfiedUntil());
    }


    @Test
    public void updateCarePlan_updatesFrequency_multipleDays_keepsSatisfiedUntil() throws Exception {
        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        // Vi opdaterer frekvens til at indeholde torsdag, men ønsker stadig at beholde nuværende satisfiedUntil for at den blå alarm ikke 'forsvinder' indtil deadline kl. 11
        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.TUE, Weekday.THU), "11:00"));
        FrequencyEnumerator fe = new FrequencyEnumerator(frequencies.get(QUESTIONNAIRE_ID_1));
        Instant nextNextSatisfiedUntilTime = fe.getSatisfiedUntilForFrequencyChange(POINT_IN_TIME);
        PatientModel patientModel = buildPatientModel();
        CarePlanModel carePlanModel = buildCarePlanModel(planDefinitionIds, questionnaireIds, patientModel);
        PatientDetails patientDetails = buildPatientDetails();
        PlanDefinitionModel planDefinition = PlanDefinitionModel.builder().build();

        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId)).thenReturn(List.of());
        Mockito.when(planDefinitionRepository.fetch(planDefinitionIds)).thenReturn(List.of(planDefinition));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME.minus(1, ChronoUnit.HOURS).plus(2, ChronoUnit.DAYS));

        subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails);

        assertEquals(nextNextSatisfiedUntilTime, carePlanModel.questionnaires().getFirst().satisfiedUntil());
        assertEquals(nextNextSatisfiedUntilTime, carePlanModel.satisfiedUntil());
    }


    @Test
    void updateCarePlan_removeQuestionnaire_withExeededDeadline_throwsError() throws Exception {
        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of(PLANDEFINITION_ID_2);
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_2);
        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_2, buildFrequencyModel(List.of(Weekday.MON), "07:00"));
        PatientDetails patientDetails = buildPatientDetails();
        PlanDefinitionModel planDefinition = PlanDefinitionModel.builder().build();
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);

        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();
        Mockito.when(planDefinitionRepository.fetch(planDefinitionIds)).thenReturn(List.of(planDefinition));
        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(List.of(questionnaire));
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME.plusSeconds(1L));

        try {
            subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails);
            fail("No error was thrown");
        } catch (ServiceException e) {
            assertEquals(ErrorDetails.PLANDEFINITION_CONTAINS_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES, e.getErrorDetails());
        }
    }

    // TODO: May be moved since it depends on fhir resources and not only models
    @Test
    void updateCarePlan_removeQuestionnaire_withUnansweredQuestionnaireResponses_throwsError() throws Exception {
        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of(PLANDEFINITION_ID_2);
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_2);
        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_2, buildFrequencyModel(List.of(Weekday.MON), "07:00"));
        PatientDetails patientDetails = buildPatientDetails();
        PlanDefinitionModel planDefinition = PlanDefinitionModel.builder().build();
        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);

        Mockito.when(planDefinitionRepository.fetch(planDefinitionIds)).thenReturn(List.of(planDefinition));
        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(List.of(questionnaire));
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        questionnaireResponse.setQuestionnaire(QUESTIONNAIRE_ID_1); // unanswered response for questionnaire that is removed
//        Mockito.when(fhirClient.lookupQuestionnaireResponsesByStatusAndCarePlanId(List.of(ExaminationStatus.NOT_EXAMINED), carePlanId)).thenReturn(FhirLookupResult.fromResources(questionnaireResponse));

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

    private CarePlanModel buildCarePlanModel(List<QualifiedId.PlanDefinitionId> planDefinitionIds, List<QualifiedId.QuestionnaireId> questionnaireIds) {
        return buildCarePlanModel(null, planDefinitionIds, questionnaireIds, null);
    }

    private CarePlanModel buildCarePlanModel(List<QualifiedId.PlanDefinitionId> planDefinitionIds, List<QualifiedId.QuestionnaireId> questionnaireIds, PatientModel patient) {
        return buildCarePlanModel(null, planDefinitionIds, questionnaireIds, patient);

    }

    private CarePlanModel buildCarePlanModel(QualifiedId.CarePlanId id, List<QualifiedId.PlanDefinitionId> planDefinitionIds, List<QualifiedId.QuestionnaireId> questionnaireIds, PatientModel patient) {
        return CarePlanModel.builder()
                .id(id)
                .patient(patient)
                .planDefinitions(List.of())
                .planDefinitions(Optional.ofNullable(planDefinitionIds).map(x1 -> x1.stream().map(this::buildPlanDefinitionModel).toList()).orElse(null))
                .questionnaires(questionnaireIds != null ? questionnaireIds.stream().map(this::buildQuestionnaireWrapperModel).toList() : List.of())
                .build();
    }


    private PatientModel buildPatientModel() {
        return PatientModel
                .builder()
                .id(CarePlanServiceTest.PATIENT_ID_1)
                .contactDetails(this.buildContactDetails())
                .primaryContact(this.buildPrimaryContact())
                .build();
    }

    private PrimaryContactModel buildPrimaryContact() {
        return PrimaryContactModel.builder()
                .name("Poul")
                .organisation(new QualifiedId.OrganizationId("infektionsmedicinsk"))
                .affiliation("Onkel")
                .build();
    }

    private ContactDetailsModel buildContactDetails() {
        return ContactDetailsModel.builder().build();
    }

    private PatientModel buildPatientModel(QualifiedId.PatientId patientId, String givenName, String familyName) {
        return PatientModel.builder()
                .id(patientId)
                .contactDetails(ContactDetailsModel.builder().build())
                .name(new PersonNameModel(familyName, List.of(givenName)))
                .build();
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel(QualifiedId.QuestionnaireId questionnaireId) {
        return buildQuestionnaireWrapperModel(questionnaireId, POINT_IN_TIME);
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel(QualifiedId.QuestionnaireId questionnaireId, Instant satisfiedUntil) {
        return QuestionnaireWrapperModel.builder()
                .questionnaire(QuestionnaireModel.builder()
                        .id(questionnaireId)
                        .build())
                .frequency(FrequencyModel.builder()
                        .weekdays(List.of(Weekday.TUE))
                        .timeOfDay(LocalTime.parse("04:00")).build())
                .satisfiedUntil(satisfiedUntil).build();
    }

    private PlanDefinitionModel buildPlanDefinitionModel(QualifiedId.PlanDefinitionId planDefinitionId) {
        return PlanDefinitionModel.builder().id(planDefinitionId).build();
    }


    private CarePlanModel buildCarePlan(QualifiedId.CarePlanId carePlanId, QualifiedId.PatientId patientId) {
        return buildCarePlan(carePlanId, patientId, null);
    }

    private CarePlanModel buildCarePlan(QualifiedId.CarePlanId carePlanId, QualifiedId.PatientId patientId, QualifiedId.QuestionnaireId questionnaireId) {
        CarePlanModel.Builder builder = CarePlanModel.builder()
                .id(carePlanId)
                .patient(PatientModel.builder().id(patientId).build());


        if (questionnaireId != null) {
            CarePlan.CarePlanActivityDetailComponent detail = new CarePlan.CarePlanActivityDetailComponent();
            detail.setInstantiatesCanonical(List.of(new CanonicalType(questionnaireId.unqualified())));
            detail.setScheduled(new Timing());
            detail.addExtension(ExtensionMapper.mapActivitySatisfiedUntil(POINT_IN_TIME));
//            carePlan.addActivity().setDetail(detail);
//            carePlan.addExtension(ExtensionMapper.mapCarePlanSatisfiedUntil(POINT_IN_TIME));
        }

        return builder.build();
    }

    private FrequencyModel buildFrequencyModel(List<Weekday> weekdays, String timeOfDay) {
        return FrequencyModel.builder()
                .weekdays(weekdays)
                .timeOfDay(LocalTime.parse(timeOfDay)).build();
    }

    private PatientModel buildPatient(QualifiedId.PatientId patientId, CPR cpr) {

        return PatientModel.builder()
                .id(patientId)
                .name(PersonNameModel.builder().given(List.of("Yvonne")).build())
                .primaryContact(PrimaryContactModel.builder()
                        .name("Yvonne")
                        .affiliation("Moster")
                        .organisation(new QualifiedId.OrganizationId("infektionsmedicinsk"))
                        .build())
                .build();


//        var identifier = new Identifier();
//        identifier.setSystem(Systems.CPR);
//        identifier.setValue(cpr);
//        patient.setIdentifier(List.of(identifier));
//        var contact = new Patient.ContactComponent();
//
//        var name = new HumanName();
//        name.setGiven(List.of(new StringType("Yvonne")));
//        contact.setName(name);
//
//        var affiliation = List.of(new CodeableConcept().setText("Moster"));
//        contact.setRelationship(affiliation);
//        contact.setOrganization(buildReference());
//
//        patient.setContact(new ArrayList<>(List.of(contact)));
//        return patient;
    }



    private PatientModel buildPatient(QualifiedId.PatientId patientId, CPR cpr, String givenName, String familyName) {
        PatientModel patient = buildPatient(patientId, cpr);

        return PatientModel.Builder.from(patient)
                .name(PersonNameModel.builder()
                        .given(List.of(givenName))
                        .family(familyName)
                        .build()
                )
                .build();
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

    private PlanDefinitionModel buildPlanDefinition() {
        return PlanDefinitionModel.builder()
                .id(CarePlanServiceTest.PLANDEFINITION_ID_1)
                .build();

    }

    private PlanDefinitionModel buildPlanDefinitionModel(QualifiedId.QuestionnaireId questionnaireId, ThresholdModel questionnaireThreshold) {
        return PlanDefinitionModel.builder()
                .questionnaires(List.of(QuestionnaireWrapperModel
                        .builder()
                        .questionnaire(QuestionnaireModel.builder().id(questionnaireId).build())
                        .thresholds(List.of(questionnaireThreshold))
                        .build())).build();
    }

    private QuestionnaireModel buildQuestionnaire() {
        return QuestionnaireModel.builder()
                .id(CarePlanServiceTest.QUESTIONNAIRE_ID_1)
                .build();
    }


}