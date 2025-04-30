package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.model.constants.AnswerType;
import dk.kvalitetsit.hjemmebehandling.model.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.model.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import org.junit.jupiter.api.Test;
import org.openapitools.model.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DtoMapperTest {
    private static final String CAREPLAN_ID_1 = "CarePlan/careplan-1";
    private static final String ORGANIZATION_ID_1 = "Organization/organization-1";
    private static final String PATIENT_ID_1 = "Patient/patient-1";
    private static final String PLANDEFINITION_ID_1 = "PlanDefinition/plandefinition-1";
    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";
    private static final String QUESTIONNAIRERESPONSE_ID_1 = "QuestionnaireResponse/questionnaireresponse-1";
    private final DtoMapper subject = new DtoMapper();
    QuestionnaireModel questionnaireModel = buildQuestionnaireModel();

    @Test
    public void mapPlanDefinitionDto_success() {
        PlanDefinitionDto planDefinitionDto = buildPlanDefinitionDto();
        PlanDefinitionModel result = subject.mapPlanDefinitionDto(planDefinitionDto);

        assertEquals(planDefinitionDto.getId().get(), result.id().toString());
    }

    @Test
    public void mapPlanDefinitionModel_success() {
        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel();

        PlanDefinitionDto result = subject.mapPlanDefinitionModel(planDefinitionModel);

        assertEquals(planDefinitionModel.id().toString(), result.getId().get());
    }

    @Test
    public void mapCarePlanDto_success() {
        CarePlanDto carePlanDto = buildCarePlanDto();

        CarePlanModel result = subject.mapCarePlanDto(carePlanDto);

        assertEquals(carePlanDto.getId().get(), result.id().toString());
    }

    @Test
    public void mapCarePlanModel_success() {
        CarePlanModel carePlanModel = buildCarePlanModel();

        CarePlanDto result = subject.mapCarePlanModel(carePlanModel);

        assertEquals(Optional.ofNullable(carePlanModel.id().toString()), result.getId());
    }

    @Test
    public void mapPatientDto_success() {
        PatientDto patientDto = buildPatientDto();

        PatientModel result = subject.mapPatientDto(patientDto);

        assertEquals(patientDto.getCpr().get(), result.cpr());
    }

    @Test
    public void mapPatientModel_success() {
        PatientModel patientModel = buildPatientModel();

        PatientDto result = subject.mapPatientModel(patientModel);

        assertEquals(patientModel.cpr(), result.getCpr().get());
    }

    @Test
    public void mapPersonModel_success() {
        PersonModel personModel = buildPersonModel();

        PersonDto result = subject.mapPersonModel(personModel);

        assertEquals(personModel.identifier().id(), result.getCpr().get());
        assertEquals("Arthur A.", result.getGivenName().get());
    }

    @Test
    public void mapQuestionnaireResponseModel_success() {
        QuestionnaireResponseModel questionnaireResponseModel = buildQuestionnaireResponseModel();

        QuestionnaireResponseDto result = subject.mapQuestionnaireResponseModel(questionnaireResponseModel);

        assertEquals(questionnaireResponseModel.id().toString(), result.getId());
    }

    @Test
    public void mapQuestionnaireModel_callToAction() {
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel(buildQuestionModel());
        QuestionnaireDto result = subject.mapQuestionnaireModel(questionnaireModel);
        assertNotNull(result.getCallToAction());
    }

    @Test
    public void mapQuestionnaireModel_enableWhen() {

        QuestionModel questionModel = buildQuestionModel(List.of(new QuestionModel.EnableWhen(null, null)));


        QuestionnaireModel questionnaireModel = buildQuestionnaireModel(List.of(questionModel));

        QuestionnaireDto result = subject.mapQuestionnaireModel(questionnaireModel);

        assertEquals(1, result.getQuestions().size());
        assertEquals(1, result.getQuestions().get(0).getEnableWhen().size());
    }

    @Test
    public void mapQuestionnaireDto_enableWhen() {
        QuestionnaireDto questionnaireDto = new QuestionnaireDto();

        QuestionDto questionDto = buildQuestionDto();
        questionDto.setEnableWhen(List.of(new EnableWhen()));
        questionnaireDto.setQuestions(List.of(questionDto));

        QuestionnaireModel result = subject.mapQuestionnaireDto(questionnaireDto);

        assertEquals(1, result.questions().size());
        assertEquals(1, result.questions().get(0).enableWhens().size());
    }


    @Test
    public void mapFrequencyModel_allValuesAreNull_noErrors() {
        var toMap = FrequencyModel.builder().build();
        var result = subject.mapFrequencyModel(toMap);
        assertNotNull(result);
    }

    @Test
    public void mapMeasurementTypeModel_success() {
        MeasurementTypeModel measurementTypeModel = MeasurementTypeModel.builder()
                .system("system")
                .code("code")
                .display("display")
                .build();

        MeasurementTypeDto result = subject.mapMeasurementTypeModel(measurementTypeModel);

        assertEquals(measurementTypeModel.system(), result.getSystem().get());
        assertEquals(measurementTypeModel.code(), result.getCode().get());
        assertEquals(measurementTypeModel.display(), result.getDisplay().get());
    }

    @Test
    public void mapQuestionnaireModel_thresholds() {
        QuestionModel questionModel = buildQuestionModel().builder()
                .thresholds(List.of(buildBooleanThresholdModel(null), buildNumberThresholdModel(null)))
                .build();

        QuestionnaireModel questionnaireModel = buildQuestionnaireModel(List.of(questionModel));


        QuestionnaireDto result = subject.mapQuestionnaireModel(questionnaireModel);

        assertEquals(1, result.getQuestions().size());
        assertEquals(Optional.ofNullable(questionModel.linkId()), result.getQuestions().get(0).getThresholds().get(0).getQuestionId());

        assertEquals(2, result.getQuestions().get(0).getThresholds().size());
        var firstThreshold = result.getQuestions().get(0).getThresholds().get(0);
        assertNotNull(firstThreshold);
        assertEquals(ThresholdDto.TypeEnum.NORMAL, firstThreshold.getType().get());
        assertEquals(Optional.ofNullable(questionModel.linkId()), firstThreshold.getQuestionId());
        assertEquals(Optional.ofNullable(null), firstThreshold.getValueQuantityHigh());
        assertEquals(Optional.ofNullable(null), firstThreshold.getValueQuantityLow());
        assertEquals(Optional.ofNullable(Boolean.TRUE), firstThreshold.getValueBoolean());

        var secondThreshold = result.getQuestions().get(0).getThresholds().get(1);
        assertNotNull(secondThreshold);
        assertEquals(Optional.ofNullable(ThresholdDto.TypeEnum.NORMAL), secondThreshold.getType());
        assertEquals(Optional.ofNullable(questionModel.linkId()), secondThreshold.getQuestionId());
        assertEquals(Optional.ofNullable(2.0), secondThreshold.getValueQuantityLow());
        assertEquals(Optional.ofNullable(5.0), secondThreshold.getValueQuantityHigh());
        assertEquals(Optional.ofNullable(null), secondThreshold.getValueBoolean());
    }


    // TODO: Get rid of all these builders below
    private ThresholdModel buildBooleanThresholdModel(String questionLinkId) {
        return new ThresholdModel(
                questionLinkId,
                ThresholdType.NORMAL,
                null,
                null,
                Boolean.TRUE,
                null
        );
    }

    private ThresholdModel buildNumberThresholdModel(String questionLinkId) {
        return new ThresholdModel(
                questionLinkId,
                ThresholdType.NORMAL,
                2.0,
                5.0,
                null,
                null);
    }


    private AnswerModel buildAnswerModel() {
        return new AnswerModel(null, "foo", AnswerType.STRING, null);
    }

    private CarePlanDto buildCarePlanDto() {
        CarePlanDto carePlanDto = new CarePlanDto();

        carePlanDto.setId(Optional.of(CAREPLAN_ID_1));
        carePlanDto.setStatus(Optional.of("ACTIVE"));
        carePlanDto.setPatientDto(Optional.of(buildPatientDto()));
        carePlanDto.setQuestionnaires(List.of(buildQuestionnaireWrapperDto()));
        carePlanDto.setPlanDefinitions(List.of(buildPlanDefinitionDto()));

        return carePlanDto;
    }

    private CarePlanModel buildCarePlanModel() {
        return CarePlanModel.builder()
                .id(new QualifiedId(CAREPLAN_ID_1))
                .status(CarePlanStatus.ACTIVE)
                .patient(buildPatientModel())
                .questionnaires(List.of(buildQuestionnaireWrapperModel()))
                .planDefinitions(List.of(buildPlanDefinitionModel()))
                .build();
    }

    private ContactDetailsDto buildContactDetailsDto() {
        return new ContactDetailsDto().street("Fiskergade");
    }

    private ContactDetailsModel buildContactDetailsModel() {
        return ContactDetailsModel.builder().street("Fiskergade").build();
    }

    private FrequencyDto buildFrequencyDto() {
        return new FrequencyDto().weekdays(List.of(FrequencyDto.WeekdaysEnum.FRI)).timeOfDay("04:00");
    }

    private FrequencyModel buildFrequencyModel() {
        return FrequencyModel.builder()
                .weekdays(List.of(Weekday.FRI))
                .timeOfDay(LocalTime.parse("04:00"))
                .build();
    }

    private PatientDto buildPatientDto() {
        return new PatientDto()
                .cpr("0101010101")
                .patientContactDetails(buildContactDetailsDto())
                .primaryRelativeContactDetails(buildContactDetailsDto())
                .additionalRelativeContactDetails(List.of(buildContactDetailsDto()));
    }

    private PatientModel buildPatientModel() {
        return PatientModel.builder()
                .cpr("0101010101")
                .contactDetails(buildContactDetailsModel())
                .primaryContact(PrimaryContactModel.builder()
                        .contactDetails(buildContactDetailsModel())
                        .build())
                .contactDetails(buildContactDetailsModel())
                .additionalRelativeContactDetails(List.of(buildContactDetailsModel()))
                .build();

    }


    private PersonModel buildPersonModel() {
        return PersonModel.builder()
                .identifier(PersonIdentifierModel.builder()
                        .id("0101010101")
                        .build()
                )
                .name(new PersonNameModel("Dent", List.of("Arthur", "A.")))
                .birthDate("1980-05-05")
                .deceasedBoolean(false)
                .gender("M")
                .address(PersonAddressModel.builder()
                        .city("Arhus")
                        .build()
                ).build();
    }

    private PlanDefinitionDto buildPlanDefinitionDto() {
        return new PlanDefinitionDto()
                .id(PLANDEFINITION_ID_1)
                .status("ACTIVE")
                .questionnaires(List.of(buildQuestionnaireWrapperDto()));
    }

    private PlanDefinitionModel buildPlanDefinitionModel() {
        return PlanDefinitionModel.builder()
                .id(new QualifiedId(PLANDEFINITION_ID_1))
                .status(PlanDefinitionStatus.ACTIVE)
                .questionnaires(List.of(buildQuestionnaireWrapperModel()))
                .build();
    }

    private QuestionDto buildQuestionDto() {
        return new QuestionDto().text("Hvordan har du det?");
    }

    private QuestionModel buildQuestionModel() {
        return QuestionModel.builder()
                .text("Hvordan har du det?")
                .build();
    }

    private QuestionModel buildQuestionModel(List<QuestionModel.EnableWhen> enableWhens) {
        return QuestionModel.builder()
                .text("Hvordan har du det?")
                .enableWhens(enableWhens)
                .build();
    }


    private QuestionAnswerPairModel buildQuestionAnswerPairModel() {
        return new QuestionAnswerPairModel(buildQuestionModel(), buildAnswerModel());
    }

    private QuestionnaireDto buildQuestionnaireDto() {
        return new QuestionnaireDto()
                .questions(List.of(buildQuestionDto()))
                .status("DRAFT").id("questionnaire-1");
    }

    private QuestionnaireModel buildQuestionnaireModel() {
        return QuestionnaireModel.builder()
                .id(new QualifiedId(QUESTIONNAIRE_ID_1))
                .questions(List.of(buildQuestionModel()))
                .status(QuestionnaireStatus.DRAFT)
                .build();
    }

    private QuestionnaireModel buildQuestionnaireModel(QuestionModel callToAction) {
        return QuestionnaireModel.builder()
                .id(new QualifiedId(QUESTIONNAIRE_ID_1))
                .questions(List.of(buildQuestionModel()))
                .status(QuestionnaireStatus.DRAFT)
                .callToAction(callToAction)
                .build();
    }


    private QuestionnaireModel buildQuestionnaireModel(List<QuestionModel> questions) {
        return QuestionnaireModel.builder()
                .id(new QualifiedId(QUESTIONNAIRE_ID_1))
                .questions(List.of(buildQuestionModel()))
                .status(QuestionnaireStatus.DRAFT)
                .questions(questions)
                .build();
    }


    private QuestionnaireResponseModel buildQuestionnaireResponseModel() {
        return QuestionnaireResponseModel.builder()
                .id(new QualifiedId(QUESTIONNAIRERESPONSE_ID_1))
                .questionnaireId(new QualifiedId(QUESTIONNAIRE_ID_1))
                .carePlanId(new QualifiedId(CAREPLAN_ID_1))
                .questionAnswerPairs(List.of(buildQuestionAnswerPairModel()))
                .patient(buildPatientModel())
                .build();
    }


    private QuestionnaireWrapperDto buildQuestionnaireWrapperDto() {
        return new QuestionnaireWrapperDto()
                .questionnaire(buildQuestionnaireDto())
                .frequency(buildFrequencyDto());
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel() {
        return QuestionnaireWrapperModel.builder()
                .questionnaire(buildQuestionnaireModel())
                .frequency(buildFrequencyModel()).build();
    }
}