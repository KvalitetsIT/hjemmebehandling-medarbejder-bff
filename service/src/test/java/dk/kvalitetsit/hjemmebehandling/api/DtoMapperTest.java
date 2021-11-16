package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.api.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DtoMapperTest {
    private DtoMapper subject = new DtoMapper();

    @Test
    public void mapCarePlanDto_success() {
        // Arrange
        CarePlanDto carePlanDto = buildCarePlanDto();

        // Act
        CarePlanModel result = subject.mapCarePlanDto(carePlanDto);

        // Assert
        assertEquals(carePlanDto.getId(), result.getId());
    }

    @Test
    public void mapCarePlanModel_success() {
        // Arrange
        CarePlanModel carePlanModel = buildCarePlanModel();

        // Act
        CarePlanDto result = subject.mapCarePlanModel(carePlanModel);

        // Assert
        assertEquals(carePlanModel.getId(), result.getId());
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
        assertEquals(questionnaireResponseModel.getId(), result.getId());
    }

    private AnswerModel buildAnswerModel() {
        AnswerModel answerModel = new AnswerModel();

        answerModel.setAnswerType(AnswerType.STRING);
        answerModel.setValue("foo");

        return answerModel;
    }

    private CarePlanDto buildCarePlanDto() {
        CarePlanDto carePlanDto = new CarePlanDto();

        carePlanDto.setId("careplan-1");
        carePlanDto.setPatientDto(buildPatientDto());
        carePlanDto.setQuestionnaires(List.of(buildQuestionnaireWrapperDto()));
        carePlanDto.setPlanDefinitions(List.of(buildPlanDefinitionDto()));

        return carePlanDto;
    }

    private CarePlanModel buildCarePlanModel() {
        CarePlanModel carePlanModel = new CarePlanModel();

        carePlanModel.setId("careplan-1");
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

        frequencyDto.setWeekday(Weekday.FRI);

        return frequencyDto;
    }

    private FrequencyModel buildFrequencyModel() {
        FrequencyModel frequencyModel = new FrequencyModel();

        frequencyModel.setWeekday(Weekday.FRI);

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
        patientModel.setPatientContactDetails(buildContactDetailsModel());
        patientModel.setPrimaryRelativeContactDetails(buildContactDetailsModel());
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

        planDefinitionDto.setQuestionnaires(List.of(buildQuestionnaireWrapperDto()));

        return planDefinitionDto;
    }

    private PlanDefinitionModel buildPlanDefinitionModel() {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

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

    private QuestionAnswerPairModel buildQuestionAnswerPairModel() {
        QuestionAnswerPairModel questionAnswerPairModel = new QuestionAnswerPairModel(buildQuestionModel(), buildAnswerModel());

        return questionAnswerPairModel;
    }

    private QuestionnaireDto buildQuestionnaireDto() {
        QuestionnaireDto questionnaireDto = new QuestionnaireDto();

        questionnaireDto.setId("questionnaire-1");
        questionnaireDto.setQuestions(List.of(buildQuestionDto()));

        return questionnaireDto;
    }

    private QuestionnaireModel buildQuestionnaireModel() {
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();

        questionnaireModel.setId("questionnaire-1");
        questionnaireModel.setQuestions(List.of(buildQuestionModel()));

        return questionnaireModel;
    }

    private QuestionnaireResponseModel buildQuestionnaireResponseModel() {
        QuestionnaireResponseModel questionnaireResponseModel = new QuestionnaireResponseModel();

        questionnaireResponseModel.setId("questionnaireresponse-1");
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