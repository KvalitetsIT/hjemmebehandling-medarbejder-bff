package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.*;
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

import static dk.kvalitetsit.hjemmebehandling.service.Constants.*;

@ExtendWith(MockitoExtension.class)
public class FhirMapperTest {

    private static final Instant POINT_IN_TIME = Instant.parse("2021-11-23T00:00:00.000Z");
    @InjectMocks
    private FhirMapper subject;
    @Mock
    private DateProvider dateProvider;

    private QuestionModel buildCallToAction(QuestionModel questionModel) {
        AnswerModel answer = new AnswerModel(questionModel.linkId(), Boolean.TRUE.toString(), AnswerType.BOOLEAN, null);

        QuestionModel.EnableWhen enableWhen = new QuestionModel.EnableWhen(answer, EnableWhenOperator.EQUAL);

        return buildCallToAction(List.of(enableWhen));
    }

    private QuestionModel buildCallToAction(List<QuestionModel.EnableWhen> enableWhens) {
        return buildQuestionModel(QuestionType.DISPLAY, "call to action text");
    }

    private QuestionModel buildCallToAction() {
        return buildQuestionModel(QuestionType.DISPLAY, "call to action text");
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
        return buildMeasurementCode("NPU08676", "Legeme temp.;Pt");
    }

    private Coding buildCrpCode() {
        return buildMeasurementCode("NPU19748", "C-reaktivt protein [CRP];P");
    }

