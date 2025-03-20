package dk.kvalitetsit.hjemmebehandling.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kvalitetsit.hjemmebehandling.api.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;
import dk.kvalitetsit.hjemmebehandling.constants.PlanDefinitionStatus;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import org.junit.jupiter.api.Test;
import org.openapitools.model.ContactDetailsDto;
import org.openapitools.model.MeasurementTypeDto;
import org.openapitools.model.PatientDto;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DtoMapperTest {
    private DtoMapper subject = new DtoMapper();

    private static final String CAREPLAN_ID_1 = "CarePlan/careplan-1";
    private static final String ORGANIZATION_ID_1 = "Organization/organization-1";
    private static final String PATIENT_ID_1 = "Patient/patient-1";
    private static final String PLANDEFINITION_ID_1 = "PlanDefinition/plandefinition-1";
    private static final String QUESTIONNAIRE_ID_1 = "Questionnaire/questionnaire-1";
    private static final String QUESTIONNAIRERESPONSE_ID_1 = "QuestionnaireResponse/questionnaireresponse-1";

    @Test
    public void mapPlanDefinitionDto_success() {
        // Arrange
        PlanDefinitionDto planDefinitionDto = buildPlanDefinitionDto();
        // Act
        PlanDefinitionModel result = subject.mapPlanDefinitionDto(planDefinitionDto);

        // Assert
        assertEquals(planDefinitionDto.getId(), result.getId().toString());
    }

    @Test
    public void mapPlanDefinitionModel_success() {
        // Arrange
        PlanDefinitionModel planDefinitionModel = buildPlanDefinitionModel();

        // Act
        PlanDefinitionDto result = subject.mapPlanDefinitionModel(planDefinitionModel);

        // Assert
        assertEquals(planDefinitionModel.getId().toString(), result.getId());
    }

    @Test
    public void mapCarePlanDto_success() {
        // Arrange
        CarePlanDto carePlanDto = buildCarePlanDto();

        // Act
        CarePlanModel result = subject.mapCarePlanDto(carePlanDto);

        // Assert
        assertEquals(carePlanDto.getId(), result.getId().toString());
    }

    @Test
    public void mapCarePlanModel_success() {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel();

        // Act
        CarePlanDto result = subject.mapCarePlanModel(carePlanModel);

        // Assert
        assertEquals(carePlanModel.getId().toString(), result.getId());
    }

    @Test
    public void mapPatientDto_success() {
        // Arrange
        PatientDto patientDto = buildPatientDto();

        // Act
        PatientModel result = subject.mapPatientDto(patientDto);

        // Assert
        assertEquals(patientDto.getCpr(), result.getCpr());
    }

    @Test
    public void mapPatientModel_success() {
        // Arrange
        PatientModel patientModel = buildPatientModel();

        // Act
        PatientDto result = subject.mapPatientModel(patientModel);

        // Assert
        assertEquals(patientModel.getCpr(), result.getCpr());
    }

    @Test
    public void mapPersonModel_success() {
        // Arrange
        PersonModel personModel = buildPersonModel();

        // Act
        PersonDto result = subject.mapPersonModel(personModel);

        // Assert
        assertEquals(personModel.getIdentifier().getId(), result.getCpr());
        assertEquals("Arthur A.", result.getGivenName());
    }

    @Test
    public void mapQuestionnaireResponseModel_success() {
        // Arrange
        QuestionnaireResponseModel questionnaireResponseModel = buildQuestionnaireResponseModel();

        // Act
        QuestionnaireResponseDto result = subject.mapQuestionnaireResponseModel(questionnaireResponseModel);

        // Assert
        assertEquals(questionnaireResponseModel.getId().toString(), result.getId());
    }

    @Test
    public void mapQuestionnaireModel_callToAction() {
        // Arrange
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel();
        questionnaireModel.setCallToAction(buildQuestionModel());

        // Act
        QuestionnaireDto result = subject.mapQuestionnaireModel(questionnaireModel);

        // Assert
        assertNotNull(result.getCallToAction());
    }

        QuestionnaireModel questionnaireModel = buildQuestionnaireModel();
    @Test
    public void mapQuestionnaireModel_enableWhen() {
        // Arrange
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel();

        QuestionModel questionModel = buildQuestionModel();
        questionModel.setEnableWhens( List.of(buildEnableWhen()));
        questionnaireModel.setQuestions( List.of(questionModel) );

        // Act
        QuestionnaireDto result = subject.mapQuestionnaireModel(questionnaireModel);

        // Assert
        assertEquals(1, result.getQuestions().size());
        assertEquals(1, result.getQuestions().get(0).getEnableWhen().size());
    }

    @Test
    public void mapQuestionnaireDto_enableWhen() {
        // Arrange
        QuestionnaireDto questionnaireDto = new QuestionnaireDto();

        QuestionDto questionDto = buildQuestionDto();
        questionDto.setEnableWhen( List.of(buildEnableWhen()) );
        questionnaireDto.setQuestions( List.of(questionDto) );

        // Act
        QuestionnaireModel result = subject.mapQuestionnaireDto(questionnaireDto);

        // Assert
        assertEquals(1, result.getQuestions().size());
        assertEquals(1, result.getQuestions().get(0).getEnableWhens().size());
    }

    @Test
    public void mapQuestionDto_includeSubQuestions() throws JsonProcessingException {
        // Arrange
        QuestionDto questionDto = buildQuestionDto();

        QuestionDto subQuestion1 = new QuestionDto();
        subQuestion1.setText("SubQuestion1");

        QuestionDto subQuestion2 = new QuestionDto();
        subQuestion2.setText("SubQuestion2");

        var subQuestions = List.of(subQuestion1, subQuestion2);


        questionDto.setSubQuestions(subQuestions);
        String json = new ObjectMapper().writeValueAsString(questionDto);

        var expected = """
                {"linkId":null,"text":"Hvordan har du det?","abbreviation":null,"helperText":null,"required":false,"questionType":null,"options":null,"enableWhen":null,"thresholds":null,"measurementType":null,"subQuestions":[{"linkId":null,"text":"SubQuestion1","abbreviation":null,"helperText":null,"required":false,"questionType":null,"options":null,"enableWhen":null,"thresholds":null,"measurementType":null,"subQuestions":null,"deprecated":false},{"linkId":null,"text":"SubQuestion2","abbreviation":null,"helperText":null,"required":false,"questionType":null,"options":null,"enableWhen":null,"thresholds":null,"measurementType":null,"subQuestions":null,"deprecated":false}],"deprecated":false}""";

        assertEquals(expected, json);

    }

    @Test
    public void mapFrequencyModel_allValuesAreNull_noErrors(){
        var toMap = new FrequencyModel();
        var result = subject.mapFrequencyModel(toMap);
        assertNotNull(result);

    }

    @Test
    public void mapMeasurementTypeModel_success() {
        // Arrange
        MeasurementTypeModel measurementTypeModel = new MeasurementTypeModel();
        measurementTypeModel.setSystem("system");
        measurementTypeModel.setCode("code");
        measurementTypeModel.setDisplay("display");
   
        // Act
        MeasurementTypeDto result = subject.mapMeasurementTypeModel(measurementTypeModel);

        // Assert
        assertEquals(measurementTypeModel.getSystem(), result.getSystem());
        assertEquals(measurementTypeModel.getCode(), result.getCode());
        assertEquals(measurementTypeModel.getDisplay(), result.getDisplay());
    }

 @Test
    public void mapQuestionnaireModel_thresholds() {
        // Arrange
        QuestionnaireModel questionnaireModel = buildQuestionnaireModel();
        QuestionModel questionModel = buildQuestionModel();
        questionModel.setThresholds( List.of(buildBooleanThresholdModel(questionModel.getLinkId()), buildNumberThresholdModel(questionModel.getLinkId())) );
        questionnaireModel.setQuestions( List.of(questionModel) );

        // Act
        QuestionnaireDto result = subject.mapQuestionnaireModel(questionnaireModel);

        // Assert
        assertEquals(1, result.getQuestions().size());
        assertEquals(questionModel.getLinkId(), result.getQuestions().get(0).getThresholds().get(0).getQuestionId());

        assertEquals(2, result.getQuestions().get(0).getThresholds().size());
        var firstThreshold = result.getQuestions().get(0).getThresholds().get(0);
        assertNotNull(firstThreshold);
        assertEquals(ThresholdType.NORMAL,firstThreshold.getType());
        assertEquals(questionModel.getLinkId(),firstThreshold.getQuestionId());
        assertEquals(null,firstThreshold.getValueQuantityHigh());
        assertEquals(null,firstThreshold.getValueQuantityLow());
        assertEquals(Boolean.TRUE,firstThreshold.getValueBoolean());

         var secondThreshold = result.getQuestions().get(0).getThresholds().get(1);
         assertNotNull(secondThreshold);
         assertEquals(ThresholdType.NORMAL,secondThreshold.getType());
         assertEquals(questionModel.getLinkId(),secondThreshold.getQuestionId());
         assertEquals(2.0,secondThreshold.getValueQuantityLow());
         assertEquals(5.0,secondThreshold.getValueQuantityHigh());
         assertEquals(null,secondThreshold.getValueBoolean());

    }

    private ThresholdModel buildBooleanThresholdModel(String questionLinkId) {
        ThresholdModel thresholdModel = new ThresholdModel();
        thresholdModel.setType(ThresholdType.NORMAL);
        thresholdModel.setQuestionnaireItemLinkId(questionLinkId);
        thresholdModel.setValueBoolean(Boolean.TRUE);

        return thresholdModel;
    }

    private ThresholdModel buildNumberThresholdModel(String questionLinkId) {
        ThresholdModel thresholdModel = new ThresholdModel();
        thresholdModel.setType(ThresholdType.NORMAL);
        thresholdModel.setQuestionnaireItemLinkId(questionLinkId);
        thresholdModel.setValueQuantityLow(2.0);
        thresholdModel.setValueQuantityHigh(5.0);

        return thresholdModel;
    }


    private AnswerModel buildAnswerModel() {
        AnswerModel answerModel = new AnswerModel();

        answerModel.setAnswerType(AnswerType.STRING);
        answerModel.setValue("foo");

        return answerModel;
    }

    private CarePlanDto buildCarePlanDto() {
        CarePlanDto carePlanDto = new CarePlanDto();

        carePlanDto.setId(CAREPLAN_ID_1);
        carePlanDto.setStatus("ACTIVE");
        carePlanDto.setPatientDto(buildPatientDto());
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
        ContactDetailsDto contactDetailsDto = new ContactDetailsDto();

        contactDetailsDto.setStreet("Fiskergade");

        return contactDetailsDto;
    }

    private ContactDetailsModel buildContactDetailsModel() {
        ContactDetailsModel contactDetailsModel = new ContactDetailsModel();

        contactDetailsModel.setStreet("Fiskergade");

        return contactDetailsModel;
    }

    private FrequencyDto buildFrequencyDto() {
        FrequencyDto frequencyDto = new FrequencyDto();

        frequencyDto.setWeekdays(List.of(Weekday.FRI));
        frequencyDto.setTimeOfDay("04:00");

        return frequencyDto;
    }

    private FrequencyModel buildFrequencyModel() {
        FrequencyModel frequencyModel = new FrequencyModel();

        frequencyModel.setWeekdays(List.of(Weekday.FRI));
        frequencyModel.setTimeOfDay(LocalTime.parse("04:00"));

        return frequencyModel;
    }

    private PatientDto buildPatientDto() {
        PatientDto patientDto = new PatientDto();

        patientDto.setCpr("0101010101");
        patientDto.setPatientContactDetails(buildContactDetailsDto());
        patientDto.setPrimaryRelativeContactDetails(buildContactDetailsDto());
        patientDto.setAdditionalRelativeContactDetails(List.of(buildContactDetailsDto()));

        return patientDto;
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
        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();

        planDefinitionDto.setId(PLANDEFINITION_ID_1);
        planDefinitionDto.setStatus("ACTIVE");
        planDefinitionDto.setQuestionnaires(List.of(buildQuestionnaireWrapperDto()));

        return planDefinitionDto;
    }

    private PlanDefinitionModel buildPlanDefinitionModel() {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        planDefinitionModel.setId(new QualifiedId(PLANDEFINITION_ID_1));
        planDefinitionModel.setStatus(PlanDefinitionStatus.ACTIVE);
        planDefinitionModel.setQuestionnaires(List.of(buildQuestionnaireWrapperModel()));

        return planDefinitionModel;
    }

    private QuestionDto buildQuestionDto() {
        QuestionDto questionDto = new QuestionDto();

        questionDto.setText("Hvordan har du det?");

        return questionDto;
    }

    private QuestionModel buildQuestionModel() {
        QuestionModel questionModel = new QuestionModel();

        questionModel.setText("Hvordan har du det?");

        return questionModel;
    }

    private QuestionModel.EnableWhen buildEnableWhen() {
        QuestionModel.EnableWhen enableWhen = new QuestionModel.EnableWhen();

        return enableWhen;
    }

    private QuestionAnswerPairModel buildQuestionAnswerPairModel() {
        QuestionAnswerPairModel questionAnswerPairModel = new QuestionAnswerPairModel(buildQuestionModel(), buildAnswerModel());

        return questionAnswerPairModel;
    }

    private QuestionnaireDto buildQuestionnaireDto() {
        QuestionnaireDto questionnaireDto = new QuestionnaireDto();

        questionnaireDto.setId("questionnaire-1");
        questionnaireDto.setQuestions(List.of(buildQuestionDto()));
        questionnaireDto.setStatus("DRAFT");

        return questionnaireDto;
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
        QuestionnaireWrapperDto questionnaireWrapperDto = new QuestionnaireWrapperDto();

        questionnaireWrapperDto.setQuestionnaire(buildQuestionnaireDto());
        questionnaireWrapperDto.setFrequency(buildFrequencyDto());

        return questionnaireWrapperDto;
    }

    private QuestionnaireWrapperModel buildQuestionnaireWrapperModel() {
        QuestionnaireWrapperModel questionnaireWrapperModel = new QuestionnaireWrapperModel();

        questionnaireWrapperModel.setQuestionnaire(buildQuestionnaireModel());
        questionnaireWrapperModel.setFrequency(buildFrequencyModel());

        return questionnaireWrapperModel;
    }
}