package dk.kvalitetsit.hjemmebehandling.service.concrete;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.*;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ErrorKind;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.service.frequency.FrequencyEnumerator;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteCarePlanService;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static dk.kvalitetsit.hjemmebehandling.MockFactory.*;
import static dk.kvalitetsit.hjemmebehandling.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class CarePlanServiceTest {


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
    private DateProvider dateProvider;
    @Mock
    private CustomUserClient customUserService;
    @Mock
    private OrganizationRepository<OrganizationModel> organizationRepository;


    private static Stream<Arguments> getCarePlansWithUnsatisfiedSchedules_sortCarePlans_byPatientName() {
        PersonNameModel a_a = new PersonNameModel("a", List.of("a"));
        PersonNameModel a_b = new PersonNameModel("b", List.of("a"));
        PersonNameModel b_a = new PersonNameModel("a", List.of("b"));
        PersonNameModel b_b = new PersonNameModel("b", List.of("b"));
        return Stream.of(
                //getPointInTime_initializedWithSeed
                Arguments.of(a_a, a_b, List.of(a_a, a_b)),
                Arguments.of(a_b, a_a, List.of(a_a, a_b)),
                Arguments.of(b_a, b_b, List.of(b_a, b_b)),
                Arguments.of(b_b, b_a, List.of(b_a, b_b))
        );
    }

    @Test
    public void createCarePlan_ThrowsBadGateway_WhenCustomloginFails() throws JsonProcessingException, AccessValidationException, ServiceException {
        ReflectionTestUtils.setField(subject, "patientidpApiUrl", "http://foo");

        CarePlanModel carePlanModel = CarePlanModel.builder()
                .patient(buildPatient(PATIENT_ID_1, CPR_1))
                .build();

        Mockito.when(customUserService.createUser(any())).thenThrow(JsonProcessingException.class);
        Mockito.when(patientRepository.fetch(CPR_1)).thenReturn(Optional.empty());
        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

        var e = assertThrows(ServiceException.class, () -> subject.createCarePlan(carePlanModel));

        assertEquals(ErrorDetails.CUSTOMLOGIN_UNKNOWN_ERROR, e.getErrorDetails());
        assertEquals(ErrorKind.BAD_GATEWAY, e.getErrorKind());
    }

    @Test
    public void createCarePlan_patientDoesNotExist_patientIsCreated() throws Exception {
        PatientModel patient = PatientModel.builder().cpr(CPR_1).build();
        CarePlanModel carePlan = CarePlanModel.builder()
                .startDate(POINT_IN_TIME)
                .created(POINT_IN_TIME)
                .status(CarePlanStatus.ACTIVE)
                .satisfiedUntil(Instant.MAX)
                .patient(patient)
                .build();

        Mockito.when(patientRepository.fetch(CPR_1)).thenReturn(Optional.empty());
        Mockito.when(dateProvider.today()).thenReturn(Date.from(POINT_IN_TIME));

        subject.createCarePlan(carePlan);
        Mockito.verify(carePlanRepository, Mockito.times(1)).save(carePlan, patient);
    }

    @Test
    public void createCarePlan_activePlanExists_throwsException() throws Exception {
        PatientModel patient = buildPatient(PATIENT_ID_1, CPR_1);

        CarePlanModel carePlanModel = CarePlanModel.builder()
                .patient(patient)
                .planDefinitions(List.of())
                .build();

        CarePlanModel existingCarePlan = CarePlanModel.builder().build();

        boolean onlyActiveCarePlans = true;

        Mockito.when(patientRepository.fetch(CPR_1)).thenReturn(Optional.of(patient));
        Mockito.when(carePlanRepository.fetchCarePlansByPatientId(PATIENT_ID_1, onlyActiveCarePlans)).thenReturn(List.of(existingCarePlan));

        assertThrows(ServiceException.class, () -> subject.createCarePlan(carePlanModel));
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
*/

    @Test
    public void getCarePlanByCpr_carePlansPresent_returnsCarePlans() throws Exception {
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        PatientModel patientModel = PatientModel.builder()
                .cpr(CPR_1)
                .id(PATIENT_ID_1)
                .build();
        boolean onlyActiveCarePlans = true;
        boolean onlyUnSatisfied = false;

        var unsatisfiedAt = Instant.now();

        Mockito.when(dateProvider.now()).thenReturn(unsatisfiedAt);
        Mockito.when(patientRepository.fetch(patientModel.cpr())).thenReturn(Optional.of(patientModel));
        Mockito.when(carePlanRepository.fetch(patientModel.id(), unsatisfiedAt, onlyActiveCarePlans, onlyUnSatisfied)).thenReturn(List.of(carePlan));

        List<CarePlanModel> result = subject.getCarePlansWithFilters(patientModel.cpr(), onlyActiveCarePlans, onlyUnSatisfied, PAGINATION);

        assertEquals(1, result.size());
        assertEquals(carePlan, result.getFirst());
    }

    @Test
    public void getCarePlanByCpr_carePlansMissing_returnsEmptyList() throws Exception {
        boolean onlyActiveCarePlans = true;
        boolean onlyUnSatisfied = false;
        var unsatisfiedAt = Instant.now();

        var patient = buildPatient(PATIENT_ID_1, CPR_1);

        Mockito.when(dateProvider.now()).thenReturn(unsatisfiedAt);
        Mockito.when(patientRepository.fetch(CPR_1)).thenReturn(Optional.of(patient));
        Mockito.when(carePlanRepository.fetch(patient.id(), unsatisfiedAt, onlyActiveCarePlans, onlyUnSatisfied)).thenReturn(List.of());

        List<CarePlanModel> result = subject.getCarePlansWithFilters(CPR_1, onlyActiveCarePlans, onlyUnSatisfied, PAGINATION);

        assertEquals(0, result.size());
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_carePlansPresent_returnsCarePlans() throws Exception {
        boolean onlyActiveCarePlans = true;
        boolean unsatisfied = true;

        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(carePlanRepository.fetch(POINT_IN_TIME, onlyActiveCarePlans, unsatisfied)).thenReturn(List.of(carePlan));

        List<CarePlanModel> result = subject.getCarePlansWithFilters(onlyActiveCarePlans, unsatisfied, new Pagination(0, 4));

        assertEquals(1, result.size());
        assertEquals(carePlan, result.getFirst());
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
    public void getCarePlansWithUnsatisfiedSchedules_sortCarePlans_byPatientName(PersonNameModel name1, PersonNameModel name2, List<PersonNameModel> expectedOrder) throws Exception {
        boolean onlyActiveCarePlans = true;
        boolean unsatisfied = true;
        CarePlanModel carePlan1 = CarePlanModel.builder()
                .id(CAREPLAN_ID_1)
                .patient(PatientModel.builder()
                        .id(PATIENT_ID_1)
                        .name(name1)
                        .build())
                .satisfiedUntil(POINT_IN_TIME)
                .build();

        CarePlanModel carePlan2 = CarePlanModel.builder()
                .id(CAREPLAN_ID_2)
                .patient(PatientModel.builder()
                        .id(PATIENT_ID_2)
                        .name(name2)
                        .build())
                .satisfiedUntil(POINT_IN_TIME)
                .build();


        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(carePlanRepository.fetch(POINT_IN_TIME, onlyActiveCarePlans, unsatisfied)).thenReturn(List.of(carePlan1, carePlan2));

        List<CarePlanModel> result = subject.getCarePlansWithFilters(onlyActiveCarePlans, unsatisfied);

        assertEquals(2, result.size());
        assertEquals(expectedOrder.get(0).given().getFirst(), result.get(0).patient().name().given().getFirst());
        assertEquals(expectedOrder.get(0).family(), result.get(0).patient().name().family());
        assertEquals(expectedOrder.get(1).given().getFirst(), result.get(1).patient().name().given().getFirst());
        assertEquals(expectedOrder.get(1).family(), result.get(1).patient().name().family());
    }

    @Test
    public void getCarePlanById_carePlanPresent_returnsCarePlan() throws Exception {
        CarePlanModel carePlanModel = CarePlanModel.builder().build();
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlanModel));
        Optional<CarePlanModel> result = subject.getCarePlanById(CAREPLAN_ID_1);
        assertEquals(carePlanModel, result.get());
    }


    @Test
    public void getCarePlanById_carePlanMissing_returnsEmpty() throws Exception {
        QualifiedId.CarePlanId carePlanId = CAREPLAN_ID_1;
        Mockito.when(carePlanRepository.fetch(carePlanId)).thenReturn(Optional.empty());
        Optional<CarePlanModel> result = subject.getCarePlanById(carePlanId);
        assertFalse(result.isPresent());
    }

    @Test
    public void resolveAlarm_carePlanMissing_throwsException() throws ServiceException, AccessValidationException {
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.empty());
        assertThrows(ServiceException.class, () -> subject.resolveAlarm(CAREPLAN_ID_1, QUESTIONNAIRE_ID_1));
    }


    @Test
    public void resolveAlarm_carePlanSatisfiedIntoTheFuture_throwsException() throws Exception {
        CarePlanModel carePlan = CarePlanModel.builder()
                .id(CAREPLAN_ID_1)
                .patient(PatientModel.builder().id(PATIENT_ID_1).build())
                .satisfiedUntil(POINT_IN_TIME.plusSeconds(200))
                .build();
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        assertThrows(ServiceException.class, () -> subject.resolveAlarm(CAREPLAN_ID_1, QUESTIONNAIRE_ID_1));
    }

    @Test
    public void resolveAlarm_recomputesSatisfiedUntil_savesCarePlan() throws Exception {
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);

        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);

        subject.resolveAlarm(CAREPLAN_ID_1, QUESTIONNAIRE_ID_1);

        Mockito.verify(carePlanRepository, Mockito.times(1)).update(carePlan);

        CarePlanModel carePlanModel = CarePlanModel.builder()
                .satisfiedUntil(POINT_IN_TIME.minusSeconds(100))
                .questionnaires(List.of(
                        buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1, POINT_IN_TIME.minusSeconds(100)),
                        buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_2, POINT_IN_TIME.plusSeconds(100))
                )).build();

        // Verify that the first questionnaire has its satisfied-timestamp pushed to the next scheduled weekday,
        // the second questionnaire has its timestamp left untouched, and the careplan has its timestamp set to
        // the earliest timestamp (now that of the second questionnaire).
        assertEquals(Instant.parse("2021-11-30T03:00:00.000Z"), carePlanModel.questionnaires().get(0).satisfiedUntil());
        assertEquals(POINT_IN_TIME.plusSeconds(100), carePlanModel.questionnaires().get(1).satisfiedUntil());
        assertEquals(POINT_IN_TIME.plusSeconds(100), carePlanModel.satisfiedUntil());
    }

    @Test
    public void completeCarePlan_NoQuestionnaireResponses_and_satisfiedSchedule_ShouldBeCompleted() throws ServiceException, AccessValidationException {
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), CAREPLAN_ID_1)).thenReturn(List.of());
        assertDoesNotThrow(() -> subject.completeCarePlan(CAREPLAN_ID_1));
    }

    @Test
    public void completeCarePlan_OneQuestionnaireResponses_ShouldThrowError() throws ServiceException, AccessValidationException {
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), CAREPLAN_ID_1)).thenReturn(List.of(QuestionnaireResponseModel.builder().build()));

        var e = assertThrows(ServiceException.class, () -> subject.completeCarePlan(CAREPLAN_ID_1));
        assertEquals(ErrorKind.BAD_REQUEST, e.getErrorKind());
        assertEquals(ErrorDetails.CAREPLAN_HAS_UNHANDLED_QUESTIONNAIRERESPONSES, e.getErrorDetails());

    }

    @Test
    public void completeCarePlan_unsatisfiedSchedule_ShouldThrowError() throws ServiceException, AccessValidationException {
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1)));
        try {
            subject.completeCarePlan(CAREPLAN_ID_1);
            fail("CarePlan should be failing due to questionnaireresponses on careplan");
        } catch (ServiceException serviceException) {
            assertEquals(ErrorKind.BAD_REQUEST, serviceException.getErrorKind());
            assertEquals(ErrorDetails.CAREPLAN_IS_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES, serviceException.getErrorDetails());
        }
    }


    @Test
    public void updateCarePlan_transfersThresholdsFromPlanDefinition() throws Exception {
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
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), CAREPLAN_ID_1)).thenReturn(List.of());

        subject.updateCarePlan(CAREPLAN_ID_1, planDefinitionIds, questionnaireIds, frequencies, patientDetails);

        assertEquals(1, carePlanModel.questionnaires().size());
        assertEquals(threshold, carePlanModel.questionnaires().getFirst().thresholds().getFirst());
    }

    @Test
    public void updateCarePlan_updatesPatientDetails() throws Exception {
        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.MON), "07:00"));
        PatientDetails patientDetails = buildPatientDetails();

        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        PatientModel patientModel = buildPatientModel();
        var threshold = new ThresholdModel(null, null, null, null, null, null);

        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel(QUESTIONNAIRE_ID_1, threshold);

        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(List.of(questionnaire));
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), CAREPLAN_ID_1)).thenReturn(List.of());
        Mockito.when(planDefinitionRepository.fetch(planDefinitionIds)).thenReturn(List.of(planDefinitionModel));
        Mockito.when(organizationRepository.getOrganizationId()).thenReturn(ORGANIZATION_ID_1);

        subject.updateCarePlan(CAREPLAN_ID_1, planDefinitionIds, questionnaireIds, frequencies, patientDetails);

        assertEquals(patientDetails.patientPrimaryPhone(), patientModel.contactDetails().primaryPhone());
        assertEquals(patientDetails.patientSecondaryPhone(), patientModel.contactDetails().secondaryPhone());
        assertEquals(patientDetails.primaryRelativeName(), patientModel.primaryContact().name());

    }

    @Test
    public void updateCarePlan_updatesFrequency_keepsMaxOf_currentSatistiedUntil_and_newCalculated() throws Exception {

        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();

        PatientDetails patientDetails = buildPatientDetails();
        PatientModel patientModel = PatientModel
                .builder()
                .id(PATIENT_ID_1)
                .contactDetails(buildContactDetails())
                .primaryContact(PrimaryContactModel.builder().organisation(new QualifiedId.OrganizationId("infektionsmedicinsk")).build())
                .build();

        CarePlanModel carePlanModel = buildCarePlanModel(CAREPLAN_ID_1, planDefinitionIds, questionnaireIds, patientModel);
        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.MON, Weekday.TUE), "12:00"));
        FrequencyEnumerator fe = new FrequencyEnumerator(frequencies.get(QUESTIONNAIRE_ID_1));
        Instant nextNextSatisfiedUntilTime = fe.getSatisfiedUntilForFrequencyChange(POINT_IN_TIME);

        PlanDefinitionModel planDefinition = buildPlanDefinition();

        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(planDefinitionRepository.fetch(planDefinitionIds)).thenReturn(List.of(planDefinition));
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), CAREPLAN_ID_1)).thenReturn(List.of());
        Mockito.when(questionnaireResponseRepository.fetch(carePlanModel.id(), List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of());
        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(List.of(questionnaire));
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlanModel));


        subject.updateCarePlan(CAREPLAN_ID_1, planDefinitionIds, questionnaireIds, frequencies, patientDetails);

        assertEquals(nextNextSatisfiedUntilTime, carePlanModel.questionnaires().getFirst().satisfiedUntil());
        assertEquals(nextNextSatisfiedUntilTime, carePlanModel.satisfiedUntil());
    }


    @Test
    public void updateCarePlan_updatesFrequency_multipleDays_keepsSatisfiedUntil() throws Exception {
        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
        List<QuestionnaireWrapperModel> questionnaires = List.of(buildQuestionnaireWrapperModel(QUESTIONNAIRE_ID_1));

        // Vi opdaterer frekvens til at indeholde torsdag, men ønsker stadig at beholde nuværende satisfiedUntil for at den blå alarm ikke 'forsvinder' indtil deadline kl. 11
        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_1, buildFrequencyModel(List.of(Weekday.TUE, Weekday.THU), "11:00"));
        FrequencyEnumerator fe = new FrequencyEnumerator(frequencies.get(QUESTIONNAIRE_ID_1));
        Instant nextNextSatisfiedUntilTime = fe.getSatisfiedUntilForFrequencyChange(POINT_IN_TIME);
        PatientModel patientModel = buildPatientModel();

        List<QualifiedId.QuestionnaireId> questionnaireIds = questionnaires.stream()
                .map(QuestionnaireWrapperModel::questionnaire)
                .map(QuestionnaireModel::id)
                .toList();

        CarePlanModel carePlanModel = buildCarePlanModel(planDefinitionIds, questionnaireIds, patientModel);
        PatientDetails patientDetails = buildPatientDetails();
        PlanDefinitionModel planDefinition = PlanDefinitionModel.builder()
                .questionnaires(questionnaires)
                .build();

        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(questionnaires.stream().map(QuestionnaireWrapperModel::questionnaire).toList());
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), CAREPLAN_ID_1)).thenReturn(List.of());
        Mockito.when(planDefinitionRepository.fetch(planDefinitionIds)).thenReturn(List.of(planDefinition));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME.minus(1, ChronoUnit.HOURS).plus(2, ChronoUnit.DAYS));
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlanModel));

        subject.updateCarePlan(CAREPLAN_ID_1, planDefinitionIds, questionnaireIds, frequencies, patientDetails);

        ArgumentCaptor<CarePlanModel> carePlanCaptor = ArgumentCaptor.forClass(CarePlanModel.class);
        ArgumentCaptor<PatientModel> patientCaptor = ArgumentCaptor.forClass(PatientModel.class);

        Mockito.verify(carePlanRepository, Mockito.times(1)).update(carePlanCaptor.capture(), patientCaptor.capture());

        assertEquals(nextNextSatisfiedUntilTime, carePlanCaptor.getValue().questionnaires().getFirst().satisfiedUntil());
        assertEquals(nextNextSatisfiedUntilTime, carePlanCaptor.getValue().satisfiedUntil());
    }


    @Test
    void updateCarePlan_removeQuestionnaire_withExeededDeadline_throwsError() throws Exception {

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

        var e = assertThrows(ServiceException.class, () -> subject.updateCarePlan(CAREPLAN_ID_1, planDefinitionIds, questionnaireIds, frequencies, patientDetails));
        assertEquals(ErrorDetails.PLANDEFINITION_CONTAINS_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES, e.getErrorDetails());
    }

    // TODO: May be moved since it depends on fhir resources and not only models
    @Test
    void updateCarePlan_removeQuestionnaire_withUnansweredQuestionnaireResponses_throwsError() throws Exception {

        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of(PLANDEFINITION_ID_2);
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_2);
        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of(QUESTIONNAIRE_ID_2, buildFrequencyModel(List.of(Weekday.MON), "07:00"));
        PatientDetails patientDetails = buildPatientDetails();
        PlanDefinitionModel planDefinition = buildPlanDefinitionModel(QUESTIONNAIRE_ID_2, ThresholdModel.builder().build());
        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();
        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);

        Mockito.when(planDefinitionRepository.fetch(planDefinitionIds)).thenReturn(List.of(planDefinition));
        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(List.of(questionnaire));
        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
        Mockito.when(dateProvider.now()).thenReturn(POINT_IN_TIME);
        Mockito.when(questionnaireResponseRepository.fetch(List.of(ExaminationStatus.NOT_EXAMINED), CAREPLAN_ID_1)).thenReturn(List.of(QuestionnaireResponseModel.builder().questionnaireId(QUESTIONNAIRE_ID_1).build()));

        var e = assertThrows(ServiceException.class, () -> subject.updateCarePlan(CAREPLAN_ID_1, planDefinitionIds, questionnaireIds, frequencies, patientDetails));
        assertEquals(ErrorDetails.PLANDEFINITION_CONTAINS_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES, e.getErrorDetails());

    }


    //    TODO: Tests below should be included again
    //    @Test
