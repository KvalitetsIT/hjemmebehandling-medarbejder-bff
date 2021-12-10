package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.*;
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

    private static final String CAREPLAN_ID_1 = "CarePlan/careplan-1";
    private static final String ORGANIZATION_ID_1 = "Organization/organization-1";
    private static final String PATIENT_ID_1 = "Patient/patient-1";
    private static final String PLANDEFINITION_ID_1 = "PlanDefinition/plandefinition-1";
    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";
    private static final String QUESTIONNAIRERESPONSE_ID_1 = "QuestionnaireResponse/questionnaireresponse-1";

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
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);

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
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);

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
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);

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
    public void mapCarePlan_includesQuestionnaires() {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        Organization organization = buildOrganization(ORGANIZATION_ID_1);
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(carePlan, patient, questionnaire, organization, planDefinition);

        // Act
        CarePlanModel result = subject.mapCarePlan(carePlan, lookupResult);

        // Assert
        assertEquals(1, result.getQuestionnaires().size());

        assertEquals(QUESTIONNAIRE_ID_1, result.getQuestionnaires().get(0).getQuestionnaire().getId().toString());
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
    }

    @Test
    public void mapQuestionnaireResponse_canMapAnswers() {
        // Arrange
        QuestionnaireResponse questionnaireResponse = buildQuestionnaireResponse(QUESTIONNAIRERESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID_1, List.of(buildStringItem("hej", "1"), buildQuantityItem(2, "2")));
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(buildQuestionItem("1", Questionnaire.QuestionnaireItemType.STRING), buildQuestionItem("2", Questionnaire.QuestionnaireItemType.QUANTITY)));
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);

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
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(patient, questionnaire, carePlan, planDefinition);

        // Act
        QuestionnaireResponse result = subject.mapQuestionnaireResponseModel(subject.mapQuestionnaireResponse(questionnaireResponse, lookupResult));

        // Assert
        assertEquals(questionnaireResponse.getExtension().size(), result.getExtension().size());
        assertTrue(result.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.ORGANIZATION)));
        assertTrue(result.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.EXAMINATION_STATUS)));
        assertTrue(result.getExtension().stream().anyMatch(e -> e.getUrl().equals(Systems.TRIAGING_CATEGORY)));
    }

    @Test
    public void mapQuestionnaireResponse_roundtrip_preservesReferences() {
        // Arrange
        QuestionnaireResponse questionnaireResponse = buildQuestionnaireResponse(QUESTIONNAIRERESPONSE_ID_1, QUESTIONNAIRE_ID_1, PATIENT_ID_1, List.of(buildStringItem("hej", "1"), buildIntegerItem(2, "2")));
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1, List.of(buildQuestionItem("1", Questionnaire.QuestionnaireItemType.STRING), buildQuestionItem("2", Questionnaire.QuestionnaireItemType.INTEGER)));
        Patient patient = buildPatient(PATIENT_ID_1, "0101010101");
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID_1, PATIENT_ID_1, QUESTIONNAIRE_ID_1, PLANDEFINITION_ID_1);
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(patient, questionnaire, carePlan, planDefinition);

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
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);

        FhirLookupResult lookupResult = FhirLookupResult.fromResources(patient, questionnaire, carePlan, planDefinition);

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

    private PlanDefinition buildPlanDefinition(String planDefinitionId) {
        PlanDefinition planDefinition = new PlanDefinition();

        planDefinition.setId(planDefinitionId);
        planDefinition.addExtension(ExtensionMapper.mapOrganizationId(ORGANIZATION_ID_1));

        return planDefinition;
    }

    private PlanDefinitionModel buildPlanDefinitionModel() {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        planDefinitionModel.setId(new QualifiedId(PLANDEFINITION_ID_1));
        planDefinitionModel.setQuestionnaires(List.of(buildQuestionnaireWrapperModel()));

        return planDefinitionModel;
    }

    private Questionnaire.QuestionnaireItemComponent buildQuestionItem(String linkId, Questionnaire.QuestionnaireItemType itemType) {
        var item = new Questionnaire.QuestionnaireItemComponent();

        item.setType(itemType);
        item.setLinkId(linkId);

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
        QuestionModel questionModel = new QuestionModel();

        questionModel.setText("Hvordan har du det?");

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