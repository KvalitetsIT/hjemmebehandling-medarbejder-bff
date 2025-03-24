package dk.kvalitetsit.hjemmebehandling.api;

import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import dk.kvalitetsit.hjemmebehandling.constants.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import jakarta.validation.Valid;
import org.hl7.fhir.r4.model.ResourceType;
import org.openapitools.model.*;

import org.openapitools.model.Option;
import org.springframework.stereotype.Component;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;

@Component
public class DtoMapper {
    private Option mapOptionModel(dk.kvalitetsit.hjemmebehandling.model.Option o) {
        return new Option().option(o.getOption()).comment(o.getComment());
    }

    public CarePlanModel mapCarePlanDto(CarePlanDto carePlanDto) {
        CarePlanModel carePlanModel = new CarePlanModel();
        mapBaseAttributesToModel(carePlanModel, carePlanDto, ResourceType.CarePlan);
        carePlanModel.setTitle(carePlanDto.getTitle());
        Optional.ofNullable(carePlanDto.getStatus()).ifPresent(status -> carePlanModel.setStatus(Enum.valueOf(CarePlanStatus.class, status)));
        carePlanModel.setCreated(carePlanDto.getCreated());
        carePlanModel.setStartDate(carePlanDto.getStartDate());
        carePlanModel.setEndDate(carePlanDto.getEndDate());
        carePlanModel.setPatient(mapPatientDto(carePlanDto.getPatientDto()));
        carePlanModel.setQuestionnaires(List.of());
        Optional.ofNullable(carePlanDto.getQuestionnaires()).ifPresent(questionnaires -> carePlanModel.setQuestionnaires(carePlanDto.getQuestionnaires().stream().map(this::mapQuestionnaireWrapperDto).collect(Collectors.toList())));
        carePlanModel.setPlanDefinitions(List.of());
        Optional.ofNullable(carePlanDto.getPlanDefinitions()).ifPresent(planDefinitionDtos -> carePlanModel.setPlanDefinitions(planDefinitionDtos.stream().map(this::mapPlanDefinitionDto).collect(Collectors.toList())));
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

        if (frequencyDto.getWeekdays() == null) {
            throw new IllegalArgumentException("Weekdays must be non-null!");
        }
        frequencyModel.setWeekdays(frequencyDto.getWeekdays().stream().map(this::mapWeekdayDto).toList());
        if (frequencyDto.getTimeOfDay() == null || Objects.equals(frequencyDto.getTimeOfDay(), "")) {
            throw new IllegalArgumentException("TimeOfDay must not be null or empty string!");
        }
        frequencyModel.setTimeOfDay(LocalTime.parse(frequencyDto.getTimeOfDay()));

        return frequencyModel;
    }

    private Weekday mapWeekdayDto(FrequencyDto.WeekdaysEnum weekdaysEnum) {
        return switch (weekdaysEnum) {
            case MON -> Weekday.MON;
            case TUE -> Weekday.TUE;
            case WED -> Weekday.WED;
            case THU -> Weekday.THU;
            case FRI -> Weekday.FRI;
            case SAT -> Weekday.SAT;
            case SUN -> Weekday.SUN;
        };
    }

    public FrequencyDto mapFrequencyModel(FrequencyModel frequencyModel) {
        FrequencyDto frequencyDto = new FrequencyDto();

        frequencyDto.setWeekdays(frequencyModel.getWeekdays() != null ? frequencyModel.getWeekdays().stream().map(this::mapWeekdayModel).toList() : null);
        if (frequencyModel.getTimeOfDay() != null)
            frequencyDto.setTimeOfDay(frequencyModel.getTimeOfDay().toString());

        return frequencyDto;
    }

    private FrequencyDto.WeekdaysEnum mapWeekdayModel(Weekday weekday) {
        return switch (weekday) {
            case MON -> FrequencyDto.WeekdaysEnum.MON;
            case TUE -> FrequencyDto.WeekdaysEnum.TUE;
            case WED -> FrequencyDto.WeekdaysEnum.WED;
            case THU -> FrequencyDto.WeekdaysEnum.THU;
            case FRI -> FrequencyDto.WeekdaysEnum.FRI;
            case SAT -> FrequencyDto.WeekdaysEnum.SAT;
            case SUN -> FrequencyDto.WeekdaysEnum.SUN;
        };
    }

