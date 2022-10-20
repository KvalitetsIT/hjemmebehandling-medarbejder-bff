package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import jdk.jfr.Threshold;
import org.hamcrest.core.IsNull;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.matchers.NotNull;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class FhirMapperTest {
    @InjectMocks
    private FhirMapper subject;

    @Mock
    private DateProvider dateProvider;

    private static final String CPR_1 = "0101010101";

    private static final String CAREPLAN_ID_1 = "CarePlan/careplan-1";
    private static final String ORGANIZATION_ID_1 = "Organization/organization-1";
    private static final String PATIENT_ID_1 = "Patient/patient-1";
    private static final String PLANDEFINITION_ID_1 = "PlanDefinition/plandefinition-1";
    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";
    private static final String QUESTIONNAIRERESPONSE_ID_1 = "QuestionnaireResponse/questionnaireresponse-1";
    private static final String PRACTITIONER_ID_1 = "Practitioner/practitioner-1";

    private static final Instant POINT_IN_TIME = Instant.parse("2021-11-23T00:00:00.000Z");

    @Test
    public void mapCarePlanModel_mapsSubject() {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel();

        // Act
        CarePlan result = subject.mapCarePlanModel(carePlanModel);

        // Assert
        assertEquals(result.getSubject().getReference(), carePlanModel.getPatient().getId().toString());
    }

    @Test
    public void mapCarePlan_mapsPeriod() {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        Organization organization = buildOrganization(ORGANIZATION_ID_1);
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient, questionnaire, organization, planDefinition);

        // Act
        CarePlanModel result = subject.mapCarePlan(carePlan, lookupResult);

        // Assert
        assertEquals(result.getStartDate(), Instant.parse("2021-10-28T00:00:00Z"));
        assertEquals(result.getEndDate(), Instant.parse("2021-10-29T00:00:00Z"));
    }

    @Test
    public void mapCarePlan_roundtrip_preservesExtensions() {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        Organization organization = buildOrganization(ORGANIZATION_ID_1);
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient, questionnaire, organization, planDefinition);

        // Act
        CarePlan result = subject.mapCarePlanModel(subject.mapCarePlan(carePlan, lookupResult));

        // Assert
        assertEquals(carePlan.getExtension().size(), result.getExtension().size());
        assertTrue(result.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.ORGANIZATION)));
        assertTrue(result.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.CAREPLAN_SATISFIED_UNTIL)));
    }

    @Test
    public void mapCarePlan_roundtrip_preservesActivities() {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        Organization organization = buildOrganization(ORGANIZATION_ID_1);
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient, questionnaire, organization, planDefinition);

        // Act
        CarePlan result = subject.mapCarePlanModel(subject.mapCarePlan(carePlan, lookupResult));

        // Assert
        assertEquals(carePlan.getActivity().size(), result.getActivity().size());
        assertEquals(carePlan.getActivity().get(0).getDetail().getInstantiatesCanonical().get(0).getValue(), result.getActivity().get(0).getDetail().getInstantiatesCanonical().get(0).getValue());
        assertEquals(carePlan.getActivity().get(0).getDetail().getScheduledTiming().getRepeat().getDayOfWeek().get(0).getValue(), result.getActivity().get(0).getDetail().getScheduledTiming().getRepeat().getDayOfWeek().get(0).getValue());

        assertTrue(result.getActivity().get(0).getDetail().getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.ACTIVITY_SATISFIED_UNTIL)));
    }

        @Test
        public void mapCarePlan_includesQuestionnaires_andThresholds() {
            // Arrange
            CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
            Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
            Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);

            String linkId = "question-1";
            Questionnaire.QuestionnaireItemComponent itemComponent = buildQuestionItemWithThreshold(linkId);
            questionnaire.addItem(itemComponent);

            Organization organization = buildOrganization(ORGANIZATION_ID_1);
            PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);

            FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient, questionnaire, organization, planDefinition);

            // Act
            CarePlanModel result = subject.mapCarePlan(carePlan, lookupResult);

            // Assert
            assertEquals(1, result.getQuestionnaires().size());

            assertEquals(QUESTIONNAIRE_ID_1, result.getQuestionnaires().get(0).getQuestionnaire().getId().toString());
            var thresholdsOfFirstQuestionnaire =result.getQuestionnaires().get(0).getThresholds();
            assertEquals(4, thresholdsOfFirstQuestionnaire.size());
            assertEquals(2, thresholdsOfFirstQuestionnaire.stream().filter(q -> q.getQuestionnaireItemLinkId().equals(linkId)).collect(Collectors.toList()).size()); // from the questionnaire
            assertEquals(2, thresholdsOfFirstQuestionnaire.stream().filter(q -> q.getQuestionnaireItemLinkId().equals("1")).collect(Collectors.toList()).size()); // from buildPlanDefinition..
        }


        @Test
        public void mapPlandefinitionModel_includesOnlyQuestionnaires_andThresholds_1() {
            // Arrange
            ThresholdModel measurementThreshold = buildThresholdModel("linkId-1", ThresholdType.NORMAL, Double.valueOf("0.5"), Double.valueOf("1") );
            ThresholdModel booleanThreshold = buildThresholdModel("linkId-2", ThresholdType.NORMAL, Boolean.TRUE);

            PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel();
            QuestionnaireWrapperModel questionnaireWrapperModel = buildQuestionnaireWrapperModel();
            questionnaireWrapperModel.setThresholds(List.of(measurementThreshold, booleanThreshold));


            String linkId = "question-1";
            Questionnaire.QuestionnaireItemComponent questionnaireItemComponent = buildQuestionItemWithThreshold(linkId);
            Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(questionnaireItemComponent));
            //questionnaire.addExtension( ExtensionMapper.mapThreshold(booleanThreshold) );

            Organization organization = buildOrganization(ORGANIZATION_ID_1);
            PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);
            planDefinition.addExtension( ExtensionMapper.mapThreshold(measurementThreshold) );

            FhirLookupResult lookupResult = FhirLookupResult.fromResources(questionnaire, organization, planDefinition);

            // Act
            PlanDefinitionModel result = subject.mapPlanDefinition(planDefinition, lookupResult);

            // Assert
            assertEquals(1, result.getQuestionnaires().size());

            assertEquals(QUESTIONNAIRE_ID_1, result.getQuestionnaires().get(0).getQuestionnaire().getId().toString());

            //check that both thresholds are mapped on to the wrapper
            assertEquals(2, result.getQuestionnaires().get(0).getThresholds().size());
            assertEquals(0, result.getQuestionnaires().get(0).getThresholds().stream().filter(q -> q.getQuestionnaireItemLinkId().equals(linkId)).collect(Collectors.toList()).size()); // from the questionnaire
            assertEquals(2, result.getQuestionnaires().get(0).getThresholds().stream().filter(q -> q.getQuestionnaireItemLinkId().equals("1")).collect(Collectors.toList()).size()); // from buildPlanDefinition..
        }

        @Test
        public void mapPlandefinitionModel_includesQuestionnaires_andThresholds() {
            // Arrange
            ThresholdModel measurementThreshold = buildThresholdModel("linkId-1", ThresholdType.NORMAL, Double.valueOf("0.5"), Double.valueOf("1") );
            ThresholdModel booleanThreshold = buildThresholdModel("linkId-2", ThresholdType.NORMAL, Boolean.TRUE);

            PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel();
            QuestionnaireWrapperModel questionnaireWrapperModel = buildQuestionnaireWrapperModel();
            questionnaireWrapperModel.setThresholds(List.of(measurementThreshold, booleanThreshold));

            Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of());

            Organization organization = buildOrganization(ORGANIZATION_ID_1);
            PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);
            planDefinition.addExtension( ExtensionMapper.mapThreshold(measurementThreshold) );

            FhirLookupResult lookupResult = FhirLookupResult.fromResources(questionnaire, organization, planDefinition);

            // Act
            PlanDefinitionModel result = subject.mapPlanDefinition(planDefinition, lookupResult);

            // Assert
            assertEquals(1, result.getQuestionnaires().size());

            assertEquals(QUESTIONNAIRE_ID_1, result.getQuestionnaires().get(0).getQuestionnaire().getId().toString());

            assertEquals(2, result.getQuestionnaires().get(0).getThresholds().size());

            var firstThreshold = result.getQuestionnaires().get(0).getThresholds().get(0);

            assertNotNull(firstThreshold);
            assertEquals("1",firstThreshold.getQuestionnaireItemLinkId());
            assertEquals(null,firstThreshold.getValueQuantityHigh());
            assertEquals(null,firstThreshold.getValueQuantityLow());
            assertEquals(ThresholdType.NORMAL,firstThreshold.getType());
            assertEquals(Boolean.TRUE,firstThreshold.getValueBoolean());

            var secondThreshold = result.getQuestionnaires().get(0).getThresholds().get(1);
            assertNotNull(secondThreshold);
            assertEquals("1",secondThreshold.getQuestionnaireItemLinkId());
            assertEquals(null,secondThreshold.getValueBoolean());
            assertEquals(5.0,secondThreshold.getValueQuantityHigh());
            assertEquals(2.0,secondThreshold.getValueQuantityLow());
            assertEquals(ThresholdType.NORMAL,secondThreshold.getType());

        }

    private ThresholdModel buildThresholdModel(String linkId, ThresholdType type, Boolean valueBoolean) {
        return buildThresholdModel(linkId, type, valueBoolean, null, null);
    }

    private ThresholdModel buildThresholdModel(String linkId, ThresholdType type, Double valueLow, Double valueHigh) {
        return buildThresholdModel(linkId, type, null, valueLow, valueHigh);
    }

    private ThresholdModel buildThresholdModel(String linkId, ThresholdType type, Boolean valueBoolean, Double valueLow, Double valueHigh) {
        ThresholdModel thresholdModel = new ThresholdModel();

        thresholdModel.setQuestionnaireItemLinkId(linkId);
        thresholdModel.setType(type);
        thresholdModel.setValueBoolean(valueBoolean);
        thresholdModel.setValueQuantityLow(valueLow);
        thresholdModel.setValueQuantityHigh(valueHigh);

        return thresholdModel;
    }


    @Test
    public void mapPlandefinition_noCreatedDate_DontThrowError() {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        Organization organization = buildOrganization(ORGANIZATION_ID_1);
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient, questionnaire, organization, planDefinition);

        // Act
        planDefinition.setDate(null);
        PlanDefinitionModel result = subject.mapPlanDefinition(planDefinition, lookupResult);

        // Assert
        assertEquals(1, result.getQuestionnaires().size());

        assertEquals(QUESTIONNAIRE_ID_1, result.getQuestionnaires().get(0).getQuestionnaire().getId().toString());
        assertEquals(2, result.getQuestionnaires().get(0).getThresholds().size());
    }

    @Test
    public void mapTiming_allValuesAreNull_noErrors(){
        var timingToMap = new Timing();
        var result = subject.mapTiming(timingToMap);
        assertNotNull(result);
    }

    @Test
    public void mapPlandefinition() {
        // Arrange
        //CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
//        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        Organization organization = buildOrganization(ORGANIZATION_ID_1);
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(/*carePlan, patient,*/ questionnaire, organization, planDefinition);

        // Act
        PlanDefinitionModel result = subject.mapPlanDefinition(planDefinition, lookupResult);

        // Assert
        assertEquals(1, result.getQuestionnaires().size());
        assertEquals(QUESTIONNAIRE_ID_1, result.getQuestionnaires().get(0).getQuestionnaire().getId().toString());
        assertEquals(ORGANIZATION_ID_1, result.getOrganizationId());
        assertEquals(PlanDefinitionStatus.ACTIVE, result.getStatus());
        assertEquals(POINT_IN_TIME, result.getCreated());


    }

    @Test
    public void mapPatientModel_mapsCpr() {
        // Arrange
        PatientModel patientModel = buildPatientModel();

        // Act
        Patient result = subject.mapPatientModel(patientModel);

        // Assert
        assertEquals(1, result.getIdentifier().size());
        assertEquals(Systems.CPR, result.getIdentifier().get(0).getSystem());
        assertEquals("0101010101", result.getIdentifier().get(0).getValue());
    }

    @Test
    public void mapPatient_mapsCpr() {
        // Arrange
        Patient patient = new Patient();

        patient.setId(PATIENT_ID_1);

        Identifier identifier = new Identifier();
        identifier.setSystem(Systems.CPR);
        identifier.setValue("0101010101");
        patient.getIdentifier().add(identifier);

        // Act
        PatientModel result = subject.mapPatient(patient);

        // Assert
        assertEquals("0101010101", result.getCpr());
    }

    @Test
    public void mapPatient_roundtrip_preservesInformation() {
        // Arrange
        Patient patient = buildPatient(PATIENT_ID_1, CPR_1);

        // Act
        Patient result = subject.mapPatientModel(subject.mapPatient(patient));

        // Assert
        assertEquals(PATIENT_ID_1, result.getId());

        assertEquals(patient.getName().get(0).getFamily(), result.getName().get(0).getFamily());

        assertEquals(patient.getContact().get(0).getName().getText(), result.getContact().get(0).getName().getText());
        assertEquals(patient.getContact().get(0).getRelationshipFirstRep().getCodingFirstRep().getCode(), result.getContact().get(0).getRelationshipFirstRep().getCodingFirstRep().getCode());
        assertEquals(patient.getContact().get(0).getTelecomFirstRep().getValue(), result.getContact().get(0).getTelecomFirstRep().getValue());

        assertEquals(patient.getAddressFirstRep().getCity(), result.getAddressFirstRep().getCity());
    }

    @Test
    public void mapQuestionnaireResponseModel_mapsAnswers() {
        // Arrange
        QuestionnaireResponseModel model = buildQuestionnaireResponseModel();

        // Act
        QuestionnaireResponse result = subject.mapQuestionnaireResponseModel(model);

        // Assert
        assertEquals(1, result.getItem().size());
        assertEquals(new IntegerType(2).getValue(), result.getItem().get(0).getAnswer().get(0).getValueIntegerType().getValue());
    }

    @Test
    public void mapQuestionnaireResponseModel_mapsExaminationStatus() {
        // Arrange
        QuestionnaireResponseModel model = buildQuestionnaireResponseModel();

        // Act
        QuestionnaireResponse result = subject.mapQuestionnaireResponseModel(model);

        // Assert
        assertTrue(result.getExtension().stream().anyMatch(e ->
                e.getUrl().equals(Systems.EXAMINATION_STATUS) &&
                        e.getValue().toString().equals(new StringType(ExaminationStatus.NOT_EXAMINED.name()).toString())));
        assertTrue(result.getExtension().stream().noneMatch(e -> e.getUrl().equals(Systems.EXAMINATION_AUTHOR)));
    }

    @Test
    public void mapQuestionnaireResponse_canMapAnswers() {
        // Arrange
        QuestionnaireResponse questionnaireResponse = buildQuestionnaireResponse(QUESTIONNAIRERESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID_1, List.of(buildStringItem("hej", "1"), buildQuantityItem(2, "2")));
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(buildQuestionItem("1", Questionnaire.QuestionnaireItemType.STRING), buildQuestionItem("2", Questionnaire.QuestionnaireItemType.QUANTITY)));
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);

        // Act
        QuestionnaireResponseModel result = subject.mapQuestionnaireResponse(questionnaireResponse, FhirLookupResult.fromResources(questionnaireResponse, questionnaire, patient, carePlan, planDefinition));

        // Assert
        assertEquals(2, result.getQuestionAnswerPairs().size());
        assertEquals(AnswerType.STRING, result.getQuestionAnswerPairs().get(0).getAnswer().getAnswerType());
        assertEquals(AnswerType.QUANTITY, result.getQuestionAnswerPairs().get(1).getAnswer().getAnswerType());
    }

    @Test
    public void mapQuestionnaireResponse_roundtrip_preservesExtensions() {
        // Arrange
        QuestionnaireResponse questionnaireResponse = buildQuestionnaireResponse(QUESTIONNAIRERESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID_1, List.of(buildStringItem("hej", "1"), buildIntegerItem(2, "2")));
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(buildQuestionItem("1", Questionnaire.QuestionnaireItemType.STRING), buildQuestionItem("2", Questionnaire.QuestionnaireItemType.INTEGER)));
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);
        Practitioner practitioner = buildPractitioner(PRACTITIONER_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(patient, questionnaire, carePlan, planDefinition, practitioner);

        // Act
        QuestionnaireResponse result = subject.mapQuestionnaireResponseModel(subject.mapQuestionnaireResponse(questionnaireResponse, lookupResult));

        // Assert
        assertEquals(questionnaireResponse.getExtension().size(), result.getExtension().size());
        assertTrue(result.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.ORGANIZATION)));
        assertTrue(result.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.EXAMINATION_STATUS)));
        assertTrue(result.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.EXAMINATION_AUTHOR)));
        assertTrue(result.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.TRIAGING_CATEGORY)));
    }

    @Test
    public void mapQuestionnaireResponse_roundtrip_preservesReferences() {
        // Arrange
        QuestionnaireResponse questionnaireResponse = buildQuestionnaireResponse(QUESTIONNAIRERESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID_1, List.of(buildStringItem("hej", "1"), buildIntegerItem(2, "2")));
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(buildQuestionItem("1", Questionnaire.QuestionnaireItemType.STRING), buildQuestionItem("2", Questionnaire.QuestionnaireItemType.INTEGER)));
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);
        Practitioner practitioner = buildPractitioner(PRACTITIONER_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(patient, questionnaire, carePlan, planDefinition, practitioner);

        // Act
        QuestionnaireResponse result = subject.mapQuestionnaireResponseModel(subject.mapQuestionnaireResponse(questionnaireResponse, lookupResult));

        // Assert
        assertTrue(!questionnaireResponse.getBasedOn().isEmpty());
        assertEquals(questionnaireResponse.getBasedOn().size(), result.getBasedOn().size());
        assertEquals(questionnaireResponse.getBasedOn().get(0).getReference(), result.getBasedOn().get(0).getReference());

        assertEquals(questionnaireResponse.getAuthor().getReference(), result.getAuthor().getReference());

        assertEquals(questionnaireResponse.getSource().getReference(), result.getSource().getReference());
    }

    @Test
    public void mapQuestionnaireResponse_roundtrip_preservesLinks() {
        // Arrange
        var stringItem = buildStringItem("hej", "1");
        var integerItem = buildIntegerItem(2, "2");
        var quantityItem = buildQuantityItem(3.1, "3");

        QuestionnaireResponse questionnaireResponse = buildQuestionnaireResponse(QUESTIONNAIRERESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID_1, List.of(stringItem, integerItem, quantityItem));
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(buildQuestionItem("1", Questionnaire.QuestionnaireItemType.STRING), buildQuestionItem("2", Questionnaire.QuestionnaireItemType.INTEGER), buildQuestionItem("3", Questionnaire.QuestionnaireItemType.QUANTITY)));
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);
        Practitioner practitioner = buildPractitioner(PRACTITIONER_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(patient, questionnaire, carePlan, planDefinition, practitioner);

        // Act
        QuestionnaireResponse result = subject.mapQuestionnaireResponseModel(subject.mapQuestionnaireResponse(questionnaireResponse, lookupResult));

        // Assert
        assertEquals(questionnaireResponse.getItem().size(), result.getItem().size());

        assertEquals(stringItem.getLinkId(), result.getItem().get(0).getLinkId());
        assertEquals(stringItem.getAnswerFirstRep().getValueStringType().getValue(), result.getItem().get(0).getAnswerFirstRep().getValueStringType().getValue());

        assertEquals(integerItem.getLinkId(), result.getItem().get(1).getLinkId());
        assertEquals(integerItem.getAnswerFirstRep().getValueIntegerType().getValue(), result.getItem().get(1).getAnswerFirstRep().getValueIntegerType().getValue());

        assertEquals(quantityItem.getLinkId(), result.getItem().get(2).getLinkId());
        assertEquals(quantityItem.getAnswerFirstRep().getValueQuantity().getValue(), result.getItem().get(2).getAnswerFirstRep().getValueQuantity().getValue());
    }

    @Test
    public void mapQuestion_callToAction() {
        Questionnaire.QuestionnaireItemComponent question1 = buildQuestionItem("1", Questionnaire.QuestionnaireItemType.BOOLEAN, "Har du det godt?");

        Questionnaire.QuestionnaireItemComponent callToActionGroup = buildQuestionItem("call_to_action", Questionnaire.QuestionnaireItemType.GROUP);
        Questionnaire.QuestionnaireItemEnableWhenComponent enableWhen = question1.addEnableWhen().setQuestion("1").setOperator(Questionnaire.QuestionnaireItemOperator.EQUAL).setAnswer(new BooleanType(true));
        Questionnaire.QuestionnaireItemComponent callToAction = buildQuestionItem("call_to_action", Questionnaire.QuestionnaireItemType.DISPLAY, "Det var ikke så godt. Ring til afdelingen");
        callToAction.addEnableWhen(enableWhen);
        callToActionGroup.addItem(callToAction);

        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(question1, callToActionGroup));

        // Act
        QuestionnaireModel result = subject.mapQuestionnaire(questionnaire);

        // Assert
        assertEquals(1, result.getQuestions().size());
        assertTrue(result.getQuestions().stream().anyMatch(q -> q.getQuestionType().equals(QuestionType.BOOLEAN)));

        assertEquals(1, result.getCallToActions().size());
        assertTrue(result.getCallToActions().stream().allMatch(q -> q.getQuestionType().equals(QuestionType.DISPLAY)));
    }

    @Test
    public void mapQuestion_abbreviation() {
        String abbreviation = "dagsform";
        Questionnaire.QuestionnaireItemComponent question1 = buildQuestionItem("1", Questionnaire.QuestionnaireItemType.BOOLEAN, "Har du det godt?", abbreviation);
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(question1));

        // Act
        QuestionnaireModel result = subject.mapQuestionnaire(questionnaire);

        // Assert
        assertEquals(1, result.getQuestions().size());
        assertEquals(abbreviation, result.getQuestions().get(0).getAbbreviation());

    }

    @Test
    public void mapQuestionModel_abbreviation() {
        String abbreviation = "dagsform";
        QuestionModel questionModel = buildQuestionModel(QuestionType.BOOLEAN, "har du det godt?", abbreviation);
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel();
        questionnaireModel.setQuestions(List.of(questionModel));

//        Questionnaire.QuestionnaireItemComponent question1 = buildQuestionItem("1", Questionnaire.QuestionnaireItemType.BOOLEAN, "Har du det godt?", abbreviation);
//        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(question1));

        // Act
        Questionnaire result = subject.mapQuestionnaireModel(questionnaireModel);

        // Assert
        assertEquals(1, result.getItem().size());
        assertTrue(result.getItemFirstRep().hasExtension(Systems.QUESTION_ABBREVIATION));

    }

    @Test
    public void mapQuestion_helperText() {
        String helperText = "help me";
        Questionnaire.QuestionnaireItemComponent question1 = buildQuestionItem("1", Questionnaire.QuestionnaireItemType.BOOLEAN, "Har du det godt?");
        question1.addItem( buildQuestionHelperTextItem(helperText) );
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(question1));

        // Act
        QuestionnaireModel result = subject.mapQuestionnaire(questionnaire);

        // Assert
        assertEquals(1, result.getQuestions().size());
        assertEquals(helperText, result.getQuestions().get(0).getHelperText());
    }

    @Test
    public void mapQuestionModel_helperText() {
        String helperText = "help me";
        QuestionModel questionModel = buildQuestionModel(QuestionType.BOOLEAN, "har du det godt?");
        questionModel.setHelperText(helperText);

        QuestionnaireModel questionnaireModel = buildQuestionnaireModel();
        questionnaireModel.setQuestions(List.of(questionModel));

        // Act
        Questionnaire result = subject.mapQuestionnaireModel(questionnaireModel);

        // Assert
        assertEquals(1, result.getItem().size());
        assertEquals(Questionnaire.QuestionnaireItemType.DISPLAY, result.getItemFirstRep().getItemFirstRep().getType());
        assertEquals(helperText, result.getItemFirstRep().getItemFirstRep().getText());
    }

    @Test
    public void mapQuestionnaire_question_withMeasurementType() {
        Questionnaire.QuestionnaireItemComponent question = buildQuestionItem("1", Questionnaire.QuestionnaireItemType.QUANTITY, "Hvad er din temperatur?");
        Coding temperature = buildTemperatureCode();
        question.setCode(List.of(temperature));

        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(question));

        // Act
        QuestionnaireModel result = subject.mapQuestionnaire(questionnaire);

        // Assert
        assertEquals(1, result.getQuestions().size());
        assertTrue(result.getQuestions().get(0).getQuestionType().equals(QuestionType.QUANTITY));
        assertNotNull(result.getQuestions().get(0).getMeasurementType());
        assertEquals(temperature.getSystem(),  result.getQuestions().get(0).getMeasurementType().getSystem());
        assertEquals(temperature.getCode(),  result.getQuestions().get(0).getMeasurementType().getCode());
        assertEquals(temperature.getDisplay(),  result.getQuestions().get(0).getMeasurementType().getDisplay());

        assertTrue(result.getCallToActions().stream().allMatch(q -> q.getQuestionType().equals(QuestionType.DISPLAY)));

    }

    @Test
    public void mapValueSet_to_MeasurementType() {
        // Arrange
        ValueSet valueSet = buildMeasurementTypesValueSet();


        // Act
        List<MeasurementTypeModel> result = subject.extractMeasurementTypes(valueSet);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(mt -> mt.getSystem().contains(Systems.NPU)));
        assertTrue(result.stream().anyMatch(mt -> mt.getCode().equals("NPU08676")));
        assertTrue(result.stream().anyMatch(mt -> mt.getCode().equals("NPU19748")));
    }

    /**
     * Thresholds is modelled as an extension on PlanDefinition, but was previously modelled on CarePlan.
     * Make sure that Thresholds is defined as extension the right place
     */
    @Test
    public void mapCarePlan_where_PlanDefinition_has_thresholds() {
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1, QUESTIONNAIRE_ID_1);
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);

        // Act

        // Assert
        assertTrue(planDefinition.getAction().stream().anyMatch(a -> a.hasExtension(Systems.THRESHOLD)));
        assertTrue(carePlan.getActivity().stream().noneMatch(a -> a.getDetail().hasExtension(Systems.THRESHOLD)));
    }

    @Test
    public void mapQuestionnaireModel_callToActions() {
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel();

        QuestionModel callToAction = buildCallToAction(questionnaireModel.getQuestions().get(0));
        questionnaireModel.setCallToActions(List.of(callToAction));

        // Act
        Questionnaire result = subject.mapQuestionnaireModel(questionnaireModel);

        // Assert
        assertEquals(2, result.getItem().size());
        assertTrue(result.getItem().stream().anyMatch(i -> i.getType().equals(Questionnaire.QuestionnaireItemType.BOOLEAN))); // the question
        assertTrue(result.getItem().stream().anyMatch(i -> i.getType().equals(Questionnaire.QuestionnaireItemType.GROUP))); // the call-to-action

        List<Questionnaire.QuestionnaireItemComponent> callToActionItemComponents = result.getItem().stream()
            .filter(i -> i.getType().equals(Questionnaire.QuestionnaireItemType.GROUP))
            .flatMap(i -> i.getItem().stream())
            .collect(Collectors.toList());
        assertEquals(1, callToActionItemComponents.size());
        assertEquals(callToAction.getText(), callToActionItemComponents.get(0).getText());

        assertEquals(1, callToActionItemComponents.get(0).getEnableWhen().size());
    }

    private QuestionModel buildCallToAction(QuestionModel questionModel) {
        QuestionModel callToAction = buildCallToAction();

        AnswerModel answer = new AnswerModel();
        answer.setLinkId(questionModel.getLinkId());
        answer.setAnswerType(AnswerType.BOOLEAN);
        answer.setValue(Boolean.TRUE.toString());

        QuestionModel.EnableWhen enableWhen = new QuestionModel.EnableWhen();
        enableWhen.setAnswer(answer);
        enableWhen.setOperator(EnableWhenOperator.EQUAL);

        callToAction.setEnableWhens(List.of(enableWhen));
        return callToAction;
    }

    private QuestionModel buildCallToAction() {
        QuestionModel callToAction = buildQuestionModel(QuestionType.DISPLAY, "call to action text");

        return callToAction;
    }

    private ValueSet buildMeasurementTypesValueSet() {
        ValueSet vs = new ValueSet();

        var npu08676 = new ValueSet.ConceptReferenceComponent();
        npu08676.setCode("NPU08676").setDisplay("Legeme temp.;Pt");

        var npu19748 = new ValueSet.ConceptReferenceComponent();
        npu19748.setCode("NPU19748").setDisplay("C-reaktivt protein [CRP];P");

        vs.getCompose().getIncludeFirstRep()
            .setSystem("urn:oid:1.2.208.176.2.1")
            .setConcept(List.of(npu08676, npu19748));

        return vs;
    }

    private Coding buildTemperatureCode() {
        return buildMeasurementCode("urn:oid:1.2.208.176.2.1", "NPU08676", "Legeme temp.;Pt");
    }

    private Coding buildCrpCode() {
        return buildMeasurementCode("urn:oid:1.2.208.176.2.1", "NPU19748", "C-reaktivt protein [CRP];P");
    }

    private Coding buildMeasurementCode(String system, String code, String display) {
        Coding coding = new Coding();
        coding.setSystem(system)
            .setCode(code)
            .setDisplay(display);

        return coding;
    }

    private Practitioner buildPractitioner(String practitionerId) {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(practitionerId);

        return practitioner;
    }

    private CarePlan buildCarePlan(String careplanId, String patientId, String questionnaireId, String planDefinitionId) {
        CarePlan carePlan = new CarePlan();

        carePlan.setId(careplanId);
        carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
        carePlan.setSubject(new Reference(patientId));
        carePlan.addInstantiatesCanonical(planDefinitionId);
        carePlan.setPeriod(new Period());
        carePlan.setCreated(Date.from(Instant.parse("2021-10-28T00:00:00Z")));
        carePlan.getPeriod().setStart(Date.from(Instant.parse("2021-10-28T00:00:00Z")));
        carePlan.getPeriod().setEnd(Date.from(Instant.parse("2021-10-29T00:00:00Z")));
        carePlan.addExtension(ExtensionMapper.mapCarePlanSatisfiedUntil(Instant.parse("2021-12-07T10:11:12.124Z")));
        carePlan.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1));

        var detail = new CarePlan.CarePlanActivityDetailComponent();
        detail.setInstantiatesCanonical(List.of(new CanonicalType(questionnaireId)));
        detail.setScheduled(buildTiming());
        detail.addExtension(ExtensionMapper.mapActivitySatisfiedUntil(POINT_IN_TIME));

        carePlan.addActivity().setDetail(detail);

        return carePlan;
    }

    private CarePlanModel buildCarePlanModel() {
        CarePlanModel carePlanModel = new CarePlanModel();

        carePlanModel.setId(new QualifiedId(CAREPLAN_ID_1));
        carePlanModel.setStatus(CarePlanStatus.ACTIVE);
        carePlanModel.setCreated(Instant.parse("2021-12-07T10:11:12.124Z"));
        carePlanModel.setPatient(buildPatientModel());
        carePlanModel.setQuestionnaires(List.of(buildQuestionnaireWrapperModel()));
        carePlanModel.setPlanDefinitions(List.of(buildPlanDefinitionModel()));
        carePlanModel.setSatisfiedUntil(Instant.parse("2021-12-07T10:11:12.124Z"));

        return carePlanModel;
    }

    private ContactDetailsModel buildContactDetailsModel() {
        ContactDetailsModel contactDetailsModel = new ContactDetailsModel();

        contactDetailsModel.setStreet("Fiskergade");

        return contactDetailsModel;
    }

    private FrequencyModel buildFrequencyModel() {
        FrequencyModel frequencyModel = new FrequencyModel();

        frequencyModel.setWeekdays(List.of(Weekday.FRI));
        frequencyModel.setTimeOfDay(LocalTime.parse("05:00"));

        return frequencyModel;
    }

    private Organization buildOrganization(String organizationId) {
        Organization organization = new Organization();

        organization.setId(organizationId);
        organization.setName("Infektionsmedicinsk Afdeling");

        return organization;
    }

    private Patient buildPatient(String patientId, String cpr) {
        Patient patient = new Patient();

        patient.setId(patientId);

        var identifier = new Identifier();
        identifier.setSystem(Systems.CPR);
        identifier.setValue(cpr);
        patient.setIdentifier(List.of(identifier));

        var name = new HumanName();
        name.setFamily("Dent");
        name.addGiven("Arthur");
        patient.addName(name);

        var address = new Address();
        address.setCity("Aarhus");
        patient.addAddress(address);

        var primaryTelecom = new ContactPoint();
        primaryTelecom.setSystem(ContactPoint.ContactPointSystem.PHONE);
        primaryTelecom.setValue("12345678");
        primaryTelecom.setRank(1);
        patient.addTelecom(primaryTelecom);

        var secondaryTelecom = new ContactPoint();
        secondaryTelecom.setSystem(ContactPoint.ContactPointSystem.PHONE);
        secondaryTelecom.setValue("12345678");
        secondaryTelecom.setRank(2);
        patient.addTelecom(secondaryTelecom);

        var contactComponent = new Patient.ContactComponent();
        var contactName = new HumanName();
        contactName.setText("Slartibartfast");
        contactComponent.setName(contactName);
        contactComponent.setRelationship(List.of(new CodeableConcept(new Coding(Systems.CONTACT_RELATIONSHIP, "Ven", "Ven"))));
        contactComponent.addTelecom(primaryTelecom);
        contactComponent.addTelecom(secondaryTelecom);
        patient.addContact(contactComponent);

        return patient;
    }

    private PatientModel buildPatientModel() {
        PatientModel patientModel = new PatientModel();

        patientModel.setId(new QualifiedId(PATIENT_ID_1));
        patientModel.setCpr("0101010101");
        patientModel.setPatientContactDetails(buildContactDetailsModel());
        patientModel.setPrimaryRelativeContactDetails(buildContactDetailsModel());
        patientModel.setAdditionalRelativeContactDetails(List.of(buildContactDetailsModel()));

        return patientModel;
    }

    private PlanDefinition buildPlanDefinition(String planDefinitionId, String questionnaireId) {
        PlanDefinition planDefinition = new PlanDefinition();

        planDefinition.setId(planDefinitionId);
        planDefinition.setStatus(Enumerations.PublicationStatus.ACTIVE);
        planDefinition.setDate(Date.from(POINT_IN_TIME));
        planDefinition.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1));

        PlanDefinition.PlanDefinitionActionComponent action = new PlanDefinition.PlanDefinitionActionComponent();
        action.setDefinition(new CanonicalType(questionnaireId));
        action.getTimingTiming().getRepeat().addDayOfWeek(Timing.DayOfWeek.MON).addTimeOfDay("11:00");

        ThresholdModel booleanThreshold = new ThresholdModel();
        booleanThreshold.setQuestionnaireItemLinkId("1");
        booleanThreshold.setType(ThresholdType.NORMAL);
        booleanThreshold.setValueBoolean(true);
        action.addExtension(ExtensionMapper.mapThreshold(booleanThreshold));

        ThresholdModel numberedThreshold = new ThresholdModel();
        numberedThreshold.setQuestionnaireItemLinkId("1");
        numberedThreshold.setType(ThresholdType.NORMAL);
        numberedThreshold.setValueQuantityHigh(5.0);
        numberedThreshold.setValueQuantityLow(2.0);
        action.addExtension(ExtensionMapper.mapThreshold(numberedThreshold));

        planDefinition.addAction(action);

        return planDefinition;
    }

    private PlanDefinitionModel buildPlanDefinitionModel() {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        planDefinitionModel.setId(new QualifiedId(PLANDEFINITION_ID_1));
        planDefinitionModel.setQuestionnaires(List.of(buildQuestionnaireWrapperModel()));

        return planDefinitionModel;
    }

    private Questionnaire.QuestionnaireItemComponent buildQuestionItem(String linkId, Questionnaire.QuestionnaireItemType itemType) {
        return buildQuestionItem(linkId, itemType, null, null);
    }

    private Questionnaire.QuestionnaireItemComponent buildQuestionItem(String linkId, Questionnaire.QuestionnaireItemType itemType, String text) {
        return buildQuestionItem(linkId, itemType, text, null);
    }

    private Questionnaire.QuestionnaireItemComponent buildQuestionItem(String linkId, Questionnaire.QuestionnaireItemType itemType, String text, String abbreviation) {
        var item = new Questionnaire.QuestionnaireItemComponent();

        item.setType(itemType);
        item.setLinkId(linkId);
        item.setText(text);
        if (abbreviation != null) {
            item.addExtension(ExtensionMapper.mapQuestionAbbreviation(abbreviation));
        }

        return item;
    }

    private Questionnaire.QuestionnaireItemComponent buildQuestionItemWithThreshold(String linkId) {
        ThresholdModel normal = buildThresholdModel(linkId, ThresholdType.NORMAL, Boolean.TRUE);
        ThresholdModel critical = buildThresholdModel(linkId, ThresholdType.CRITICAL, Boolean.FALSE);

        Questionnaire.QuestionnaireItemComponent itemComponent = buildQuestionItem(linkId, Questionnaire.QuestionnaireItemType.BOOLEAN, "Har du det godt?");
        itemComponent.getExtension().addAll(ExtensionMapper.mapThresholds(List.of(normal, critical)));

        return itemComponent;
    }

    private Questionnaire.QuestionnaireItemComponent buildQuestionHelperTextItem(String text) {
        var item = new Questionnaire.QuestionnaireItemComponent();

        item.setType(Questionnaire.QuestionnaireItemType.DISPLAY);
        item.setLinkId("help");
        item.setText(text);

        return item;
    }


    private QuestionnaireResponseModel buildQuestionnaireResponseModel() {
        QuestionnaireResponseModel model = new QuestionnaireResponseModel();

        model.setId(new QualifiedId(QUESTIONNAIRERESPONSE_ID_1));
        model.setQuestionnaireId(new QualifiedId(QUESTIONNAIRE_ID_1));
        model.setCarePlanId(new QualifiedId(CAREPLAN_ID_1));
        model.setAuthorId(new QualifiedId(PATIENT_ID_1));
        model.setSourceId(new QualifiedId(PATIENT_ID_1));

        model.setAnswered(Instant.parse("2021-11-03T00:00:00Z"));

        model.setQuestionAnswerPairs(new ArrayList<>());

        QuestionModel question = new QuestionModel();
        AnswerModel answer = new AnswerModel();
        answer.setAnswerType(AnswerType.INTEGER);
        answer.setValue("2");

        model.getQuestionAnswerPairs().add(new QuestionAnswerPairModel(question, answer));

        model.setExaminationStatus(ExaminationStatus.NOT_EXAMINED);
        model.setTriagingCategory(TriagingCategory.GREEN);

        PatientModel patientModel = new PatientModel();
        patientModel.setId(new QualifiedId(PATIENT_ID_1));
        model.setPatient(patientModel);

        return model;
    }

    private QuestionModel buildQuestionModel() {
        return buildQuestionModel(QuestionType.BOOLEAN, "Hvordan har du det?", "dagsform");
    }

    private QuestionModel buildQuestionModel(QuestionType type, String text) {
        return buildQuestionModel(QuestionType.BOOLEAN, "Hvordan har du det?",null);
    }

    private QuestionModel buildQuestionModel(QuestionType type, String text, String abbreviation) {
        QuestionModel questionModel = new QuestionModel();
        questionModel.setText(text);
        questionModel.setAbbreviation(abbreviation);
        questionModel.setQuestionType(type);

        return questionModel;
    }

    private Questionnaire buildQuestionnaire(String questionnaireId) {
        return buildQuestionnaire(questionnaireId, List.of());
    }

    private Questionnaire buildQuestionnaire(String questionnaireId, List<Questionnaire.QuestionnaireItemComponent> questionItems) {
        Questionnaire questionnaire = new Questionnaire();

        questionnaire.setId(questionnaireId);
        questionnaire.setStatus(Enumerations.PublicationStatus.ACTIVE);
        questionnaire.getItem().addAll(questionItems);
        questionnaire.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1));

        return questionnaire;
    }

    private QuestionnaireModel buildQuestionnaireModel() {
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();

        questionnaireModel.setId(new QualifiedId(QUESTIONNAIRE_ID_1));
        questionnaireModel.setStatus(QuestionnaireStatus.ACTIVE);
        questionnaireModel.setQuestions(List.of(buildQuestionModel()));

        return questionnaireModel;
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel() {
        QuestionnaireWrapperModel questionnaireWrapperModel = new QuestionnaireWrapperModel();

        questionnaireWrapperModel.setQuestionnaire(buildQuestionnaireModel());
        questionnaireWrapperModel.setFrequency(buildFrequencyModel());
        questionnaireWrapperModel.setSatisfiedUntil(Instant.parse("2021-12-08T10:11:12.124Z"));

        return questionnaireWrapperModel;
    }

    private QuestionnaireResponse buildQuestionnaireResponse(String questionnaireResponseId, String questionnaireId, String patiientId, List<QuestionnaireResponse.QuestionnaireResponseItemComponent> answerItems) {
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();

        questionnaireResponse.setId(questionnaireResponseId);
        questionnaireResponse.setQuestionnaire(questionnaireId);
        questionnaireResponse.setBasedOn(List.of(new Reference(CAREPLAN_ID_1)));
        questionnaireResponse.setAuthor(new Reference(PATIENT_ID_1));
        questionnaireResponse.setSource(new Reference(PATIENT_ID_1));
        questionnaireResponse.setSubject(new Reference(patiientId));
        questionnaireResponse.getItem().addAll(answerItems);
        questionnaireResponse.setAuthored(Date.from(Instant.parse("2021-10-28T00:00:00Z")));
        questionnaireResponse.getExtension().add(new Extension(Systems.EXAMINATION_STATUS, new StringType(ExaminationStatus.EXAMINED.toString())));
        questionnaireResponse.getExtension().add(new Extension(Systems.EXAMINATION_AUTHOR, new StringType(PRACTITIONER_ID_1)));
        questionnaireResponse.getExtension().add(new Extension(Systems.TRIAGING_CATEGORY, new StringType(TriagingCategory.GREEN.toString())));
        questionnaireResponse.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1));

        return questionnaireResponse;
    }

    private QuestionnaireResponse.QuestionnaireResponseItemComponent buildStringItem(String value, String linkId) {
        return buildItem(new StringType(value), linkId);
    }

    private QuestionnaireResponse.QuestionnaireResponseItemComponent buildIntegerItem(int value, String linkId) {
        return buildItem(new IntegerType(value), linkId);
    }

    private QuestionnaireResponse.QuestionnaireResponseItemComponent buildQuantityItem(double value, String linkId) {
        return buildItem(new Quantity(value), linkId);
    }

    private QuestionnaireResponse.QuestionnaireResponseItemComponent buildItem(Type value, String linkId) {
        var item = new QuestionnaireResponse.QuestionnaireResponseItemComponent();

        var answer = new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent();
        answer.setValue(value);
        item.getAnswer().add(answer);

        item.setLinkId(linkId);

        return item;
    }

    private Timing buildTiming() {
        Timing timing = new Timing();

        var repeat = new Timing.TimingRepeatComponent();
        repeat.setDayOfWeek(List.of(new Enumeration<>(new Timing.DayOfWeekEnumFactory(), Timing.DayOfWeek.FRI)));
        repeat.setTimeOfDay(List.of(new TimeType("04:00")));

        timing.setRepeat(repeat);

        return timing;
    }
}