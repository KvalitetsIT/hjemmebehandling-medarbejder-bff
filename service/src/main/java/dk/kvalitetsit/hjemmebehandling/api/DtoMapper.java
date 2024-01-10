package dk.kvalitetsit.hjemmebehandling.api;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import dk.kvalitetsit.hjemmebehandling.constants.*;
import dk.kvalitetsit.hjemmebehandling.model.MeasurementTypeModel;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Component;

import dk.kvalitetsit.hjemmebehandling.api.answer.AnswerDto;
import dk.kvalitetsit.hjemmebehandling.api.question.QuestionDto;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.BaseModel;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.ContactDetailsModel;
import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.PersonModel;
import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionAnswerPairModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireWrapperModel;
import dk.kvalitetsit.hjemmebehandling.model.ThresholdModel;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;

@Component
public class DtoMapper {
    public CarePlanModel mapCarePlanDto(CarePlanDto carePlanDto) {
        CarePlanModel carePlanModel = new CarePlanModel();

        mapBaseAttributesToModel(carePlanModel, carePlanDto, ResourceType.CarePlan);

        carePlanModel.setTitle(carePlanDto.getTitle());
        if(carePlanDto.getStatus() != null) {
            carePlanModel.setStatus(Enum.valueOf(CarePlanStatus.class, carePlanDto.getStatus()));
        }
        carePlanModel.setCreated(carePlanDto.getCreated());
        carePlanModel.setStartDate(carePlanDto.getStartDate());
        carePlanModel.setEndDate(carePlanDto.getEndDate());
        carePlanModel.setPatient(mapPatientDto(carePlanDto.getPatientDto()));
        carePlanModel.setQuestionnaires(List.of());
        if(carePlanDto.getQuestionnaires() != null) {
            carePlanModel.setQuestionnaires(carePlanDto.getQuestionnaires().stream().map(this::mapQuestionnaireWrapperDto).collect(Collectors.toList()));
        }
        carePlanModel.setPlanDefinitions(List.of());
        if(carePlanDto.getPlanDefinitions() != null) {
            carePlanModel.setPlanDefinitions(carePlanDto.getPlanDefinitions().stream().map(this::mapPlanDefinitionDto).collect(Collectors.toList()));
        }
        carePlanModel.setDepartmentName(carePlanDto.getDepartmentName());

        return carePlanModel;
    }

    public CarePlanDto mapCarePlanModel(CarePlanModel carePlan) {
        CarePlanDto carePlanDto = new CarePlanDto();

        carePlanDto.setId(carePlan.getId().toString());
        carePlanDto.setTitle(carePlan.getTitle());
        carePlanDto.setStatus(carePlan.getStatus().toString());
        carePlanDto.setCreated(carePlan.getCreated());
        carePlanDto.setStartDate(carePlan.getStartDate());
        carePlanDto.setEndDate(carePlan.getEndDate());
        carePlanDto.setPatientDto(mapPatientModel(carePlan.getPatient()));
        carePlanDto.setQuestionnaires(carePlan.getQuestionnaires().stream().map(this::mapQuestionnaireWrapperModel).collect(Collectors.toList()));
        carePlanDto.setPlanDefinitions(carePlan.getPlanDefinitions().stream().map(this::mapPlanDefinitionModel).collect(Collectors.toList()));
        carePlanDto.setDepartmentName(carePlan.getDepartmentName());

        return carePlanDto;
    }

    public ContactDetailsModel mapContactDetailsDto(ContactDetailsDto contactDetails) {
        ContactDetailsModel contactDetailsModel = new ContactDetailsModel();

        contactDetailsModel.setCountry(contactDetails.getCountry());
        contactDetailsModel.setCity(contactDetails.getCity());
        contactDetailsModel.setPrimaryPhone(contactDetails.getPrimaryPhone());
        contactDetailsModel.setSecondaryPhone(contactDetails.getSecondaryPhone());
        contactDetailsModel.setPostalCode(contactDetails.getPostalCode());
        contactDetailsModel.setStreet(contactDetails.getStreet());

        return contactDetailsModel;
    }

    public ContactDetailsDto mapContactDetailsModel(ContactDetailsModel contactDetails) {
        ContactDetailsDto contactDetailsDto = new ContactDetailsDto();

        contactDetailsDto.setCountry(contactDetails.getCountry());
        contactDetailsDto.setCity(contactDetails.getCity());
        contactDetailsDto.setPrimaryPhone(contactDetails.getPrimaryPhone());
        contactDetailsDto.setSecondaryPhone(contactDetails.getSecondaryPhone());
        contactDetailsDto.setPostalCode(contactDetails.getPostalCode());
        contactDetailsDto.setStreet(contactDetails.getStreet());

        return contactDetailsDto;
    }