    public PatientModel mapPatientDto(PatientDto patient) {
        PatientModel patientModel = new PatientModel();

        patientModel.setCpr(patient.getCpr());
        patientModel.setFamilyName(patient.getFamilyName());
        patientModel.setGivenName(patient.getGivenName());
        if (patient.getPatientContactDetails() != null) {
            patientModel.setContactDetails(mapContactDetailsDto(patient.getPatientContactDetails()));
        }
        patientModel.getPrimaryContact().setName(patient.getPrimaryRelativeName());
        patientModel.getPrimaryContact().setAffiliation(patient.getPrimaryRelativeAffiliation());
        if (patient.getPrimaryRelativeContactDetails() != null) {
            patientModel.getPrimaryContact().setContactDetails(mapContactDetailsDto(patient.getPrimaryRelativeContactDetails()));
        }
        if (patient.getAdditionalRelativeContactDetails() != null) {
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
        if (patient.getContactDetails() != null) {
            patientDto.setPatientContactDetails(mapContactDetailsModel(patient.getContactDetails()));
        }
        patientDto.setPrimaryRelativeName(patient.getPrimaryContact().getName());
        patientDto.setPrimaryRelativeAffiliation(patient.getPrimaryContact().getAffiliation());
        if (patient.getPrimaryContact().getContactDetails() != null) {
            patientDto.setPrimaryRelativeContactDetails(mapContactDetailsModel(patient.getPrimaryContact().getContactDetails()));
        }
        if (patient.getAdditionalRelativeContactDetails() != null) {
            patientDto.setAdditionalRelativeContactDetails(patient.getAdditionalRelativeContactDetails().stream().map(this::mapContactDetailsModel).collect(Collectors.toList()));
        }

        return patientDto;
    }

    public PlanDefinitionModel mapPlanDefinitionDto(PlanDefinitionDto planDefinitionDto) {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        mapBaseAttributesToModel(planDefinitionModel, planDefinitionDto, ResourceType.PlanDefinition);

        planDefinitionModel.setName(planDefinitionDto.getName());
        planDefinitionModel.setTitle(planDefinitionDto.getTitle());
        if (planDefinitionDto.getStatus() != null) {
            planDefinitionModel.setStatus(Enum.valueOf(PlanDefinitionStatus.class, planDefinitionDto.getStatus()));
        }
        planDefinitionModel.setCreated(planDefinitionDto.getCreated() != null ? planDefinitionDto.getCreated().toInstant() : null);
        // TODO - planDefinitionModel.getQuestionnaires() should never return null - but it can for now.

        Optional.ofNullable(planDefinitionDto.getQuestionnaires())
                .ifPresent(questionnaires -> planDefinitionModel.setQuestionnaires(questionnaires.stream().map(this::mapQuestionnaireWrapperDto).collect(Collectors.toList())));


        return planDefinitionModel;
    }

    public PlanDefinitionDto mapPlanDefinitionModel(PlanDefinitionModel planDefinitionModel) {
        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();

        planDefinitionDto.setId(planDefinitionModel.getId().toString());
        planDefinitionDto.setName(planDefinitionModel.getName());
        planDefinitionDto.setTitle(planDefinitionModel.getTitle());
        planDefinitionDto.setStatus(planDefinitionModel.getStatus().toString());
        planDefinitionDto.setCreated(planDefinitionModel.getCreated() != null ? this.mapInstant(planDefinitionModel.getCreated()) : null);
        planDefinitionDto.setLastUpdated(planDefinitionModel.getLastUpdated() != null ? this.mapInstant(planDefinitionModel.getLastUpdated()) : null);
        // TODO - planDefinitionModel.getQuestionnaires() should never return null - but it can for now.
        Optional.ofNullable(planDefinitionModel.getQuestionnaires()).ifPresent(questionnaires -> planDefinitionDto.setQuestionnaires(questionnaires.stream().map(this::mapQuestionnaireWrapperModel).collect(Collectors.toList())));

        return planDefinitionDto;
    }

    public ThresholdModel mapThresholdDto(ThresholdDto thresholdDto) {
        ThresholdModel thresholdModel = new ThresholdModel();

        thresholdModel.setQuestionnaireItemLinkId(thresholdDto.getQuestionId());
        thresholdModel.setType(this.mapThresholdTypeDto(thresholdDto.getType()));
        thresholdModel.setValueBoolean(thresholdDto.getValueBoolean());
        thresholdModel.setValueQuantityLow(thresholdDto.getValueQuantityLow());
        thresholdModel.setValueQuantityHigh(thresholdDto.getValueQuantityHigh());
        thresholdModel.setValueOption(thresholdDto.getValueOption());
        return thresholdModel;
    }

    private ThresholdType mapThresholdTypeDto(ThresholdDto.TypeEnum type) {
        return switch (type) {
            case NORMAL -> ThresholdType.NORMAL;
            case ABNORMAL -> ThresholdType.ABNORMAL;
            case CRITICAL -> ThresholdType.CRITICAL;
        };
    }

    public ThresholdDto mapThresholdModel(ThresholdModel thresholdModel) {
        ThresholdDto thresholdDto = new ThresholdDto();

        thresholdDto.setQuestionId(thresholdModel.getQuestionnaireItemLinkId());
        thresholdDto.setType(this.mapThresholdTypeModel(thresholdModel.getType()));
        thresholdDto.setValueBoolean(thresholdModel.getValueBoolean());
        thresholdDto.setValueQuantityLow(thresholdModel.getValueQuantityLow());
        thresholdDto.setValueQuantityHigh(thresholdModel.getValueQuantityHigh());
        thresholdDto.setValueOption(thresholdModel.getValueOption());
        return thresholdDto;
    }

    private ThresholdDto.TypeEnum mapThresholdTypeModel(ThresholdType type) {
        return switch (type) {
            case NORMAL -> ThresholdDto.TypeEnum.NORMAL;
            case ABNORMAL -> ThresholdDto.TypeEnum.ABNORMAL;
            case CRITICAL -> ThresholdDto.TypeEnum.CRITICAL;
        };
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

        Optional.ofNullable(questionnaireDto.getStatus()).ifPresent(status -> questionnaireModel.setStatus(QuestionnaireStatus.valueOf(status)));
        Optional.ofNullable(questionnaireDto.getQuestions()).ifPresent((questions) -> questionnaireModel.setQuestions(questions.stream().map(this::mapQuestionDto).collect(Collectors.toList())));
        Optional.ofNullable(questionnaireDto.getCallToAction()).ifPresent((callToAction) -> questionnaireModel.setCallToAction(this.mapQuestionDto(callToAction)));

        return questionnaireModel;
    }

    public QuestionnaireDto mapQuestionnaireModel(QuestionnaireModel questionnaireModel) {
        QuestionnaireDto questionnaireDto = new QuestionnaireDto();
        questionnaireDto.setId(questionnaireModel.getId().toString());
        questionnaireDto.setTitle(questionnaireModel.getTitle());
        questionnaireDto.setStatus(questionnaireModel.getStatus().toString());
        questionnaireDto.setVersion(questionnaireModel.getVersion());


        questionnaireDto.setLastUpdated(questionnaireModel.getLastUpdated() != null ? mapDate(questionnaireModel.getLastUpdated()) : null);
        questionnaireDto.setQuestions(questionnaireModel.getQuestions() != null ? questionnaireModel.getQuestions().stream().map(this::mapQuestionModel).collect(Collectors.toList()) : null);
        questionnaireDto.setCallToAction(questionnaireModel.getCallToAction() != null ? mapQuestionModel(questionnaireModel.getCallToAction()) : null);
        return questionnaireDto;
    }


    public CustomUserRequestDto mapPatientModelToCustomUserRequest(PatientModel patientModel) {
        CustomUserRequestDto customUserRequestDto = new CustomUserRequestDto();

        customUserRequestDto.setFirstName(patientModel.getGivenName());
        customUserRequestDto.setFullName(patientModel.getGivenName() + " " + patientModel.getFamilyName());
        customUserRequestDto.setLastName(patientModel.getFamilyName());
        customUserRequestDto.setTempPassword(patientModel.getCpr().substring(0, 6));
        CustomUserRequestAttributesDto userCreatedRequestModelAttributes = new CustomUserRequestAttributesDto();
        userCreatedRequestModelAttributes.setCpr(patientModel.getCpr());

        userCreatedRequestModelAttributes.setInitials(getInitials(patientModel.getGivenName(), patientModel.getFamilyName()));
        customUserRequestDto.setAttributes(userCreatedRequestModelAttributes);

        return customUserRequestDto;

    }

    private String getInitials(String firstName, String lastName) {
        String initials = "";
        if (firstName != null && !firstName.isEmpty()) {
            initials = initials + firstName.charAt(0);
        }
        if (lastName != null && lastName.length() > 1) {
            assert firstName != null;
            initials = initials + firstName.substring(0, 2);
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
        questionnaireResponseDto.setAnswered(questionnaireResponseModel.getAnswered() != null ? mapInstant(questionnaireResponseModel.getAnswered()) : null);
        questionnaireResponseDto.setExaminationStatus(questionnaireResponseModel.getExaminationStatus() != null ? this.mapExaminationStatusModel(questionnaireResponseModel.getExaminationStatus()) : null);
        questionnaireResponseDto.setTriagingCategory(questionnaireResponseModel.getTriagingCategory() != null ? this.mapTriagingCategoryModel(questionnaireResponseModel.getTriagingCategory()) : null);
        questionnaireResponseDto.setPatient(mapPatientModel(questionnaireResponseModel.getPatient()));
        questionnaireResponseDto.setPlanDefinitionTitle(questionnaireResponseModel.getPlanDefinitionTitle());

        return questionnaireResponseDto;
    }

    private QuestionnaireResponseDto.TriagingCategoryEnum mapTriagingCategoryModel(TriagingCategory triagingCategory) {
        return switch (triagingCategory) {
            case GREEN -> QuestionnaireResponseDto.TriagingCategoryEnum.GREEN;
            case YELLOW -> QuestionnaireResponseDto.TriagingCategoryEnum.YELLOW;
            case RED -> QuestionnaireResponseDto.TriagingCategoryEnum.RED;
        };
    }

    public ExaminationStatusDto mapExaminationStatusModel(ExaminationStatus examinationStatus) {
        return switch (examinationStatus) {
            case NOT_EXAMINED -> ExaminationStatusDto.NOT_EXAMINED;
            case UNDER_EXAMINATION ->ExaminationStatusDto.UNDER_EXAMINATION;
            case EXAMINED -> ExaminationStatusDto.EXAMINED;
        };
    }

    private void mapBaseAttributesToModel(BaseModel target, BaseDto source, ResourceType resourceType) {
        if (source.getId() == null) {
            // OK, in case a resource is being created.
            return;
        }

        if (FhirUtils.isPlainId(source.getId())) {
            target.setId(new QualifiedId(source.getId(), resourceType));
        } else if (FhirUtils.isQualifiedId(source.getId(), resourceType)) {
            target.setId(new QualifiedId(source.getId()));
        } else {
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
        questionModel.setRequired(questionDto.getRequired() != null ? questionDto.getRequired() : false);
        questionModel.setOptions(questionDto.getOptions().stream().map(this::mapOptionDto).toList());
        questionModel.setQuestionType(questionDto.getQuestionType() != null ? mapQuestionTypeDto(questionDto.getQuestionType()) : null);
        questionModel.setEnableWhens(questionDto.getEnableWhen().stream().map(this::mapEnableWhenDto).toList());
        questionModel.setHelperText(questionDto.getHelperText());

        if (questionDto.getMeasurementType() != null) {
            questionModel.setMeasurementType(mapMeasurementTypeDto(questionDto.getMeasurementType()));
        }

        if (questionDto.getThresholds() != null) {
            questionModel.setThresholds(questionDto.getThresholds().stream().map(this::mapThresholdDto).collect(Collectors.toList()));
        }

        if (questionDto.getSubQuestions() != null) {
            questionModel.setSubQuestions(questionDto.getSubQuestions().stream().map(this::mapQuestionDto).collect(Collectors.toList()));
        }

        return questionModel;
    }

    private QuestionType mapQuestionTypeDto(QuestionDto.QuestionTypeEnum questionType) {
        return switch (questionType) {
            case CHOICE -> QuestionType.CHOICE;
            case INTEGER -> QuestionType.INTEGER;
            case QUANTITY -> QuestionType.QUANTITY;
            case STRING -> QuestionType.STRING;
            case BOOLEAN -> QuestionType.BOOLEAN;
            case DISPLAY -> QuestionType.DISPLAY;
            case GROUP -> QuestionType.GROUP;
        };
    }

    private dk.kvalitetsit.hjemmebehandling.model.Option mapOptionDto(@Valid Option option) {
        return new dk.kvalitetsit.hjemmebehandling.model.Option(option.getOption(), option.getComment());

    }

    private QuestionModel.EnableWhen mapEnableWhenDto(@Valid EnableWhen enableWhen) {
        var result = new QuestionModel.EnableWhen();
        result.setAnswer(enableWhen.getAnswer() != null ? this.mapAnswerDto(enableWhen.getAnswer()) : null);
        result.setOperator(enableWhen.getOperator() != null ? this.mapEnableWhenOperatorModel(enableWhen.getOperator()) : null);
        return result;
    }

    private AnswerModel mapAnswerDto(@Valid AnswerDto answer) {
        var result = new AnswerModel();

        result.setAnswerType(this.mapAnswerTypeDto(answer.getAnswerType()));
        result.setValue(answer.getValue());
        result.setLinkId(answer.getLinkId());
        result.setSubAnswers(answer.getSubAnswers().stream().map(this::mapAnswerDto).toList());

        return result;
    }

    private AnswerType mapAnswerTypeDto(AnswerDto.AnswerTypeEnum answerType) {
        return switch (answerType) {
            case INTEGER -> AnswerType.INTEGER;
            case STRING -> AnswerType.STRING;
            case BOOLEAN -> AnswerType.BOOLEAN;
            case QUANTITY -> AnswerType.QUANTITY;
            case GROUP -> AnswerType.GROUP;
        };
    }

    private EnableWhenOperator mapEnableWhenOperatorModel(EnableWhen.OperatorEnum operator) {
        return switch (operator) {
            case EQUAL -> EnableWhenOperator.EQUAL;
            case GREATER_THAN -> EnableWhenOperator.GREATER_THAN;
            case LESS_THAN -> EnableWhenOperator.LESS_THAN;
            case GREATER_OR_EQUAL -> EnableWhenOperator.GREATER_OR_EQUAL;
            case LESS_OR_EQUAL -> EnableWhenOperator.LESS_OR_EQUAL;
        };
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
        questionDto.setOptions(questionModel.getOptions() != null ? questionModel.getOptions().stream().map(this::mapOptionModel).toList() : null);
        questionDto.setQuestionType(questionModel.getQuestionType() != null ? mapQuestionTypeModel(questionModel.getQuestionType()) : null);
        questionDto.setEnableWhen(questionModel.getEnableWhens() != null ? questionModel.getEnableWhens().stream().map(this::mapEnableWhenModel).toList() : null);
        questionDto.setHelperText(questionModel.getHelperText());

        if (questionModel.getMeasurementType() != null) {
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

    private EnableWhen mapEnableWhenModel(QuestionModel.EnableWhen enableWhen) {
        var answerModel = enableWhen.getAnswer() != null ? this.mapAnswerModel(enableWhen.getAnswer()) : null;
        var operatorModel = enableWhen.getOperator() != null ? this.mapOperatorModel(enableWhen.getOperator()) : null;

        return new EnableWhen().answer(answerModel).operator(operatorModel);
    }

    private EnableWhen.OperatorEnum mapOperatorModel(EnableWhenOperator operator) {
        return switch (operator) {
            case EQUAL -> EnableWhen.OperatorEnum.EQUAL;
            case GREATER_THAN -> EnableWhen.OperatorEnum.GREATER_THAN;
            case LESS_THAN -> EnableWhen.OperatorEnum.LESS_THAN;
            case GREATER_OR_EQUAL -> EnableWhen.OperatorEnum.GREATER_OR_EQUAL;
            case LESS_OR_EQUAL -> EnableWhen.OperatorEnum.LESS_OR_EQUAL;
        };
    }


    private AnswerDto.AnswerTypeEnum mapAnswerTypeModel(AnswerType answerType) {
        return switch (answerType) {
            case INTEGER -> AnswerDto.AnswerTypeEnum.INTEGER;
            case STRING -> AnswerDto.AnswerTypeEnum.STRING;
            case BOOLEAN -> AnswerDto.AnswerTypeEnum.BOOLEAN;
            case QUANTITY -> AnswerDto.AnswerTypeEnum.QUANTITY;
            case GROUP -> AnswerDto.AnswerTypeEnum.GROUP;
        };
    }

    private QuestionDto.QuestionTypeEnum mapQuestionTypeModel(QuestionType questionType) {
        return switch (questionType) {
            case CHOICE -> QuestionDto.QuestionTypeEnum.CHOICE;
            case INTEGER -> QuestionDto.QuestionTypeEnum.INTEGER;
            case QUANTITY -> QuestionDto.QuestionTypeEnum.QUANTITY;
            case STRING -> QuestionDto.QuestionTypeEnum.STRING;
            case BOOLEAN -> QuestionDto.QuestionTypeEnum.BOOLEAN;
            case DISPLAY -> QuestionDto.QuestionTypeEnum.DISPLAY;
            case GROUP -> QuestionDto.QuestionTypeEnum.GROUP;
        };
    }

    private AnswerDto mapAnswerModel(AnswerModel answerModel) {
        AnswerDto answerDto = new AnswerDto();
        answerDto.setLinkId(answerModel.getLinkId());
        answerDto.setValue(answerModel.getValue());
        answerDto.setAnswerType(mapAnswerTypeModel(answerModel.getAnswerType()));

        Optional.ofNullable(answerModel.getSubAnswers()).ifPresent(subAnswers -> {
            if (answerModel.getAnswerType() == AnswerType.GROUP) {
                answerDto.setSubAnswers(subAnswers.stream().map(this::mapAnswerModel).collect(Collectors.toList()));
            }
        });

        return answerDto;
    }


    private QuestionnaireWrapperModel mapQuestionnaireWrapperDto(QuestionnaireWrapperDto questionnaireWrapper) {
        QuestionnaireWrapperModel questionnaireWrapperModel = new QuestionnaireWrapperModel();

        questionnaireWrapperModel.setQuestionnaire(mapQuestionnaireDto(questionnaireWrapper.getQuestionnaire()));
        if (questionnaireWrapper.getFrequency() != null) {
            questionnaireWrapperModel.setFrequency(mapFrequencyDto(questionnaireWrapper.getFrequency()));
        }
        questionnaireWrapperModel.setThresholds(questionnaireWrapper.getThresholds().stream().map(this::mapThresholdDto).collect(Collectors.toList()));

        return questionnaireWrapperModel;
    }

    private QuestionnaireWrapperDto mapQuestionnaireWrapperModel(QuestionnaireWrapperModel questionnaireWrapper) {
        QuestionnaireWrapperDto questionnaireWrapperDto = new QuestionnaireWrapperDto();

        questionnaireWrapperDto.setQuestionnaire(mapQuestionnaireModel(questionnaireWrapper.getQuestionnaire()));
        if (questionnaireWrapper.getFrequency() != null) {
            questionnaireWrapperDto.setFrequency(mapFrequencyModel(questionnaireWrapper.getFrequency()));
        }
        Optional.ofNullable(questionnaireWrapper.getSatisfiedUntil()).ifPresent(satisfiedUntil -> questionnaireWrapperDto.setSatisfiedUntil(this.mapInstant(satisfiedUntil)));
        questionnaireWrapperDto.setThresholds(questionnaireWrapper.getThresholds().stream().map(this::mapThresholdModel).collect(Collectors.toList()));

        return questionnaireWrapperDto;
    }

    public OffsetDateTime mapInstant(Instant satisfiedUntil) {
        return satisfiedUntil.atOffset(ZoneOffset.UTC);
    }

    private OffsetDateTime mapDate(Date lastUpdated) {
        return lastUpdated.toInstant().atOffset(ZoneOffset.UTC);
    }


    public MeasurementTypeDto mapMeasurementTypeModel(MeasurementTypeModel measurementTypeModel) {
        MeasurementTypeDto measurementTypeDto = new MeasurementTypeDto();

        measurementTypeDto.setSystem(measurementTypeModel.getSystem());
        measurementTypeDto.setCode(measurementTypeModel.getCode());
        measurementTypeDto.setDisplay(measurementTypeModel.getDisplay());

        return measurementTypeDto;
    }

    public ExaminationStatus mapExaminationStatusDto(ExaminationStatusDto examinationStatus) {
        return switch (examinationStatus){
            case NOT_EXAMINED -> ExaminationStatus.NOT_EXAMINED;
            case UNDER_EXAMINATION -> ExaminationStatus.UNDER_EXAMINATION;
            case EXAMINED -> ExaminationStatus.EXAMINED;
        };
    }

    public PlanDefinitionStatus mapPlanDefinitionStatusDto(PatchPlanDefinitionRequest.StatusEnum status) {
        return switch (status) {
            case DRAFT -> PlanDefinitionStatus.DRAFT;
            case ACTIVE -> PlanDefinitionStatus.ACTIVE;
            case RETIRED -> PlanDefinitionStatus.RETIRED;
        };
    }
}



