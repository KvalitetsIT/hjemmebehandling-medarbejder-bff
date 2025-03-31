package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;
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

        assertEquals(planDefinitionDto.getId().get(), result.getId().toString());
    }

    @Test
    public void mapPlanDefinitionModel_success() {
        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel();

        PlanDefinitionDto result = subject.mapPlanDefinitionModel(planDefinitionModel);

        assertEquals(planDefinitionModel.getId().toString(), result.getId().get());
    }

    @Test
    public void mapCarePlanDto_success() {
        CarePlanDto carePlanDto = buildCarePlanDto();

        CarePlanModel result = subject.mapCarePlanDto(carePlanDto);

        assertEquals(carePlanDto.getId().get(), result.getId().toString());
    }

    @Test
    public void mapCarePlanModel_success() {
        CarePlanModel carePlanModel = buildCarePlanModel();

        CarePlanDto result = subject.mapCarePlanModel(carePlanModel);

        assertEquals(Optional.ofNullable(carePlanModel.getId().toString()), result.getId());
    }

    @Test
    public void mapPatientDto_success() {
        PatientDto patientDto = buildPatientDto();

        PatientModel result = subject.mapPatientDto(patientDto);

        assertEquals(patientDto.getCpr().get(), result.getCpr());
    }

    @Test
    public void mapPatientModel_success() {
        PatientModel patientModel = buildPatientModel();

        PatientDto result = subject.mapPatientModel(patientModel);

        assertEquals(patientModel.getCpr(), result.getCpr().get());
    }

    @Test
    public void mapPersonModel_success() {
        PersonModel personModel = buildPersonModel();

        PersonDto result = subject.mapPersonModel(personModel);

        assertEquals(personModel.getIdentifier().getId(), result.getCpr().get());
        assertEquals("Arthur A.", result.getGivenName().get());
    }

    @Test
    public void mapQuestionnaireResponseModel_success() {
        QuestionnaireResponseModel questionnaireResponseModel = buildQuestionnaireResponseModel();

        QuestionnaireResponseDto result = subject.mapQuestionnaireResponseModel(questionnaireResponseModel);

        assertEquals(questionnaireResponseModel.getId().toString(), result.getId());
    }

    @Test
    public void mapQuestionnaireModel_callToAction() {
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel();
        questionnaireModel.setCallToAction(buildQuestionModel());

        QuestionnaireDto result = subject.mapQuestionnaireModel(questionnaireModel);

        assertNotNull(result.getCallToAction());
    }

    @Test
    public void mapQuestionnaireModel_enableWhen() {
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel();

        QuestionModel questionModel = buildQuestionModel();
        questionModel.setEnableWhens(List.of(new QuestionModel.EnableWhen()));
        questionnaireModel.setQuestions(List.of(questionModel));

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

        assertEquals(1, result.getQuestions().size());
        assertEquals(1, result.getQuestions().get(0).getEnableWhens().size());
    }


    @Test
    public void mapFrequencyModel_allValuesAreNull_noErrors() {
        var toMap = new FrequencyModel();
        var result = subject.mapFrequencyModel(toMap);
        assertNotNull(result);
    }

    @Test
    public void mapMeasurementTypeModel_success() {
        MeasurementTypeModel measurementTypeModel = new MeasurementTypeModel();
        measurementTypeModel.setSystem("system");
        measurementTypeModel.setCode("code");
        measurementTypeModel.setDisplay("display");

        MeasurementTypeDto result = subject.mapMeasurementTypeModel(measurementTypeModel);

        assertEquals(measurementTypeModel.getSystem(), result.getSystem().get());
        assertEquals(measurementTypeModel.getCode(), result.getCode().get());
        assertEquals(measurementTypeModel.getDisplay(), result.getDisplay().get());
    }

    @Test
    public void mapQuestionnaireModel_thresholds() {
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel();
        QuestionModel questionModel = buildQuestionModel();
        questionModel.setThresholds(List.of(buildBooleanThresholdModel(questionModel.getLinkId()), buildNumberThresholdModel(questionModel.getLinkId())));
        questionnaireModel.setQuestions(List.of(questionModel));

        QuestionnaireDto result = subject.mapQuestionnaireModel(questionnaireModel);

        assertEquals(1, result.getQuestions().size());
        assertEquals(Optional.ofNullable(questionModel.getLinkId()), result.getQuestions().get(0).getThresholds().get(0).getQuestionId());

        assertEquals(2, result.getQuestions().get(0).getThresholds().size());
        var firstThreshold = result.getQuestions().get(0).getThresholds().get(0);
        assertNotNull(firstThreshold);
        assertEquals(ThresholdDto.TypeEnum.NORMAL, firstThreshold.getType().get());
        assertEquals(Optional.ofNullable(questionModel.getLinkId()), firstThreshold.getQuestionId());
        assertEquals(Optional.ofNullable(null), firstThreshold.getValueQuantityHigh());
        assertEquals(Optional.ofNullable(null), firstThreshold.getValueQuantityLow());
        assertEquals(Optional.ofNullable(Boolean.TRUE), firstThreshold.getValueBoolean());

        var secondThreshold = result.getQuestions().get(0).getThresholds().get(1);
        assertNotNull(secondThreshold);
        assertEquals(Optional.ofNullable(ThresholdDto.TypeEnum.NORMAL), secondThreshold.getType());
        assertEquals(Optional.ofNullable(questionModel.getLinkId()), secondThreshold.getQuestionId());
        assertEquals(Optional.ofNullable(2.0), secondThreshold.getValueQuantityLow());
        assertEquals(Optional.ofNullable(5.0), secondThreshold.getValueQuantityHigh());
        assertEquals(Optional.ofNullable(null), secondThreshold.getValueBoolean());
    }

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
        CarePlanModel carePlanModel = new CarePlanModel();

        carePlanModel.setId(new QualifiedId(CAREPLAN_ID_1));
        carePlanModel.setStatus(CarePlanStatus.ACTIVE);
        carePlanModel.setPatient(buildPatientModel());
        carePlanModel.setQuestionnaires(List.of(buildQuestionnaireWrapperModel()));
        carePlanModel.setPlanDefinitions(List.of(buildPlanDefinitionModel()));

        return carePlanModel;
    }

    private ContactDetailsDto buildContactDetailsDto() {
        return new ContactDetailsDto().street("Fiskergade");
    }

    private ContactDetailsModel buildContactDetailsModel() {
        ContactDetailsModel contactDetailsModel = new ContactDetailsModel();
        contactDetailsModel.setStreet("Fiskergade");
        return contactDetailsModel;
    }

    private FrequencyDto buildFrequencyDto() {
        return new FrequencyDto().weekdays(List.of(FrequencyDto.WeekdaysEnum.FRI)).timeOfDay("04:00");
    }

    private FrequencyModel buildFrequencyModel() {
        FrequencyModel frequencyModel = new FrequencyModel();
        frequencyModel.setWeekdays(List.of(Weekday.FRI));
        frequencyModel.setTimeOfDay(LocalTime.parse("04:00"));
        return frequencyModel;
    }

    private PatientDto buildPatientDto() {
        return new PatientDto()
                .cpr("0101010101")
                .patientContactDetails(buildContactDetailsDto())
                .primaryRelativeContactDetails(buildContactDetailsDto())
                .additionalRelativeContactDetails(List.of(buildContactDetailsDto()));
    }

    private PatientModel buildPatientModel() {
        PatientModel patientModel = new PatientModel();
        patientModel.setCpr("0101010101");
        patientModel.setContactDetails(buildContactDetailsModel());
        patientModel.getPrimaryContact().setContactDetails(buildContactDetailsModel());
        patientModel.setAdditionalRelativeContactDetails(List.of(buildContactDetailsModel()));
        return patientModel;
    }

    private PersonModel buildPersonModel() {
        PersonModel personModel = new PersonModel();
        personModel.setIdentifier(new PersonIdentifierModel());
        personModel.getIdentifier().setId("0101010101");
        personModel.setName(new PersonNameModel());
        personModel.getName().setFamily("Dent");
        personModel.getName().setGiven(List.of("Arthur", "A."));
        personModel.setBirthDate("1980-05-05");
        personModel.setDeceasedBoolean(false);
        personModel.setGender("M");
        personModel.setAddress(new PersonAddressModel());
        personModel.getAddress().setCity("Aarhus");
        return personModel;
    }

    private PlanDefinitionDto buildPlanDefinitionDto() {
        return new PlanDefinitionDto()
                .id(PLANDEFINITION_ID_1)
                .status("ACTIVE")
                .questionnaires(List.of(buildQuestionnaireWrapperDto()));
    }

    private PlanDefinitionModel buildPlanDefinitionModel() {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        planDefinitionModel.setId(new QualifiedId(PLANDEFINITION_ID_1));
        planDefinitionModel.setStatus(PlanDefinitionStatus.ACTIVE);
        planDefinitionModel.setQuestionnaires(List.of(buildQuestionnaireWrapperModel()));
        return planDefinitionModel;
    }

    private QuestionDto buildQuestionDto() {
        return new QuestionDto().text("Hvordan har du det?");
    }

    private QuestionModel buildQuestionModel() {
        QuestionModel questionModel = new QuestionModel();
        questionModel.setText("Hvordan har du det?");
        return questionModel;
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
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        questionnaireModel.setId(new QualifiedId(QUESTIONNAIRE_ID_1));
        questionnaireModel.setQuestions(List.of(buildQuestionModel()));
        questionnaireModel.setStatus(QuestionnaireStatus.DRAFT);
        return questionnaireModel;
    }

    private QuestionnaireResponseModel buildQuestionnaireResponseModel() {
        QuestionnaireResponseModel questionnaireResponseModel = new QuestionnaireResponseModel();
        questionnaireResponseModel.setId(new QualifiedId(QUESTIONNAIRERESPONSE_ID_1));
        questionnaireResponseModel.setQuestionnaireId(new QualifiedId(QUESTIONNAIRE_ID_1));
        questionnaireResponseModel.setCarePlanId(new QualifiedId(CAREPLAN_ID_1));
        questionnaireResponseModel.setQuestionAnswerPairs(List.of(buildQuestionAnswerPairModel()));
        questionnaireResponseModel.setPatient(buildPatientModel());
        return questionnaireResponseModel;
    }

    private QuestionnaireWrapperDto buildQuestionnaireWrapperDto() {
        return new QuestionnaireWrapperDto()
                .questionnaire(buildQuestionnaireDto())
                .frequency(buildFrequencyDto());
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel() {
        QuestionnaireWrapperModel questionnaireWrapperModel = new QuestionnaireWrapperModel();
        questionnaireWrapperModel.setQuestionnaire(buildQuestionnaireModel());
        questionnaireWrapperModel.setFrequency(buildFrequencyModel());
        return questionnaireWrapperModel;
    }
}