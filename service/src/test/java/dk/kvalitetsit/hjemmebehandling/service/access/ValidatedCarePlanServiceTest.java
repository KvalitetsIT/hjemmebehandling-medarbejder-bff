package dk.kvalitetsit.hjemmebehandling.service.access;

import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteCarePlanService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.fail;

class ValidatedCarePlanServiceTest {

    @InjectMocks
    private ValidatedQuestionnaireService subject;

    @Mock
    private ConcreteCarePlanService carePlanService;

    @Mock
    private AccessValidator accessValidator;

    @Test
    void createCarePlan() {
        fail("Test logic is expected");
    }

    @Test
    void completeCarePlan() {
        fail("Test logic is expected");
    }

    @Test
    void getCarePlansWithFilters() {
        fail("Test logic is expected");
    }

    @Test
    void testGetCarePlansWithFilters() {
        fail("Test logic is expected");
    }

    @Test
    void testGetCarePlansWithFilters1() {
        fail("Test logic is expected");
    }

    @Test
    void testGetCarePlansWithFilters2() {
        fail("Test logic is expected");
    }

    @Test
    void getCarePlanById() {
        fail("Test logic is expected");
    }

    @Test
    void resolveAlarm() {
        fail("Test logic is expected");
    }

    @Test
    void updateCarePlan() {
        fail("Test logic is expected");
    }

    @Test
    void getUnresolvedQuestionnaires() {
        fail("Test logic is expected");

    }

    @Test
    void getDefaultDeadlineTime() {
        fail("Test logic is expected");
    }



// TODO: THE CODE BELOW SHOULD MY INCLUDED
//
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