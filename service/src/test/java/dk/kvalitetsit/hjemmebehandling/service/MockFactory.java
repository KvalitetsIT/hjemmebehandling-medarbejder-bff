package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Questionnaire;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dk.kvalitetsit.hjemmebehandling.service.Constants.*;

public class MockFactory {


    public static QuestionnaireModel buildQuestionnaireModel(QualifiedId.QuestionnaireId questionnaireId) {
        var builder = QuestionnaireModel.builder();
        builder.questions(new ArrayList<>());
        builder.id(questionnaireId);
        var questionBuilder = QuestionModel.builder();
        questionBuilder.linkId("question-1");
        var question = questionBuilder.build();
        builder.questions(List.of(question));
        ThresholdModel thresholdModel = buildThresholdModel(question.linkId());
        questionBuilder.thresholds(List.of(thresholdModel));
        return builder.build();
    }

    public static ThresholdModel buildThresholdModel(String questionnaireLinkId, Double valueQuantityLow) {
        return new ThresholdModel(
                questionnaireLinkId,
                ThresholdType.NORMAL,
                valueQuantityLow,
                null,
                Boolean.TRUE,
                null
        );
    }

    public static ThresholdModel buildThresholdModel(String questionnaireLinkId) {
        return new ThresholdModel(
                questionnaireLinkId,
                ThresholdType.NORMAL,
                null,
                null,
                Boolean.TRUE,
                null
        );
    }

    public static QuestionModel buildMeasurementQuestionModel() {
        var builder = QuestionModel.builder();
        builder.linkId(IdType.newRandomUuid().getValueAsString());
        builder.text("Hvad er din temperatur?");
        builder.questionType(QuestionType.QUANTITY);
        return builder.build();
    }

    public static ThresholdModel buildMeasurementThresholdModel(String questionnaireLinkId) {
        return new ThresholdModel(
                questionnaireLinkId,
                ThresholdType.NORMAL,
                Double.valueOf("36.5"),
                Double.valueOf("37.5"),
                null,
                null
        );
    }

    public static PlanDefinitionModel buildPlanDefinition() {
        return PlanDefinitionModel.builder()
                .id(PLANDEFINITION_ID_1)
                .status(Status.ACTIVE)
                .build();
    }

    public static PlanDefinitionModel buildPlanDefinitionModel(QualifiedId.QuestionnaireId questionnaireId, ThresholdModel questionnaireThreshold) {
        return PlanDefinitionModel.builder()
                .questionnaires(List.of(QuestionnaireWrapperModel.builder()
                        .questionnaire(QuestionnaireModel.builder().id(questionnaireId).build())
                        .thresholds(List.of(questionnaireThreshold))
                        .build())
                ).build();
    }

    public static QuestionnaireModel buildQuestionnaire(QualifiedId.QuestionnaireId questionnaireId) {
        return QuestionnaireModel.builder()
                .id(questionnaireId)
                .build();
    }

    public static Questionnaire.QuestionnaireItemComponent buildMeasurementQuestion() {
        Questionnaire.QuestionnaireItemComponent question = new Questionnaire.QuestionnaireItemComponent();
        question.setLinkId("temperature").setText("Hvad er din temperatur?").setType(Questionnaire.QuestionnaireItemType.QUANTITY);
        return question;
    }

    public static QuestionModel buildQuestionModel(String linkId) {
        return QuestionModel.builder()
                .linkId(linkId)
                .build();
    }

    public static QuestionModel buildQuestionModel() {
        return QuestionModel.builder().build();
    }

    public static QuestionnaireResponseModel buildQuestionnaireResponse(QualifiedId.QuestionnaireResponseId questionnaireResponseId, QualifiedId.QuestionnaireId questionnaireId) {
        return buildQuestionnaireResponse(questionnaireResponseId, questionnaireId, ORGANIZATION_ID_1);
    }

    public static QuestionnaireResponseModel buildQuestionnaireResponse(QualifiedId.QuestionnaireResponseId questionnaireResponseId, QualifiedId.QuestionnaireId questionnaireId, QualifiedId.OrganizationId organizationId) {
        return QuestionnaireResponseModel.builder()
                .id(questionnaireResponseId)
                .questionnaireId(questionnaireId)
                .organizationId(organizationId)
                .build();

    }

    public static BaseModel buildResource() {
        return buildResource(null);
    }

    public static BaseModel buildResource(QualifiedId.OrganizationId organizationId) {
        var resource = CarePlanModel.builder();
        if (organizationId != null) {
            resource.organizationId(organizationId);
        }
        return resource.build();
    }

    public static Organization buildOrganization() {
        var organization = new Organization();
        organization.setId(ORGANIZATION_ID_1.unqualified());
        return organization;
    }


    public static CarePlanModel buildCarePlanModel() {
        return buildCarePlanModel(null, null);
    }

    public static CarePlanModel buildCarePlanModel(List<QualifiedId.PlanDefinitionId> planDefinitionIds, List<QualifiedId.QuestionnaireId> questionnaireIds) {
        return buildCarePlanModel(null, planDefinitionIds, questionnaireIds, null);
    }

