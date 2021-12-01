package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.constants.TriagingCategory;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class FhirMapperTest {
    @InjectMocks
    private FhirMapper subject;

    @Mock
    private DateProvider dateProvider;

    private static final String CPR_1 = "0101010101";

    private static final String CAREPLAN_ID_1 = "careplan-1";
    private static final String ORGANIZATION_ID_1 = "organization-1";
    private static final String PATIENT_ID_1 = "patient-1";
    private static final String PLANDEFINITION_ID_1 = "plandefinition-1";
    private static final String QUESTIONNAIRE_ID_1 = "questionnaire-1";
    private static final String QUESTIONNAIRERESPONSE_ID_1 = "questionnaireresponse-1";

    private static final Instant POINT_IN_TIME = Instant.parse("2021-11-23T00:00:00.000Z");

    @Test
    public void mapCarePlanModel_mapsSubject() {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel();

        // Act
        CarePlan result = subject.mapCarePlanModel(carePlanModel);

        // Assert
        assertEquals(result.getSubject().getReference(), carePlanModel.getPatient().getId());
    }

    @Test
    public void mapCarePlan_mapsPeriod() {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient, questionnaire);

        // Act
        CarePlanModel result = subject.mapCarePlan(carePlan, lookupResult);

        // Assert
        assertEquals(result.getStartDate(), Instant.parse("2021-10-28T00:00:00Z"));
        assertEquals(result.getEndDate(), Instant.parse("2021-10-29T00:00:00Z"));
    }

    @Test
    public void mapCarePlan_roundtrip_preservesExtensions() {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient, questionnaire);

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
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient, questionnaire);

        // Act
        CarePlan result = subject.mapCarePlanModel(subject.mapCarePlan(carePlan, lookupResult));

        // Assert
        assertEquals(carePlan.getActivity().size(), result.getActivity().size());
        assertEquals(carePlan.getActivity().get(0).getDetail().getInstantiatesCanonical().get(0).getValue(), result.getActivity().get(0).getDetail().getInstantiatesCanonical().get(0).getValue());
        assertEquals(carePlan.getActivity().get(0).getDetail().getScheduledTiming().getRepeat().getDayOfWeek().get(0).getValue(), result.getActivity().get(0).getDetail().getScheduledTiming().getRepeat().getDayOfWeek().get(0).getValue());

        assertTrue(result.getActivity().get(0).getDetail().getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.ACTIVITY_SATISFIED_UNTIL)));
    }

    @Test
    public void mapCarePlan_includesQuestionnaires() {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient, questionnaire);

        // Act
        CarePlanModel result = subject.mapCarePlan(carePlan, lookupResult);

        // Assert
        assertEquals(1, result.getQuestionnaires().size());

        assertEquals(FhirUtils.qualifyId(QUESTIONNAIRE_ID_1, ResourceType.Questionnaire), result.getQuestionnaires().get(0).getQuestionnaire().getId());
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
    }

    @Test
    public void mapQuestionnaireResponse_canMapAnswers() {
        // Arrange
        QuestionnaireResponse questionnaireResponse = buildQuestionnaireResponse(QUESTIONNAIRERESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID_1, List.of(buildStringItem("hej", "1"), buildIntegerItem(2, "2")));
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(buildQuestionItem("1"), buildQuestionItem("2")));
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");

        // Act
        QuestionnaireResponseModel result = subject.mapQuestionnaireResponse(questionnaireResponse, FhirLookupResult.fromResources(questionnaireResponse, questionnaire, patient));

        // Assert
        assertEquals(2, result.getQuestionAnswerPairs().size());
        assertEquals(AnswerType.STRING, result.getQuestionAnswerPairs().get(0).getAnswer().getAnswerType());
        assertEquals(AnswerType.INTEGER, result.getQuestionAnswerPairs().get(1).getAnswer().getAnswerType());
    }

    @Test
    public void mapQuestionnaireResponse_roundtrip_preservesExtensions() {
        // Arrange
        QuestionnaireResponse questionnaireResponse = buildQuestionnaireResponse(QUESTIONNAIRERESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID_1, List.of(buildStringItem("hej", "1"), buildIntegerItem(2, "2")));
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(buildQuestionItem("1"), buildQuestionItem("2")));
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(patient, questionnaire);

        // Act
        QuestionnaireResponse result = subject.mapQuestionnaireResponseModel(subject.mapQuestionnaireResponse(questionnaireResponse, lookupResult));

        // Assert
        assertEquals(questionnaireResponse.getExtension().size(), result.getExtension().size());
        assertTrue(result.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.ORGANIZATION)));
        assertTrue(result.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.EXAMINATION_STATUS)));
        assertTrue(result.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.TRIAGING_CATEGORY)));
    }

    private CarePlan buildCarePlan(String careplanId, String patientId, String questionnaireId) {
        CarePlan carePlan = new CarePlan();

        carePlan.setId(careplanId);
        carePlan.setSubject(new Reference(patientId));
        carePlan.setPeriod(new Period());
        carePlan.setCreated(Date.from(Instant.parse("2021-10-28T00:00:00Z")));
        carePlan.getPeriod().setStart(Date.from(Instant.parse("2021-10-28T00:00:00Z")));
        carePlan.getPeriod().setEnd(Date.from(Instant.parse("2021-10-29T00:00:00Z")));
        carePlan.addExtension(ExtensionMapper.mapCarePlanSatisfiedUntil(Instant.parse("2021-12-07T10:11:12.124Z")));
        carePlan.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1));

        var detail = new CarePlan.CarePlanActivityDetailComponent();
        detail.setInstantiatesCanonical(List.of(new CanonicalType(FhirUtils.qualifyId(questionnaireId, ResourceType.Questionnaire))));
        detail.setScheduled(buildTiming());
        detail.addExtension(ExtensionMapper.mapActivitySatisfiedUntil(POINT_IN_TIME));
        carePlan.addActivity().setDetail(detail);

        return carePlan;
    }

    private CarePlanModel buildCarePlanModel() {
        CarePlanModel carePlanModel = new CarePlanModel();

        carePlanModel.setId("careplan-1");
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

    private Patient buildPatient(String patientId, String cpr) {
        Patient patient = new Patient();

        patient.setId(patientId);

        var identifier = new Identifier();
        identifier.setSystem(Systems.CPR);
        identifier.setValue(cpr);
        patient.setIdentifier(List.of(identifier));

        return patient;
    }

    private PatientModel buildPatientModel() {
        PatientModel patientModel = new PatientModel();

        patientModel.setCpr("0101010101");
        patientModel.setPatientContactDetails(buildContactDetailsModel());
        patientModel.setPrimaryRelativeContactDetails(buildContactDetailsModel());
        patientModel.setAdditionalRelativeContactDetails(List.of(buildContactDetailsModel()));

        return patientModel;
    }

    private PlanDefinitionModel buildPlanDefinitionModel() {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        planDefinitionModel.setId("plandefinition-1");
        planDefinitionModel.setQuestionnaires(List.of(buildQuestionnaireWrapperModel()));

        return planDefinitionModel;
    }

    private Questionnaire.QuestionnaireItemComponent buildQuestionItem(String linkId) {
        var item = new Questionnaire.QuestionnaireItemComponent();

        item.setType(Questionnaire.QuestionnaireItemType.INTEGER);
        item.setLinkId(linkId);

        return item;
    }

    private QuestionnaireResponseModel buildQuestionnaireResponseModel() {
        QuestionnaireResponseModel model = new QuestionnaireResponseModel();

        model.setAnswered(Instant.parse("2021-11-03T00:00:00Z"));

        model.setQuestionAnswerPairs(new ArrayList<>());

        QuestionModel question = new QuestionModel();
        AnswerModel answer = new AnswerModel();
        answer.setAnswerType(AnswerType.INTEGER);
        answer.setValue("2");

        model.getQuestionAnswerPairs().add(new QuestionAnswerPairModel(question, answer));

        model.setTriagingCategory(TriagingCategory.GREEN);

        PatientModel patientModel = new PatientModel();
        patientModel.setId("patient-1");
        model.setPatient(patientModel);

        return model;
    }

    private QuestionModel buildQuestionModel() {
        QuestionModel questionModel = new QuestionModel();

        questionModel.setText("Hvordan har du det?");

        return questionModel;
    }

    private Questionnaire buildQuestionnaire(String questionnaireId) {
        return buildQuestionnaire(questionnaireId, List.of());
    }

    private Questionnaire buildQuestionnaire(String questionnaireId, List<Questionnaire.QuestionnaireItemComponent> questionItems) {
        Questionnaire questionnaire = new Questionnaire();

        questionnaire.setId(FhirUtils.qualifyId(questionnaireId, ResourceType.Questionnaire));
        questionnaire.setStatus(Enumerations.PublicationStatus.ACTIVE);
        questionnaire.getItem().addAll(questionItems);

        return questionnaire;
    }

    private QuestionnaireModel buildQuestionnaireModel() {
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();

        questionnaireModel.setId("questionnaire-1");
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
        questionnaireResponse.setQuestionnaire(FhirUtils.qualifyId(questionnaireId, ResourceType.Questionnaire));
        questionnaireResponse.setSubject(new Reference(patiientId));
        questionnaireResponse.getItem().addAll(answerItems);
        questionnaireResponse.setAuthored(Date.from(Instant.parse("2021-10-28T00:00:00Z")));
        questionnaireResponse.getExtension().add(new Extension(Systems.EXAMINATION_STATUS, new StringType(ExaminationStatus.EXAMINED.toString())));
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