    public FrequencyModel mapFrequencyDto(FrequencyDto frequencyDto) {
        FrequencyModel frequencyModel = new FrequencyModel();

        if(frequencyDto.getWeekdays() == null) {
            throw new IllegalArgumentException("Weekdays must be non-null!");
        }
        frequencyModel.setWeekdays(frequencyDto.getWeekdays());
        if(frequencyDto.getTimeOfDay() == null || Objects.equals(frequencyDto.getTimeOfDay(), "")) {
            throw new IllegalArgumentException("TimeOfDay must not be null or empty string!");
        }
        frequencyModel.setTimeOfDay(LocalTime.parse(frequencyDto.getTimeOfDay()));

        return frequencyModel;
    }

    public FrequencyDto mapFrequencyModel(FrequencyModel frequencyModel) {
        FrequencyDto frequencyDto = new FrequencyDto();

        frequencyDto.setWeekdays(frequencyModel.getWeekdays());
        if(frequencyModel.getTimeOfDay() != null)
            frequencyDto.setTimeOfDay(frequencyModel.getTimeOfDay().toString());

        return frequencyDto;
    }

    public PatientModel mapPatientDto(PatientDto patient) {
        PatientModel patientModel = new PatientModel();

        patientModel.setCpr(patient.getCpr());
        patientModel.setFamilyName(patient.getFamilyName());
        patientModel.setGivenName(patient.getGivenName());
        if(patient.getPatientContactDetails() != null) {
            patientModel.setContactDetails(mapContactDetailsDto(patient.getPatientContactDetails()));
        }
        patientModel.getPrimaryContact().setName(patient.getPrimaryRelativeName());
        patientModel.getPrimaryContact().setAffiliation(patient.getPrimaryRelativeAffiliation());
        if(patient.getPrimaryRelativeContactDetails() != null) {
            patientModel.getPrimaryContact().setContactDetails(mapContactDetailsDto(patient.getPrimaryRelativeContactDetails()));
        }
        if(patient.getAdditionalRelativeContactDetails() != null) {
            patientModel.setAdditionalRelativeContactDetails(patient.getAdditionalRelativeContactDetails().stream().map(this::mapContactDetailsDto).collect(Collectors.toList()));
        }

        return patientModel;
    }

    public PatientDto mapPatientModel(PatientModel patient) {
        PatientDto patientDto = new PatientDto();

        patientDto.setCpr(patient.getCpr());
        patientDto.setFamilyName(patient.getFamilyName());
        patientDto.setGivenName(patient.getGivenName());
        patientDto.setCustomUserName(patient.getCustomUserName());
        if(patient.getContactDetails() != null) {
            patientDto.setPatientContactDetails(mapContactDetailsModel(patient.getContactDetails()));
        }
        patientDto.setPrimaryRelativeName(patient.getPrimaryContact().getName());
        patientDto.setPrimaryRelativeAffiliation(patient.getPrimaryContact().getAffiliation());
        if(patient.getPrimaryContact().getContactDetails() != null) {
            patientDto.setPrimaryRelativeContactDetails(mapContactDetailsModel(patient.getPrimaryContact().getContactDetails()));
        }
        if(patient.getAdditionalRelativeContactDetails() != null) {
            patientDto.setAdditionalRelativeContactDetails(patient.getAdditionalRelativeContactDetails().stream().map(this::mapContactDetailsModel).collect(Collectors.toList()));
        }

        return patientDto;
    }

    public PlanDefinitionModel mapPlanDefinitionDto(PlanDefinitionDto planDefinitionDto) {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        mapBaseAttributesToModel(planDefinitionModel, planDefinitionDto, ResourceType.PlanDefinition);

        planDefinitionModel.setName(planDefinitionDto.getName());
        planDefinitionModel.setTitle(planDefinitionDto.getTitle());
        if(planDefinitionDto.getStatus() != null) {
            planDefinitionModel.setStatus(Enum.valueOf(PlanDefinitionStatus.class, planDefinitionDto.getStatus()));
        }
        planDefinitionModel.setCreated(planDefinitionDto.getCreated());
        // TODO - planDefinitionModel.getQuestionnaires() should never return null - but it can for now.
        if(planDefinitionDto.getQuestionnaires() != null) {
            planDefinitionModel.setQuestionnaires(planDefinitionDto.getQuestionnaires().stream().map(this::mapQuestionnaireWrapperDto).collect(Collectors.toList()));
        }

        return planDefinitionModel;
    }

