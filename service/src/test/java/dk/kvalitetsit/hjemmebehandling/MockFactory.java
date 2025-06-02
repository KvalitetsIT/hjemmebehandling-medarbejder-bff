package dk.kvalitetsit.hjemmebehandling;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.TimeType;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dk.kvalitetsit.hjemmebehandling.Constants.*;


public class MockFactory {


    public static QuestionnaireModel buildQuestionnaireModel(QualifiedId.QuestionnaireId questionnaireId) {
        var linkId = "question-1";

        var question = QuestionModel.builder()
                .linkId(linkId)
                .thresholds(List.of(buildThresholdModel(linkId)))
                .build();

        return QuestionnaireModel.builder()
                .questions(new ArrayList<>())
                .id(questionnaireId)
                .questions(List.of(question))
                .build();
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
        return QuestionModel.builder()
                .linkId(IdType.newRandomUuid().getValueAsString())
                .text("Hvad er din temperatur?")
                .questionType(QuestionType.QUANTITY)
                .build();
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


    public static Questionnaire.QuestionnaireItemComponent buildMeasurementQuestion() {
        return new Questionnaire.QuestionnaireItemComponent()
                .setLinkId("temperature")
                .setText("Hvad er din temperatur?")
                .setType(Questionnaire.QuestionnaireItemType.QUANTITY);

    }

    public static QuestionModel buildQuestionModel(String linkId) {
        return QuestionModel.builder()
                .linkId(linkId)
                .build();
    }


    public static QuestionnaireResponseModel buildQuestionnaireResponse(QualifiedId.QuestionnaireResponseId questionnaireResponseId, QualifiedId.QuestionnaireId questionnaireId) {
        return buildQuestionnaireResponse(questionnaireResponseId, questionnaireId, ORGANIZATION_ID_1);
    }

    public static QuestionnaireResponseModel buildQuestionnaireResponse(QualifiedId.QuestionnaireResponseId questionnaireResponseId, QualifiedId.QuestionnaireId questionnaireId, QualifiedId.OrganizationId organizationId) {
        return QuestionnaireResponseModel.builder()
                .id(questionnaireResponseId)
                .patient(buildPatient(PATIENT_ID_1, CPR_1))
                .questionnaireId(questionnaireId)
                .organizationId(organizationId)
                .build();
    }

    public static CarePlanModel buildResource() {
        return buildResource(null);
    }

    public static CarePlanModel buildResource(QualifiedId.OrganizationId organizationId) {
        var resource = CarePlanModel.builder();
        Optional.ofNullable(organizationId).ifPresent(resource::organizationId);
        return resource.build();
    }

    public static OrganizationModel buildOrganization() {
        return new OrganizationModel(ORGANIZATION_ID_1, ORGANIZATION_NAME, new TimeType("11.00"));
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

        var thresholds = questionnaireModel.questions().stream()
                .flatMap(q -> q.thresholds().stream())
                .toList();

        var frequency = FrequencyModel.builder()
                .weekdays(List.of(Weekday.TUE))
                .timeOfDay(LocalTime.parse("04:00")).build();

        var questionnaire = QuestionnaireModel.builder()
                .id(questionnaireId)
                .build();

        return QuestionnaireWrapperModel.builder()
                .questionnaire(questionnaire)
                .frequency(frequency)
                .satisfiedUntil(satisfiedUntil)
                .questionnaire(questionnaireModel)
                .thresholds(thresholds)
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
        var primaryContact = PrimaryContactModel.builder()
                .name("Yvonne")
                .affiliation("Moster")
                .organisation(new QualifiedId.OrganizationId("infektionsmedicinsk"))
                .build();

        return PatientModel.builder()
                .id(patientId)
                .cpr(cpr)
                .name(PersonNameModel.builder().given("Yvonne").build())
                .primaryContact(primaryContact)
                .build();
    }


    public static PatientDetails buildPatientDetails() {
        return PatientDetails.builder()
                .patientPrimaryPhone("11223344")
                .patientSecondaryPhone("44332211")
                .primaryRelativeName("Dronning Margrethe")
                .primaryRelativeAffiliation("Ven")
                .primaryRelativePrimaryPhone("98798798")
                .primaryRelativeSecondaryPhone("78978978")
                .build();
    }


    public static QuestionnaireModel buildQuestionnaire() {
        return QuestionnaireModel.builder()
                .id(QUESTIONNAIRE_ID_1)
                .build();
    }


}