    public static CarePlanModel buildCarePlanModel(List<QualifiedId.PlanDefinitionId> planDefinitionIds, List<QualifiedId.QuestionnaireId> questionnaireIds, PatientModel patient) {
        return buildCarePlanModel(null, planDefinitionIds, questionnaireIds, patient);

    }

    public static CarePlanModel buildCarePlanModel(QualifiedId.CarePlanId id, List<QualifiedId.PlanDefinitionId> planDefinitionIds, List<QualifiedId.QuestionnaireId> questionnaireIds, PatientModel patient) {
        return CarePlanModel.builder()
                .id(id)
                .patient(patient)
                .planDefinitions(List.of())
                .planDefinitions(Optional.ofNullable(planDefinitionIds).map(x1 -> x1.stream().map(MockFactory::buildPlanDefinitionModel).toList()).orElse(null))
                .questionnaires(questionnaireIds != null ? questionnaireIds.stream().map(MockFactory::buildQuestionnaireWrapperModel).toList() : List.of())
                .build();
    }


    public static PatientModel buildPatientModel() {
        return PatientModel
                .builder()
                .id(PATIENT_ID_1)
                .contactDetails(buildContactDetails())
                .primaryContact(buildPrimaryContact())
                .build();
    }

    public static PrimaryContactModel buildPrimaryContact() {
        return PrimaryContactModel.builder()
                .name("Poul")
                .organisation(new QualifiedId.OrganizationId("infektionsmedicinsk"))
                .affiliation("Onkel")
                .build();
    }

    public static ContactDetailsModel buildContactDetails() {
        return ContactDetailsModel.builder().build();
    }

    public static PatientModel buildPatientModel(QualifiedId.PatientId patientId, String givenName, String familyName) {
        return PatientModel.builder()
                .id(patientId)
                .contactDetails(ContactDetailsModel.builder().build())
                .name(new PersonNameModel(familyName, List.of(givenName)))
                .build();
    }

    public static QuestionnaireWrapperModel buildQuestionnaireWrapperModel(QualifiedId.QuestionnaireId questionnaireId) {
        return buildQuestionnaireWrapperModel(questionnaireId, POINT_IN_TIME);
    }

    public static QuestionnaireWrapperModel buildQuestionnaireWrapperModel(QualifiedId.QuestionnaireId questionnaireId, Instant satisfiedUntil) {
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel(questionnaireId);
        return QuestionnaireWrapperModel.builder()
                .questionnaire(QuestionnaireModel.builder()
                        .id(questionnaireId)
                        .build())
                .frequency(FrequencyModel.builder()
                        .weekdays(List.of(Weekday.TUE))
                        .timeOfDay(LocalTime.parse("04:00")).build())
                .satisfiedUntil(satisfiedUntil)
                .questionnaire(questionnaireModel)
                .thresholds(questionnaireModel.questions().stream()
                        .flatMap(q -> q.thresholds().stream())
                        .toList())
                .build();
    }

    public static PlanDefinitionModel buildPlanDefinitionModel(QualifiedId.PlanDefinitionId planDefinitionId) {
        return PlanDefinitionModel.builder().id(planDefinitionId).build();
    }


    public static CarePlanModel buildCarePlan(QualifiedId.CarePlanId carePlanId, QualifiedId.PatientId patientId) {
        return buildCarePlan(carePlanId, patientId, null);
    }

    public static CarePlanModel buildCarePlan(QualifiedId.CarePlanId carePlanId, QualifiedId.PatientId patientId, QualifiedId.QuestionnaireId questionnaireId) {
        return CarePlanModel.builder()
                .id(carePlanId)
                .patient(PatientModel.builder().id(patientId).build())
                .satisfiedUntil(POINT_IN_TIME)
                .build();

    }


    public static FrequencyModel buildFrequencyModel(List<Weekday> weekdays, String timeOfDay) {
        return FrequencyModel.builder()
                .weekdays(weekdays)
                .timeOfDay(LocalTime.parse(timeOfDay)).build();
    }

    public static PatientModel buildPatient(QualifiedId.PatientId patientId, CPR cpr) {

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


    public static PatientModel buildPatient(QualifiedId.PatientId patientId, CPR cpr, String givenName, String familyName) {
        PatientModel patient = buildPatient(patientId, cpr);

        return PatientModel.Builder.from(patient)
                .name(PersonNameModel.builder()
                        .given(List.of(givenName))
                        .family(familyName)
                        .build()
                )
                .build();
    }

    public static PatientDetails buildPatientDetails() {
        return PatientDetails.builder()
                .patientPrimaryPhone("11223344")
                .patientSecondaryPhone("44332211")
                .primaryRelativeName("Dronning Margrethe")
                .primaryRelativeAffiliation("Ven")
                .primaryRelativePrimaryPhone("98798798")
                .primaryRelativeSecondaryPhone("78978978").build();
    }


    public static QuestionnaireModel buildQuestionnaire() {
        return QuestionnaireModel.builder()
                .id(QUESTIONNAIRE_ID_1)
                .build();
    }


}