    public PlanDefinitionDto mapPlanDefinitionModel(PlanDefinitionModel planDefinitionModel) {
        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();

        planDefinitionDto.setId(planDefinitionModel.getId().toString());
        planDefinitionDto.setName(planDefinitionModel.getName());
        planDefinitionDto.setTitle(planDefinitionModel.getTitle());
        planDefinitionDto.setStatus(planDefinitionModel.getStatus().toString());
        planDefinitionDto.setCreated(planDefinitionModel.getCreated());
        planDefinitionDto.setLastUpdated(planDefinitionModel.getLastUpdated());
        // TODO - planDefinitionModel.getQuestionnaires() should never return null - but it can for now.
        if(planDefinitionModel.getQuestionnaires() != null) {
            planDefinitionDto.setQuestionnaires(planDefinitionModel.getQuestionnaires().stream().map(this::mapQuestionnaireWrapperModel).collect(Collectors.toList()));
        }

        return planDefinitionDto;
    }

    public ThresholdModel mapThresholdDto(ThresholdDto thresholdDto) {
        ThresholdModel thresholdModel = new ThresholdModel();

        thresholdModel.setQuestionnaireItemLinkId(thresholdDto.getQuestionId());
        thresholdModel.setType(thresholdDto.getType());
        thresholdModel.setValueBoolean(thresholdDto.getValueBoolean());
        thresholdModel.setValueQuantityLow(thresholdDto.getValueQuantityLow());
        thresholdModel.setValueQuantityHigh(thresholdDto.getValueQuantityHigh());

        return thresholdModel;
    }

    public ThresholdDto mapThresholdModel(ThresholdModel thresholdModel) {
        ThresholdDto thresholdDto = new ThresholdDto();

        thresholdDto.setQuestionId(thresholdModel.getQuestionnaireItemLinkId());
        thresholdDto.setType(thresholdModel.getType());
        thresholdDto.setValueBoolean(thresholdModel.getValueBoolean());
        thresholdDto.setValueQuantityLow(thresholdModel.getValueQuantityLow());
        thresholdDto.setValueQuantityHigh(thresholdModel.getValueQuantityHigh());

        return thresholdDto;
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

        mapBaseAttributesToModel(questionnaireModel, questionnaireDto, ResourceType.Questionnaire);

        questionnaireModel.setTitle(questionnaireDto.getTitle());
        if (questionnaireDto.getStatus() != null) {
            questionnaireModel.setStatus(QuestionnaireStatus.valueOf(questionnaireDto.getStatus()));
        }
        if(questionnaireDto.getQuestions() != null) {
            questionnaireModel.setQuestions(questionnaireDto.getQuestions().stream().map(this::mapQuestionDto).collect(Collectors.toList()));
        }
        if (questionnaireDto.getCallToAction() != null) {
            questionnaireModel.setCallToAction(this.mapQuestionDto(questionnaireDto.getCallToAction()));
        }

        return questionnaireModel;
    }

    public QuestionnaireDto mapQuestionnaireModel(QuestionnaireModel questionnaireModel) {
        QuestionnaireDto questionnaireDto = new QuestionnaireDto();

        questionnaireDto.setId(questionnaireModel.getId().toString());
        questionnaireDto.setTitle(questionnaireModel.getTitle());
        questionnaireDto.setStatus(questionnaireModel.getStatus().toString());
        questionnaireDto.setVersion(questionnaireModel.getVersion());
        questionnaireDto.setLastUpdated(questionnaireModel.getLastUpdated());

        if(questionnaireModel.getQuestions() != null) {
            questionnaireDto.setQuestions(questionnaireModel.getQuestions().stream().map(this::mapQuestionModel).collect(Collectors.toList()));
        }
        if(questionnaireModel.getCallToAction() != null) {
            questionnaireDto.setCallToAction(mapQuestionModel(questionnaireModel.getCallToAction()));
        }

        return questionnaireDto;
    }
    