    private Coding buildMeasurementCode(String code, String display) {
        return new Coding().setSystem("urn:oid:1.2.208.176.2.1")
                .setCode(code)
                .setDisplay(display);
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
        planDefinitionIds.forEach(carePlan::addInstantiatesCanonical);
        carePlan.setPeriod(new Period());
        carePlan.setCreated(Date.from(Instant.parse("2021-10-28T00:00:00Z")));
        carePlan.getPeriod().setStart(Date.from(Instant.parse("2021-10-28T00:00:00Z")));
        carePlan.getPeriod().setEnd(Date.from(Instant.parse("2021-10-29T00:00:00Z")));
        carePlan.addExtension(ExtensionMapper.mapCarePlanSatisfiedUntil(Instant.parse("2021-12-07T10:11:12.124Z")));
        carePlan.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1.qualified()));

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
        return CarePlanModel.builder()
                .id(CAREPLAN_ID_1)
                .status(CarePlanStatus.ACTIVE)
                .created(Instant.parse("2021-12-07T10:11:12.124Z"))
                .patient(buildPatientModel())
                .questionnaires(List.of(buildQuestionnaireWrapperModel()))
                .planDefinitions(List.of(buildPlanDefinitionModel()))
                .satisfiedUntil(Instant.parse("2021-12-07T10:11:12.124Z")).build();
    }

    private ContactDetailsModel buildContactDetailsModel() {
        return ContactDetailsModel.builder().street("Fiskergade").build();
    }

    private FrequencyModel buildFrequencyModel() {
        return new FrequencyModel(List.of(Weekday.FRI), LocalTime.parse("05:00"));
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
        return PatientModel.builder()
                .id(PATIENT_ID_1)
                .cpr(CPR_1)
                .contactDetails(buildContactDetailsModel())
                .primaryContact(PrimaryContactModel.builder().contactDetails(buildContactDetailsModel()).build())
                .additionalRelativeContactDetails(List.of(buildContactDetailsModel()))
                .build();
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
        planDefinition.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1.qualified()));

        PlanDefinition.PlanDefinitionActionComponent action = new PlanDefinition.PlanDefinitionActionComponent();
        action.setDefinition(new CanonicalType(questionnaireId));
        action.getTimingTiming().getRepeat().addDayOfWeek(Timing.DayOfWeek.MON).addTimeOfDay("11:00");

        ThresholdModel booleanThreshold = new ThresholdModel("1", ThresholdType.NORMAL, null, null, true, null);

        action.addExtension(ExtensionMapper.mapThreshold(booleanThreshold));

        ThresholdModel numberedThreshold = new ThresholdModel("1", ThresholdType.NORMAL, 5.0, 2.0, null, null);
        action.addExtension(ExtensionMapper.mapThreshold(numberedThreshold));

        planDefinition.addAction(action);

        return planDefinition;
    }

    private PlanDefinitionModel buildPlanDefinitionModel() {
        return PlanDefinitionModel.builder()
                .id(PLANDEFINITION_ID_1)
                .questionnaires(List.of(buildQuestionnaireWrapperModel()))
                .build();
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
        QuestionModel question = buildQuestionModel();
        AnswerModel answer = new AnswerModel(null, "2", AnswerType.INTEGER, null);
        return QuestionnaireResponseModel.builder()
                .id(QUESTIONNAIRE_RESPONSE_ID_1)
                .questionnaireId(QUESTIONNAIRE_ID_1)
                .carePlanId(CAREPLAN_ID_1)
                .authorId(PRACTITIONER_ID_1)
                .sourceId(QUESTIONNAIRE_ID_1)
                .answered(Instant.parse("2021-11-03T00:00:00Z"))
                .questionAnswerPairs(new ArrayList<>())
                .patient(PatientModel.builder().id(PATIENT_ID_1).build())
                .examinationStatus(ExaminationStatus.NOT_EXAMINED)
                .triagingCategory(TriagingCategory.GREEN)
                .questionAnswerPairs(List.of(new QuestionAnswerPairModel(question, answer)))
                .build();
    }

    private QuestionModel buildQuestionModel() {
        return buildQuestionModel(QuestionType.BOOLEAN, "Hvordan har du det?", "dagsform", null);
    }

    private QuestionModel buildQuestionModel(QuestionType type, String text, List<QuestionModel.EnableWhen> enableWhens) {
        return buildQuestionModel(QuestionType.BOOLEAN, "Hvordan har du det?", null, enableWhens);
    }

    private QuestionModel buildQuestionModel(QuestionType type, String text) {
        return buildQuestionModel(QuestionType.BOOLEAN, "Hvordan har du det?", null);
    }


    private QuestionModel buildQuestionModel(QuestionType type, String text, String abbreviation, List<QuestionModel.EnableWhen> enableWhens) {
        return new QuestionModel(
                null,
                text,
                abbreviation,
                null,
                false,
                type,
                null,
                null,
                enableWhens,
                null,
                null,
                false
        );
    }

    private Questionnaire buildQuestionnaire(String questionnaireId) {
        return buildQuestionnaire(questionnaireId, List.of());
    }

    private Questionnaire buildQuestionnaire(String questionnaireId, List<Questionnaire.QuestionnaireItemComponent> questionItems) {
        Questionnaire questionnaire = new Questionnaire();

        questionnaire.setId(questionnaireId);
        questionnaire.setStatus(Enumerations.PublicationStatus.ACTIVE);
        questionnaire.getItem().addAll(questionItems);
        questionnaire.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1.qualified()));

        return questionnaire;
    }

    private QuestionnaireModel buildQuestionnaireModel() {
        return QuestionnaireModel.builder()
                .id(QUESTIONNAIRE_ID_1)
                .status(Status.ACTIVE)
                .questions(List.of(buildQuestionModel())).build();
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel() {
        return QuestionnaireWrapperModel.builder()
                .questionnaire(buildQuestionnaireModel())
                .frequency(buildFrequencyModel())
                .satisfiedUntil(Instant.parse("2021-12-08T10:11:12.124Z"))
                .build();
    }

    private QuestionnaireResponse buildQuestionnaireResponse(String questionnaireResponseId, String questionnaireId, String patientId, List<QuestionnaireResponse.QuestionnaireResponseItemComponent> answerItems) {
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();

        questionnaireResponse.setId(questionnaireResponseId);
        questionnaireResponse.setQuestionnaire(questionnaireId);
        questionnaireResponse.setBasedOn(List.of(new Reference(CAREPLAN_ID_1.qualified())));
        questionnaireResponse.setAuthor(new Reference(PATIENT_ID_1.qualified()));
        questionnaireResponse.setSource(new Reference(PATIENT_ID_1.qualified()));
        questionnaireResponse.setSubject(new Reference(patientId));
        questionnaireResponse.getItem().addAll(answerItems);
        questionnaireResponse.setAuthored(Date.from(Instant.parse("2021-10-28T00:00:00Z")));
        questionnaireResponse.getExtension().add(new Extension(Systems.EXAMINATION_STATUS, new StringType(ExaminationStatus.EXAMINED.toString())));
        questionnaireResponse.getExtension().add(new Extension(Systems.EXAMINATION_AUTHOR, new StringType(PRACTITIONER_ID_1.qualified())));
        questionnaireResponse.getExtension().add(new Extension(Systems.TRIAGING_CATEGORY, new StringType(TriagingCategory.GREEN.toString())));
        questionnaireResponse.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1.qualified()));

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