//    public void createCarePlan_questionnaireAccessViolation_throwsException() throws Exception {
//        CarePlanModel carePlanModel = buildCarePlanModel(List.of(), List.of(QUESTIONNAIRE_ID_1));
//        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();
//        Mockito.when(patientRepository.fetch(CPR_1)).thenReturn(Optional.empty());
//        Mockito.when(questionnaireRepository.fetch(List.of(QUESTIONNAIRE_ID_1))).thenReturn(List.of(questionnaire));
//        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(questionnaire));
//        assertThrows(AccessValidationException.class, () -> subject.createCarePlan(carePlanModel));
//    }
//
//    @Test
//    public void getCarePlanById_carePlanForDifferentOrganization_throwsException() throws Exception {
//        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
//        QualifiedId.PatientId patientId = new QualifiedId.PatientId("patient-1");
//        CarePlanModel carePlan = buildCarePlan(carePlanId, patientId);
//        Mockito.when(carePlanRepository.fetch(carePlan.id())).thenReturn(Optional.of(carePlan));
//        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);
//        assertThrows(AccessValidationException.class, () -> subject.getCarePlanById(carePlanId));
//    }
//
//    @Test
//    public void resolveAlarm_accessViolation_throwsException() throws Exception {
//        CarePlanModel carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1);
//        Mockito.when(carePlanRepository.fetch(CAREPLAN_ID_1)).thenReturn(Optional.of(carePlan));
//        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);
//        assertThrows(AccessValidationException.class, () -> subject.resolveAlarm(CAREPLAN_ID_1, QUESTIONNAIRE_ID_1));
//    }
//
//    @Test
//    public void updateCarePlan_questionnaireAccessViolation_throwsException() throws Exception {
//        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
//        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of(PLANDEFINITION_ID_1);
//        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
//        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of();
//        PatientDetails patientDetails = buildPatientDetails();
//
//        PlanDefinitionModel planDefinition = PlanDefinitionModel.builder().build();
//        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();
//
//        Mockito.when(planDefinitionRepository.fetch(planDefinitionIds)).thenReturn(List.of(planDefinition));
//        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(List.of(questionnaire));
//        Mockito.doNothing().when(accessValidator).validateAccess(List.of(planDefinition));
//        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(questionnaire));
//
//        assertThrows(AccessValidationException.class, () -> subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails));
//    }
//
//    @Test
//    public void updateCarePlan_carePlanAccessViolation_throwsException() throws Exception {
//        QualifiedId.CarePlanId carePlanId = new QualifiedId.CarePlanId("careplan-1");
//        List<QualifiedId.PlanDefinitionId> planDefinitionIds = List.of();
//        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of();
//        Map<QualifiedId.QuestionnaireId, FrequencyModel> frequencies = Map.of();
//        PatientDetails patientDetails = buildPatientDetails();
//
//        Mockito.when(planDefinitionRepository.fetch(planDefinitionIds)).thenReturn(List.of());
//        Mockito.when(questionnaireRepository.fetch(questionnaireIds)).thenReturn(List.of());
//
//        CarePlanModel carePlan = CarePlanModel.builder()
//                .id(carePlanId)
//                .build();
//
//        Mockito.when(carePlanRepository.fetch(carePlanId)).thenReturn(Optional.of(carePlan));
//        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(carePlan);
//
//        assertThrows(AccessValidationException.class, () -> subject.updateCarePlan(carePlanId, planDefinitionIds, questionnaireIds, frequencies, patientDetails));
//    }


}