	public CustomUserRequestDto mapPatientModelToCustomUserRequest(PatientModel patientModel) {
        CustomUserRequestDto customUserRequestDto = new CustomUserRequestDto();

        customUserRequestDto.setFirstName(patientModel.getGivenName());
        customUserRequestDto.setFullName(patientModel.getGivenName() + " "+ patientModel.getFamilyName() );
        customUserRequestDto.setLastName(patientModel.getFamilyName());
        customUserRequestDto.setTempPassword(patientModel.getCpr().substring(0,6));
        CustomUserRequestAttributesDto userCreatedRequestModelAttributes = new CustomUserRequestAttributesDto();
        userCreatedRequestModelAttributes.setCpr(patientModel.getCpr());
        
        userCreatedRequestModelAttributes.setInitials(getInitials(patientModel.getGivenName(), patientModel.getFamilyName()));
        customUserRequestDto.setAttributes(userCreatedRequestModelAttributes);
        
        return customUserRequestDto;
		
	}
	
	private String getInitials(String firstName, String lastName) {
		String initials ="";
		if (firstName != null && !firstName.isEmpty()) {
			initials = initials+firstName.charAt(0);
		}
		if (lastName != null && lastName.length()>1) {
            assert firstName != null;
            initials = initials+firstName.substring(0,2);
		}
		return initials;
	}

    public QuestionnaireResponseDto mapQuestionnaireResponseModel(QuestionnaireResponseModel questionnaireResponseModel) {
        QuestionnaireResponseDto questionnaireResponseDto = new QuestionnaireResponseDto();

        questionnaireResponseDto.setId(questionnaireResponseModel.getId().toString());
        questionnaireResponseDto.setQuestionnaireId(questionnaireResponseModel.getQuestionnaireId().toString());
        questionnaireResponseDto.setCarePlanId(questionnaireResponseModel.getCarePlanId().toString());
        questionnaireResponseDto.setQuestionnaireName(questionnaireResponseModel.getQuestionnaireName());
        questionnaireResponseDto.setQuestionAnswerPairs(questionnaireResponseModel.getQuestionAnswerPairs().stream().map(this::mapQuestionAnswerPairModel).collect(Collectors.toList()));
        questionnaireResponseDto.setAnswered(questionnaireResponseModel.getAnswered());
        questionnaireResponseDto.setExaminationStatus(questionnaireResponseModel.getExaminationStatus());
        questionnaireResponseDto.setTriagingCategory(questionnaireResponseModel.getTriagingCategory());
        questionnaireResponseDto.setPatient(mapPatientModel(questionnaireResponseModel.getPatient()));
        questionnaireResponseDto.setPlanDefinitionTitle(questionnaireResponseModel.getPlanDefinitionTitle());

        return questionnaireResponseDto;
    }

    private void mapBaseAttributesToModel(BaseModel target, BaseDto source, ResourceType resourceType) {
        if(source.getId() == null) {
            // OK, in case a resource is being created.
            return;
        }

        if(FhirUtils.isPlainId(source.getId())) {
            target.setId(new QualifiedId(source.getId(), resourceType));
        }
        else if(FhirUtils.isQualifiedId(source.getId(), resourceType)) {
            target.setId(new QualifiedId(source.getId()));
        }
        else {
            throw new IllegalArgumentException(String.format("Illegal id provided for resource of type %s: %s!", resourceType.toString(), source.getId()));
        }
    }

    private QuestionAnswerPairDto mapQuestionAnswerPairModel(QuestionAnswerPairModel questionAnswerPairModel) {
        QuestionAnswerPairDto questionAnswerPairDto = new QuestionAnswerPairDto();
        questionAnswerPairDto.setQuestion(mapQuestionModel(questionAnswerPairModel.getQuestion()));
        questionAnswerPairDto.setAnswer(mapAnswerModel(questionAnswerPairModel.getAnswer()));

        return questionAnswerPairDto;
    }

    public QuestionModel mapQuestionDto(QuestionDto questionDto) {
        QuestionModel questionModel = new QuestionModel();

        questionModel.setLinkId(questionDto.getLinkId());
        questionModel.setText(questionDto.getText());
        questionModel.setAbbreviation(questionDto.getAbbreviation());
        questionModel.setRequired(questionDto.getRequired());
        questionModel.setOptions(questionDto.getOptions());
        questionModel.setQuestionType(questionDto.getQuestionType());
        questionModel.setEnableWhens(questionDto.getEnableWhen());
        questionModel.setHelperText(questionDto.getHelperText());

        if(questionDto.getMeasurementType() != null){
            questionModel.setMeasurementType(mapMeasurementTypeDto(questionDto.getMeasurementType()));
        }

        if (questionDto.getThresholds() != null) {
            questionModel.setThresholds(questionDto.getThresholds().stream().map(this::mapThresholdDto).collect(Collectors.toList()));
        }

        return questionModel;
    }


