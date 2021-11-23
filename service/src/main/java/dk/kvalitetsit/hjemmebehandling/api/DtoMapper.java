package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.api.answer.AnswerDto;
import dk.kvalitetsit.hjemmebehandling.api.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import org.hl7.fhir.r4.model.Questionnaire;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DtoMapper {
    public CarePlanModel mapCarePlanDto(CarePlanDto carePlanDto) {
        CarePlanModel carePlanModel = new CarePlanModel();

        carePlanModel.setId(carePlanDto.getId());
        carePlanModel.setTitle(carePlanDto.getTitle());
        carePlanModel.setStatus(carePlanDto.getStatus());
        carePlanModel.setCreated(carePlanDto.getCreated());
        carePlanModel.setStartDate(carePlanDto.getStartDate());
        carePlanModel.setEndDate(carePlanDto.getEndDate());
        carePlanModel.setPatient(mapPatientDto(carePlanDto.getPatientDto()));
        if(carePlanDto.getQuestionnaires() != null) {
            carePlanModel.setQuestionnaires(carePlanDto.getQuestionnaires().stream().map(q -> mapQuestionnaireWrapperDto(q)).collect(Collectors.toList()));
        }
        if(carePlanDto.getPlanDefinitions() != null) {
            carePlanModel.setPlanDefinitions(carePlanDto.getPlanDefinitions().stream().map(pd -> mapPlanDefinitionDto(pd)).collect(Collectors.toList()));
        }
        carePlanModel.setQuestionnairesWithUnsatisfiedSchedule(carePlanDto.getQuestionnairesWithUnsatisfiedSchedule());

        return carePlanModel;
    }

    public CarePlanDto mapCarePlanModel(CarePlanModel carePlan) {
        CarePlanDto carePlanDto = new CarePlanDto();

        carePlanDto.setId(carePlan.getId());
        carePlanDto.setTitle(carePlan.getTitle());
        carePlanDto.setStatus(carePlan.getStatus());
        carePlanDto.setCreated(carePlan.getCreated());
        carePlanDto.setStartDate(carePlan.getStartDate());
        carePlanDto.setEndDate(carePlan.getEndDate());
        carePlanDto.setPatientDto(mapPatientModel(carePlan.getPatient()));
        carePlanDto.setQuestionnaires(carePlan.getQuestionnaires().stream().map(qw -> mapQuestionnaireWrapperModel(qw)).collect(Collectors.toList()));
        carePlanDto.setPlanDefinitions(carePlan.getPlanDefinitions().stream().map(pd -> mapPlanDefinitionModel(pd)).collect(Collectors.toList()));
        carePlanDto.setQuestionnairesWithUnsatisfiedSchedule(carePlan.getQuestionnairesWithUnsatisfiedSchedule());

        return carePlanDto;
    }

    public FrequencyModel mapFrequencyDto(FrequencyDto frequencyDto) {
        FrequencyModel frequencyModel = new FrequencyModel();

        frequencyModel.setWeekday(frequencyDto.getWeekday());

        return frequencyModel;
    }

    public FrequencyDto mapFrequencyModel(FrequencyModel frequencyModel) {
        FrequencyDto frequencyDto = new FrequencyDto();

        frequencyDto.setWeekday(frequencyModel.getWeekday());

        return frequencyDto;
    }

    public PatientModel mapPatientDto(PatientDto patient) {
        PatientModel patientModel = new PatientModel();

        patientModel.setCpr(patient.getCpr());
        patientModel.setFamilyName(patient.getFamilyName());
        patientModel.setGivenName(patient.getGivenName());
        if(patient.getPatientContactDetails() != null) {
            patientModel.setPatientContactDetails(mapContactDetailsDto(patient.getPatientContactDetails()));
        }
        if(patient.getPrimaryRelativeContactDetails() != null) {
            patientModel.setPrimaryRelativeContactDetails(mapContactDetailsDto(patient.getPrimaryRelativeContactDetails()));
        }
        if(patient.getAdditionalRelativeContactDetails() != null) {
            patientModel.setAdditionalRelativeContactDetails(patient.getAdditionalRelativeContactDetails().stream().map(cd -> mapContactDetailsDto(cd)).collect(Collectors.toList()));
        }

        return patientModel;
    }

    public PatientDto mapPatientModel(PatientModel patient) {
        PatientDto patientDto = new PatientDto();

        patientDto.setCpr(patient.getCpr());
        patientDto.setFamilyName(patient.getFamilyName());
        patientDto.setGivenName(patient.getGivenName());
        patientDto.setPatientContactDetails(mapContactDetailsModel(patient.getPatientContactDetails()));

        return patientDto;
    }

    public PlanDefinitionModel mapPlanDefinitionDto(PlanDefinitionDto planDefinitionDto) {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        planDefinitionModel.setId(planDefinitionDto.getId());
        planDefinitionModel.setName(planDefinitionDto.getName());
        planDefinitionModel.setTitle(planDefinitionDto.getTitle());
        // TODO - planDefinitionModel.getQuestionnaires() should never return null - but it can for now.
        if(planDefinitionDto.getQuestionnaires() != null) {
            planDefinitionModel.setQuestionnaires(planDefinitionDto.getQuestionnaires().stream().map(qw -> mapQuestionnaireWrapperDto(qw)).collect(Collectors.toList()));
        }

        return planDefinitionModel;
    }

    public PlanDefinitionDto mapPlanDefinitionModel(PlanDefinitionModel planDefinitionModel) {
        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();

        planDefinitionDto.setId(planDefinitionModel.getId());
        planDefinitionDto.setName(planDefinitionModel.getName());
        planDefinitionDto.setTitle(planDefinitionModel.getTitle());
        // TODO - planDefinitionModel.getQuestionnaires() should never return null - but it can for now.
        if(planDefinitionModel.getQuestionnaires() != null) {
            planDefinitionDto.setQuestionnaires(planDefinitionModel.getQuestionnaires().stream().map(qw -> mapQuestionnaireWrapperModel(qw)).collect(Collectors.toList()));
        }

        return planDefinitionDto;
    }
    
    public PersonDto mapPersonModel(PersonModel person) {
        PersonDto personDto = new PersonDto();

        personDto.setCpr(person.getIdentifier().getId());
        personDto.setFamilyName(person.getName().getFamily());
        personDto.setGivenName(String.join(" ", person.getName().getGiven()));
        personDto.setBirthDate(person.getBirthDate());
        personDto.setDeceasedBoolean(person.isDeceasedBoolean());
        personDto.setGender(person.getGender());
        
        personDto.setPatientContactDetails(new ContactDetailsDto());
        personDto.getPatientContactDetails().setCountry(person.getAddress().getCountry());
        personDto.getPatientContactDetails().setPostalCode(person.getAddress().getPostalCode());
        personDto.getPatientContactDetails().setStreet(person.getAddress().getLine());
        personDto.getPatientContactDetails().setCity(person.getAddress().getCity());

        return personDto;
    }

    public QuestionnaireModel mapQuestionnaireDto(QuestionnaireDto questionnaireDto) {
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();

        questionnaireModel.setId(questionnaireDto.getId());
        questionnaireModel.setTitle(questionnaireDto.getTitle());
        questionnaireModel.setStatus(questionnaireDto.getStatus());
        if(questionnaireDto.getQuestions() != null) {
            questionnaireModel.setQuestions(questionnaireDto.getQuestions().stream().map(q -> mapQuestionDto(q)).collect(Collectors.toList()));
        }

        return questionnaireModel;
    }

    public QuestionnaireDto mapQuestionnaireModel(QuestionnaireModel questionnaireModel) {
        QuestionnaireDto questionnaireDto = new QuestionnaireDto();

        questionnaireDto.setId(questionnaireModel.getId());
        questionnaireDto.setTitle(questionnaireModel.getTitle());
        questionnaireDto.setStatus(questionnaireModel.getStatus());
        if(questionnaireModel.getQuestions() != null) {
            questionnaireDto.setQuestions(questionnaireModel.getQuestions().stream().map(q -> mapQuestionModel(q)).collect(Collectors.toList()));
        }

        return questionnaireDto;
    }

    public QuestionnaireResponseDto mapQuestionnaireResponseModel(QuestionnaireResponseModel questionnaireResponseModel) {
        QuestionnaireResponseDto questionnaireResponseDto = new QuestionnaireResponseDto();

        questionnaireResponseDto.setId(questionnaireResponseModel.getId());
        questionnaireResponseDto.setQuestionnaireId(questionnaireResponseModel.getQuestionnaireId());
        questionnaireResponseDto.setQuestionnaireName(questionnaireResponseModel.getQuestionnaireName());
        questionnaireResponseDto.setQuestionAnswerPairs(questionnaireResponseModel.getQuestionAnswerPairs().stream().map(qa -> mapQuestionAnswerPairModel(qa)).collect(Collectors.toList()));
        questionnaireResponseDto.setAnswered(questionnaireResponseModel.getAnswered());
        questionnaireResponseDto.setExaminationStatus(questionnaireResponseModel.getExaminationStatus());
        questionnaireResponseDto.setTriagingCategory(questionnaireResponseModel.getTriagingCategory());
        questionnaireResponseDto.setPatient(mapPatientModel(questionnaireResponseModel.getPatient()));

        return questionnaireResponseDto;
    }

    private ContactDetailsModel mapContactDetailsDto(ContactDetailsDto contactDetails) {
        ContactDetailsModel contactDetailsModel = new ContactDetailsModel();

        contactDetailsModel.setCountry(contactDetails.getCountry());
        contactDetailsModel.setPrimaryPhone(contactDetails.getPrimaryPhone());
        contactDetailsModel.setSecondaryPhone(contactDetails.getSecondaryPhone());
        contactDetailsModel.setPostalCode(contactDetails.getPostalCode());
        contactDetailsModel.setStreet(contactDetails.getStreet());

        return contactDetailsModel;
    }

    private ContactDetailsDto mapContactDetailsModel(ContactDetailsModel contactDetails) {
        ContactDetailsDto contactDetailsDto = new ContactDetailsDto();

        contactDetailsDto.setCountry(contactDetails.getCountry());
        contactDetailsDto.setPrimaryPhone(contactDetails.getPrimaryPhone());
        contactDetailsDto.setSecondaryPhone(contactDetails.getSecondaryPhone());
        contactDetailsDto.setPostalCode(contactDetails.getPostalCode());
        contactDetailsDto.setStreet(contactDetails.getStreet());

        return contactDetailsDto;
    }

    private QuestionAnswerPairDto mapQuestionAnswerPairModel(QuestionAnswerPairModel questionAnswerPairModel) {
        QuestionAnswerPairDto questionAnswerPairDto = new QuestionAnswerPairDto();

        questionAnswerPairDto.setQuestion(mapQuestionModel(questionAnswerPairModel.getQuestion()));
        questionAnswerPairDto.setAnswer(mapAnswerModel(questionAnswerPairModel.getAnswer()));

        return questionAnswerPairDto;
    }

    private QuestionModel mapQuestionDto(QuestionDto questionDto) {
        QuestionModel questionModel = new QuestionModel();

        questionModel.setText(questionDto.getText());
        questionModel.setRequired(questionDto.getRequired());
        questionModel.setOptions(questionDto.getOptions());
        questionModel.setQuestionType(questionDto.getQuestionType());

        return questionModel;
    }

    private QuestionDto mapQuestionModel(QuestionModel questionModel) {
        QuestionDto questionDto = new QuestionDto();

        questionDto.setText(questionModel.getText());
        questionDto.setRequired(questionModel.isRequired());
        questionDto.setOptions(questionModel.getOptions());
        questionDto.setQuestionType(questionModel.getQuestionType());

        return questionDto;
    }

    private AnswerDto mapAnswerModel(AnswerModel answerModel) {
        AnswerDto answerDto = new AnswerDto();

        answerDto.setValue(answerModel.getValue());
        answerDto.setAnswerType(answerModel.getAnswerType());

        return answerDto;
    }

    private QuestionnaireWrapperModel mapQuestionnaireWrapperDto(QuestionnaireWrapperDto questionnaireWrapper) {
        QuestionnaireWrapperModel questionnaireWrapperModel = new QuestionnaireWrapperModel();

        questionnaireWrapperModel.setQuestionnaire(mapQuestionnaireDto(questionnaireWrapper.getQuestionnaire()));
        questionnaireWrapperModel.setFrequency(mapFrequencyDto(questionnaireWrapper.getFrequency()));

        return questionnaireWrapperModel;
    }

    private QuestionnaireWrapperDto mapQuestionnaireWrapperModel(QuestionnaireWrapperModel questionnaireWrapper) {
        QuestionnaireWrapperDto questionnaireWrapperDto = new QuestionnaireWrapperDto();

        questionnaireWrapperDto.setQuestionnaire(mapQuestionnaireModel(questionnaireWrapper.getQuestionnaire()));
        questionnaireWrapperDto.setFrequency(mapFrequencyModel(questionnaireWrapper.getFrequency()));

        return questionnaireWrapperDto;
    }
}
