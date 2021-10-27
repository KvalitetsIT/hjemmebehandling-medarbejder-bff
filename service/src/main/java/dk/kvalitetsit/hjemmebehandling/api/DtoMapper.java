package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.api.answer.AnswerDto;
import dk.kvalitetsit.hjemmebehandling.api.question.OptionQuestionDto;
import dk.kvalitetsit.hjemmebehandling.api.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.OptionQuestionModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class DtoMapper {
    public CarePlanDto mapCarePlanModel(CarePlanModel carePlan) {
        CarePlanDto carePlanDto = new CarePlanDto();

        carePlanDto.setId(carePlan.getId());
        carePlanDto.setTitle(carePlan.getTitle());
        carePlanDto.setStatus(carePlan.getStatus());
        carePlanDto.setPatientDto(mapPatientModel(carePlan.getPatient()));
        carePlanDto.setQuestionnaires(carePlan.getQuestionnaires().stream().map(qw -> mapQuestionnaireWrapperModel(qw)).collect(Collectors.toList()));

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

    public QuestionnaireDto mapQuestionnaireResponseModel(QuestionnaireModel questionnaireModel) {
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
        questionnaireResponseDto.setQuestionAnswerPairs(questionnaireResponseModel.getQuestionAnswerPairs().stream().map(qa -> mapQuestionAnswerPairModel(qa)).collect(Collectors.toList()));
        questionnaireResponseDto.setAnswered(questionnaireResponseModel.getAnswered());
        questionnaireResponseDto.setPatient(mapPatientModel(questionnaireResponseModel.getPatient()));

        return questionnaireResponseDto;
    }

    private ContactDetailsModel mapContactDetailsDto(ContactDetailsDto contactDetails) {
        ContactDetailsModel contactDetailsModel = new ContactDetailsModel();

        contactDetailsModel.setCountry(contactDetails.getCountry());
        contactDetailsModel.setEmailAddress(contactDetails.getEmailAddress());
        contactDetailsModel.setPrimaryPhone(contactDetails.getPrimaryPhone());
        contactDetailsModel.setSecondaryPhone(contactDetails.getSecondaryPhone());
        contactDetailsModel.setPostalCode(contactDetails.getPostalCode());
        contactDetailsModel.setStreet(contactDetails.getStreet());

        return contactDetailsModel;
    }

    private ContactDetailsDto mapContactDetailsModel(ContactDetailsModel contactDetails) {
        ContactDetailsDto contactDetailsDto = new ContactDetailsDto();

        contactDetailsDto.setCountry(contactDetails.getCountry());
        contactDetailsDto.setEmailAddress(contactDetails.getEmailAddress());
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

    private QuestionDto mapQuestionModel(QuestionModel questionModel) {
        QuestionDto questionDto = null;

        String type = questionModel.getClass().getSimpleName();
        switch(type) {
            case "OptionQuestionModel":
                questionDto = mapOptionQuestionAttributes((OptionQuestionModel) questionModel);
                break;
            default:
                throw new UnsupportedOperationException(String.format("Don't know how to map type %s!", type));
        }

        questionDto.setText(questionModel.getText());
        questionDto.setRequired(questionModel.isRequired());

        return questionDto;
    }

    private QuestionDto mapOptionQuestionAttributes(OptionQuestionModel questionModel) {
        OptionQuestionDto questionDto = new OptionQuestionDto();

        questionDto.setOptions(questionModel.getOptions());

        return questionDto;
    }

    private AnswerDto mapAnswerModel(AnswerModel answerModel) {
        AnswerDto answerDto = new AnswerDto();

        answerDto.setValue(answerModel.getValue());

        return answerDto;
    }

    private QuestionnaireWrapperDto mapQuestionnaireWrapperModel(QuestionnaireWrapperModel questionnaireWrapper) {
        QuestionnaireWrapperDto questionnaireWrapperDto = new QuestionnaireWrapperDto();

        questionnaireWrapperDto.setQuestionnaire(mapQuestionnaireResponseModel(questionnaireWrapper.getQuestionnaire()));
        questionnaireWrapperDto.setFrequency(mapFrequencyModel(questionnaireWrapper.getFrequency()));

        return questionnaireWrapperDto;
    }
}