    private MeasurementTypeModel mapMeasurementTypeDto(MeasurementTypeDto measurementTypeDto) {
        MeasurementTypeModel measurementTypeModel = new MeasurementTypeModel();

        measurementTypeModel.setCode(measurementTypeDto.getCode());
        measurementTypeModel.setDisplay(measurementTypeDto.getDisplay());
        measurementTypeModel.setSystem(measurementTypeDto.getSystem());

        return measurementTypeModel;
    }


    private QuestionDto mapQuestionModel(QuestionModel questionModel) {
        QuestionDto questionDto = new QuestionDto();
        questionDto.setDeprecated(questionModel.isDeprecated());
        questionDto.setLinkId(questionModel.getLinkId());
        questionDto.setText(questionModel.getText());
        questionDto.setAbbreviation(questionModel.getAbbreviation());
        questionDto.setRequired(questionModel.isRequired());
        questionDto.setOptions(questionModel.getOptions());
        questionDto.setQuestionType(questionModel.getQuestionType());
        questionDto.setEnableWhen(questionModel.getEnableWhens());
        questionDto.setHelperText(questionModel.getHelperText());

        if (questionModel.getMeasurementType() != null){
            questionDto.setMeasurementType(mapMeasurementTypeModel(questionModel.getMeasurementType()));
        }

        if (questionModel.getThresholds() != null) {
            questionDto.setThresholds(questionModel.getThresholds().stream().map(this::mapThresholdModel).collect(Collectors.toList()));
        }

        if (questionModel.getQuestionType() == QuestionType.GROUP && questionModel.getSubQuestions() != null) {
            questionDto.setSubQuestions(questionModel.getSubQuestions().stream().map(this::mapQuestionModel).collect(Collectors.toList()));
        }
        return questionDto;
    }

    private AnswerDto mapAnswerModel(AnswerModel answerModel) {
        AnswerDto answerDto = new AnswerDto();
        answerDto.setLinkId(answerModel.getLinkId());
        answerDto.setValue(answerModel.getValue());
        answerDto.setAnswerType(answerModel.getAnswerType());

        if (answerModel.getAnswerType() == AnswerType.GROUP && answerModel.getSubAnswers() != null) {
            answerDto.setSubAnswers(answerModel.getSubAnswers().stream().map(this::mapAnswerModel).collect(Collectors.toList()));
        }

        return answerDto;
    }

    private QuestionnaireWrapperModel mapQuestionnaireWrapperDto(QuestionnaireWrapperDto questionnaireWrapper) {
        QuestionnaireWrapperModel questionnaireWrapperModel = new QuestionnaireWrapperModel();

        questionnaireWrapperModel.setQuestionnaire(mapQuestionnaireDto(questionnaireWrapper.getQuestionnaire()));
        if (questionnaireWrapper.getFrequency() != null) {
            questionnaireWrapperModel.setFrequency(mapFrequencyDto(questionnaireWrapper.getFrequency()));
        }
        questionnaireWrapperModel.setThresholds( questionnaireWrapper.getThresholds().stream().map(this::mapThresholdDto).collect(Collectors.toList()) );

        return questionnaireWrapperModel;
    }

    private QuestionnaireWrapperDto mapQuestionnaireWrapperModel(QuestionnaireWrapperModel questionnaireWrapper) {
        QuestionnaireWrapperDto questionnaireWrapperDto = new QuestionnaireWrapperDto();

        questionnaireWrapperDto.setQuestionnaire(mapQuestionnaireModel(questionnaireWrapper.getQuestionnaire()));
        if (questionnaireWrapper.getFrequency() != null) {
            questionnaireWrapperDto.setFrequency(mapFrequencyModel(questionnaireWrapper.getFrequency()));
        }
        questionnaireWrapperDto.setSatisfiedUntil(questionnaireWrapper.getSatisfiedUntil());
        questionnaireWrapperDto.setThresholds( questionnaireWrapper.getThresholds().stream().map(this::mapThresholdModel).collect(Collectors.toList()) );

        return questionnaireWrapperDto;
    }

    public MeasurementTypeDto mapMeasurementTypeModel(MeasurementTypeModel measurementTypeModel) {
        MeasurementTypeDto measurementTypeDto = new MeasurementTypeDto();

        measurementTypeDto.setSystem(measurementTypeModel.getSystem());
        measurementTypeDto.setCode(measurementTypeModel.getCode());
        measurementTypeDto.setDisplay(measurementTypeModel.getDisplay());

        return measurementTypeDto;
    }
}



