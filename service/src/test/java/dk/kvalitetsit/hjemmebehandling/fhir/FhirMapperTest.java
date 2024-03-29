package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private static final String PLANDEFINITION_ID_2 = "PlanDefinition/plandefinition-2";
    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";
    private static final String QUESTIONNAIRE_ID_2 = "Questionnaire/questionnaire-2";
    private static final String QUESTIONNAIRERESPONSE_ID_1 = "QuestionnaireResponse/questionnaireresponse-1";
    private static final String QUESTIONNAIRERESPONSE_ID_2 = "QuestionnaireResponse/questionnaireresponse-2";
    private static final String PRACTITIONER_ID_1 = "Practitioner/practitioner-1";

    private static final Instant POINT_IN_TIME = Instant.parse("2021-11-23T00:00:00.000Z");



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
        return buildCarePlan(careplanId, patientId, List.of(questionnaireId), List.of(planDefinitionId));
    }

    private CarePlan buildCarePlan(String careplanId, String patientId, List<String> questionnaireIds, List<String> planDefinitionIds) {

        CarePlan carePlan = new CarePlan();

        carePlan.setId(careplanId);
        carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
        carePlan.setSubject(new Reference(patientId));
        planDefinitionIds.forEach(planDefinitionId -> carePlan.addInstantiatesCanonical(planDefinitionId));
        carePlan.setPeriod(new Period());
        carePlan.setCreated(Date.from(Instant.parse("2021-10-28T00:00:00Z")));
        carePlan.getPeriod().setStart(Date.from(Instant.parse("2021-10-28T00:00:00Z")));
        carePlan.getPeriod().setEnd(Date.from(Instant.parse("2021-10-29T00:00:00Z")));
        carePlan.addExtension(ExtensionMapper.mapCarePlanSatisfiedUntil(Instant.parse("2021-12-07T10:11:12.124Z")));
        carePlan.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1));

        questionnaireIds.forEach(questionnaireId -> {
            var detail = new CarePlan.CarePlanActivityDetailComponent();
            detail.setInstantiatesCanonical(List.of(new CanonicalType(questionnaireId)));
            detail.setScheduled(buildTiming());
            detail.addExtension(ExtensionMapper.mapActivitySatisfiedUntil(POINT_IN_TIME));

            carePlan.addActivity().setDetail(detail);
        });

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
        patientModel.setContactDetails(buildContactDetailsModel());
        patientModel.getPrimaryContact().setContactDetails(buildContactDetailsModel());
        patientModel.setAdditionalRelativeContactDetails(List.of(buildContactDetailsModel()));

        return patientModel;
    }

    private PlanDefinition buildPlanDefinition(String planDefinitionId, String questionnaireId) {
        return buildPlanDefinition(planDefinitionId, "title", questionnaireId);
    }
    private PlanDefinition buildPlanDefinition(String planDefinitionId, String title, String questionnaireId) {
        PlanDefinition planDefinition = new PlanDefinition();

        planDefinition.setId(planDefinitionId);
        planDefinition.setTitle(title);
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
/*
    private Questionnaire.QuestionnaireItemComponent buildQuestionItemWithThreshold(String linkId) {
        ThresholdModel normal = buildThresholdModel(linkId, ThresholdType.NORMAL, Boolean.TRUE);
        ThresholdModel critical = buildThresholdModel(linkId, ThresholdType.CRITICAL, Boolean.FALSE);

        Questionnaire.QuestionnaireItemComponent itemComponent = buildQuestionItem(linkId, Questionnaire.QuestionnaireItemType.BOOLEAN, "Har du det godt?");
        itemComponent.getExtension().addAll(ExtensionMapper.mapThresholds(List.of(normal, critical)));

        return itemComponent;
    }

 */

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