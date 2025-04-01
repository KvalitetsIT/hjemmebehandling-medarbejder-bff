package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.constants.*;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import jakarta.validation.Valid;
import org.hl7.fhir.r4.model.ResourceType;
import org.openapitools.model.*;
import org.openapitools.model.Option;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class DtoMapper {
    private Option mapOptionModel(dk.kvalitetsit.hjemmebehandling.model.Option o) {
        return new Option().option(o.option()).comment(o.comment());
    }

    public CarePlanModel mapCarePlanDto(CarePlanDto carePlanDto) {
        CarePlanModel carePlanModel = new CarePlanModel();
        mapBaseAttributesToModel(carePlanModel, carePlanDto, ResourceType.CarePlan);
        carePlanDto.getTitle().ifPresent(carePlanModel::setTitle);
        carePlanDto.getStatus().ifPresent(status -> carePlanModel.setStatus(Enum.valueOf(CarePlanStatus.class, status)));
        carePlanDto.getCreated().map(OffsetDateTime::toInstant).ifPresent(carePlanModel::setCreated);
        carePlanDto.getEndDate().map(OffsetDateTime::toInstant).ifPresent(carePlanModel::setEndDate);
        carePlanDto.getPatientDto().map(this::mapPatientDto).ifPresent(carePlanModel::setPatient);
        carePlanModel.setQuestionnaires(List.of());
        Optional.ofNullable(carePlanDto.getQuestionnaires()).map(x -> x.stream().map(this::mapQuestionnaireWrapperDto).toList()).ifPresent(carePlanModel::setQuestionnaires);
        carePlanModel.setPlanDefinitions(List.of());
        Optional.ofNullable(carePlanDto.getPlanDefinitions()).map(x -> x.stream().map(this::mapPlanDefinitionDto).toList()).ifPresent(carePlanModel::setPlanDefinitions);
        carePlanDto.getDepartmentName().ifPresent(carePlanModel::setDepartmentName);
        return carePlanModel;
    }

    public CarePlanDto mapCarePlanModel(CarePlanModel carePlan) {
        CarePlanDto carePlanDto = new CarePlanDto();
        carePlanDto.setId(Optional.ofNullable(carePlan.getId().toString()));
        carePlanDto.setTitle(Optional.ofNullable(carePlan.getTitle()));
        carePlanDto.setStatus(Optional.ofNullable(carePlan.getStatus().toString()));
        carePlanDto.setCreated(Optional.ofNullable(carePlan.getCreated()).map(this::mapInstant));
        carePlanDto.setStartDate(Optional.ofNullable(carePlan.getStartDate()).map(this::mapInstant));
        carePlanDto.setEndDate(Optional.ofNullable(carePlan.getEndDate()).map(this::mapInstant));
        carePlanDto.setEndDate(Optional.ofNullable(carePlan.getEndDate()).map(this::mapInstant));
        carePlanDto.setPatientDto(Optional.ofNullable(carePlan.patient()).map(this::mapPatientModel));
        carePlanDto.setQuestionnaires(carePlan.getQuestionnaires().stream().map(this::mapQuestionnaireWrapperModel).toList());
        carePlanDto.setPlanDefinitions(carePlan.getPlanDefinitions().stream().map(this::mapPlanDefinitionModel).toList());
        carePlanDto.setDepartmentName(Optional.ofNullable(carePlan.getDepartmentName()));

        return carePlanDto;
    }

    public ContactDetailsModel mapContactDetailsDto(ContactDetailsDto contactDetails) {
        ContactDetailsModel contactDetailsModel = new ContactDetailsModel();

        contactDetails.getCountry().ifPresent(contactDetailsModel::setCountry);
        contactDetails.getCity().ifPresent(contactDetailsModel::setCity);
        contactDetails.primaryPhone().ifPresent(contactDetailsModel::setPrimaryPhone);
        contactDetails.secondaryPhone().ifPresent(contactDetailsModel::setSecondaryPhone);
        contactDetails.getPostalCode().ifPresent(contactDetailsModel::setPostalCode);
        contactDetails.getStreet().ifPresent(contactDetailsModel::setStreet);

        return contactDetailsModel;
    }

    public ContactDetailsDto mapContactDetailsModel(ContactDetailsModel contactDetails) {
        ContactDetailsDto contactDetailsDto = new ContactDetailsDto();

        contactDetailsDto.setCountry(Optional.ofNullable(contactDetails.getCountry()));
        contactDetailsDto.setCity(Optional.ofNullable(contactDetails.getCity()));
        contactDetailsDto.setPrimaryPhone(Optional.ofNullable(contactDetails.primaryPhone()));
        contactDetailsDto.setSecondaryPhone(Optional.ofNullable(contactDetails.secondaryPhone()));
        contactDetailsDto.setPostalCode(Optional.ofNullable(contactDetails.getPostalCode()));
        contactDetailsDto.setStreet(Optional.ofNullable(contactDetails.getStreet()));

        return contactDetailsDto;
    }

    public FrequencyModel mapFrequencyDto(FrequencyDto frequencyDto) {
        FrequencyModel frequencyModel = new FrequencyModel();

        if (frequencyDto.getWeekdays() == null) {
            throw new IllegalArgumentException("Weekdays must be non-null!");
        }
        frequencyModel.setWeekdays(frequencyDto.getWeekdays().stream().map(this::mapWeekdayDto).toList());
        if (frequencyDto.getTimeOfDay().isEmpty() || Objects.equals(frequencyDto.getTimeOfDay().get(), "")) {
            throw new IllegalArgumentException("TimeOfDay must not be null or empty string!");
        }

        frequencyDto.getTimeOfDay().map(LocalTime::parse).ifPresent(frequencyModel::setTimeOfDay);

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
        Optional.ofNullable(frequencyModel.getWeekdays()).map(x -> x.stream().map(this::mapWeekdayModel).toList()).ifPresent(frequencyDto::setWeekdays);
        frequencyDto.setTimeOfDay(Optional.ofNullable(frequencyModel.getTimeOfDay()).map(LocalTime::toString));
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

        patient.getCpr().ifPresent(patientModel::setCpr);
        patient.getFamilyName().ifPresent(patientModel::setFamilyName);
        patient.getGivenName().ifPresent(patientModel::setGivenName);
        patient.getPatientContactDetails().map(this::mapContactDetailsDto).ifPresent(patientModel::setContactDetails);
        patient.getPrimaryRelativeName().ifPresent(x -> patientModel.primaryContact().setName(x));
        patient.getPrimaryRelativeAffiliation().ifPresent(x -> patientModel.primaryContact().setAffiliation(x));
        patient.getPrimaryRelativeContactDetails().map(this::mapContactDetailsDto).ifPresent(x -> patientModel.primaryContact().setContactDetails(x));
        Optional.ofNullable(patient.getAdditionalRelativeContactDetails()).ifPresent(x -> patientModel.setAdditionalRelativeContactDetails(x.stream().map(this::mapContactDetailsDto).toList()));

        return patientModel;
    }

    public PatientDto mapPatientModel(PatientModel patient) {
        PatientDto patientDto = new PatientDto();
        patientDto.setCpr(Optional.ofNullable(patient.getCpr()));
        patientDto.setFamilyName(Optional.ofNullable(patient.getFamilyName()));
        patientDto.setGivenName(Optional.ofNullable(patient.getGivenName()));
        patientDto.setCustomUserName(Optional.ofNullable(patient.getCustomUserName()));
        Optional.ofNullable(patient.contactDetails()).map(this::mapContactDetailsModel).ifPresent(x -> patientDto.setPatientContactDetails(Optional.of(x)));
        patientDto.setPrimaryRelativeName(Optional.ofNullable(patient.primaryContact().name()));
        patientDto.setPrimaryRelativeAffiliation(Optional.ofNullable(patient.primaryContact().getAffiliation()));
        Optional.ofNullable(patient.primaryContact().contactDetails()).map(this::mapContactDetailsModel).ifPresent(x -> patientDto.setPrimaryRelativeContactDetails(Optional.of(x)));
        Optional.ofNullable(patient.getAdditionalRelativeContactDetails()).map(x -> x.stream().map(this::mapContactDetailsModel).toList()).ifPresent(patientDto::setAdditionalRelativeContactDetails);
        return patientDto;
    }

    public PlanDefinitionModel mapPlanDefinitionDto(PlanDefinitionDto planDefinitionDto) {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();
        mapBaseAttributesToModel(planDefinitionModel, planDefinitionDto, ResourceType.PlanDefinition);
        planDefinitionDto.name().ifPresent(planDefinitionModel::setName);
        planDefinitionDto.getTitle().ifPresent(planDefinitionModel::setTitle);
        planDefinitionDto.getStatus().map(x -> Enum.valueOf(PlanDefinitionStatus.class, x)).ifPresent(planDefinitionModel::setStatus);
        planDefinitionDto.getCreated().map(OffsetDateTime::toInstant).ifPresent(planDefinitionModel::setCreated);
        Optional.ofNullable(planDefinitionDto.getQuestionnaires()).ifPresent(questionnaires -> planDefinitionModel.setQuestionnaires(questionnaires.stream().map(this::mapQuestionnaireWrapperDto).toList()));
        return planDefinitionModel;
    }

    public PlanDefinitionDto mapPlanDefinitionModel(PlanDefinitionModel planDefinitionModel) {
        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();
        planDefinitionDto.setId(Optional.ofNullable(planDefinitionModel.getId()).map(Object::toString));
        planDefinitionDto.setName(Optional.ofNullable(planDefinitionModel.name()));
        planDefinitionDto.setTitle(Optional.ofNullable(planDefinitionModel.getTitle()));
        planDefinitionDto.setStatus(Optional.ofNullable(planDefinitionModel.getStatus().toString()));
        planDefinitionDto.setCreated(Optional.ofNullable(planDefinitionModel.getCreated()).map(this::mapInstant));
        planDefinitionDto.setLastUpdated(Optional.ofNullable(planDefinitionModel.getLastUpdated()).map(this::mapInstant));
        Optional.ofNullable(planDefinitionModel.getQuestionnaires()).ifPresent(questionnaires -> planDefinitionDto.setQuestionnaires(questionnaires.stream().map(this::mapQuestionnaireWrapperModel).toList()));
        return planDefinitionDto;
    }

    public ThresholdModel mapThresholdDto(ThresholdDto thresholdDto) {
        return new ThresholdModel(
                thresholdDto.getQuestionId().orElse(null),
                thresholdDto.getType().map(this::mapThresholdTypeDto).orElse(null),
                thresholdDto.getValueQuantityLow().orElse(null),
                thresholdDto.getValueQuantityHigh().orElse(null),
                thresholdDto.getValueBoolean().orElse(null),
                thresholdDto.getValueOption().orElse(null)
        );
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
        thresholdDto.setQuestionId(Optional.ofNullable(thresholdModel.questionnaireItemLinkId()));
        thresholdDto.setValueBoolean(Optional.ofNullable(thresholdModel.valueBoolean()));
        thresholdDto.setValueQuantityLow(Optional.ofNullable(thresholdModel.valueQuantityLow()));
        thresholdDto.setValueQuantityHigh(Optional.ofNullable(thresholdModel.valueQuantityHigh()));
        thresholdDto.setValueOption(Optional.ofNullable(thresholdModel.valueOption()));
        thresholdDto.setType(Optional.ofNullable(thresholdModel.type()).map(this::mapThresholdTypeModel));
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
        personDto.setCpr(Optional.ofNullable(person.getIdentifier().getId()));
        personDto.setFamilyName(Optional.ofNullable(person.name().getFamily()));
        personDto.setGivenName(Optional.of(String.join(" ", person.name().getGiven())));
        personDto.setBirthDate(Optional.ofNullable(person.getBirthDate()));
        personDto.setDeceasedBoolean(Optional.of(person.isDeceasedBoolean()));
        personDto.setGender(Optional.ofNullable(person.getGender()));
        personDto.setPatientContactDetails(Optional.of(new ContactDetailsDto()));
        personDto.getPatientContactDetails().ifPresent(x -> x.setCountry(Optional.ofNullable(person.getAddress().getCountry())));
        personDto.getPatientContactDetails().ifPresent(x -> x.setPostalCode(Optional.ofNullable(person.getAddress().getPostalCode())));
        personDto.getPatientContactDetails().ifPresent(x -> x.setStreet(Optional.ofNullable(person.getAddress().getLine())));
        personDto.getPatientContactDetails().ifPresent(x -> x.setCity(Optional.ofNullable(person.getAddress().getCity())));
        return personDto;
    }

    public QuestionnaireModel mapQuestionnaireDto(QuestionnaireDto questionnaireDto) {
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();
        mapBaseAttributesToModel(questionnaireModel, questionnaireDto, ResourceType.Questionnaire);
        questionnaireDto.getTitle().ifPresent(questionnaireModel::setTitle);
        questionnaireDto.getStatus().ifPresent(status -> questionnaireModel.setStatus(QuestionnaireStatus.valueOf(status)));
        questionnaireDto.getCallToAction().map(this::mapQuestion).ifPresent(questionnaireModel::setCallToAction);
        Optional.ofNullable(questionnaireDto.getQuestions()).ifPresent((questions) -> questionnaireModel.setQuestions(questions.stream().map(this::mapQuestion).toList()));
        return questionnaireModel;
    }

    public QuestionnaireDto mapQuestionnaireModel(QuestionnaireModel questionnaireModel) {
        QuestionnaireDto questionnaireDto = new QuestionnaireDto();
        questionnaireDto.setId(Optional.ofNullable(questionnaireModel.getId()).map(Object::toString));
        questionnaireDto.setTitle(Optional.ofNullable(questionnaireModel.getTitle()));
        questionnaireDto.setStatus(Optional.ofNullable(questionnaireModel.getStatus().toString()));
        questionnaireDto.setVersion(Optional.ofNullable(questionnaireModel.getVersion()));
        questionnaireDto.setLastUpdated(Optional.ofNullable(questionnaireModel.getLastUpdated()).map(this::mapDate));
        Optional.ofNullable(questionnaireModel.getQuestions()).map(x -> x.stream().map(this::mapQuestion).toList()).ifPresent(questionnaireDto::setQuestions);
        questionnaireDto.setCallToAction(Optional.ofNullable(questionnaireModel.getCallToAction()).map(this::mapQuestion));
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
        questionnaireResponseDto.setQuestionnaireId(Optional.ofNullable(questionnaireResponseModel.getQuestionnaireId()).map(Object::toString));
        questionnaireResponseDto.setCarePlanId(Optional.ofNullable(questionnaireResponseModel.getCarePlanId()).map(Object::toString));
        questionnaireResponseDto.setQuestionnaireName(Optional.ofNullable(questionnaireResponseModel.getQuestionnaireName()));
        questionnaireResponseDto.setQuestionAnswerPairs(questionnaireResponseModel.getQuestionAnswerPairs().stream().map(this::mapQuestionAnswerPairModel).toList());
        questionnaireResponseDto.setAnswered(Optional.ofNullable(questionnaireResponseModel.getAnswered()).map(this::mapInstant));
        questionnaireResponseDto.setExaminationStatus(Optional.ofNullable(questionnaireResponseModel.examinationStatus()).map(this::mapExaminationStatusModel));
        questionnaireResponseDto.setTriagingCategory(Optional.ofNullable(questionnaireResponseModel.triagingCategory()).map(this::mapTriagingCategoryModel));
        questionnaireResponseDto.setPatient(Optional.ofNullable(questionnaireResponseModel.patient()).map(this::mapPatientModel));
        questionnaireResponseDto.setPlanDefinitionTitle(Optional.ofNullable(questionnaireResponseModel.getPlanDefinitionTitle()));
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
            case UNDER_EXAMINATION -> ExaminationStatusDto.UNDER_EXAMINATION;
            case EXAMINED -> ExaminationStatusDto.EXAMINED;
        };
    }

    private void mapBaseAttributesToModel(BaseModel target, BaseDto source, ResourceType resourceType) {
        if (source.getId().isEmpty()) {
            // OK, in case a resource is being created.
            return;
        }

        if (FhirUtils.isPlainId(source.getId().get())) {
            target.setId(new QualifiedId(source.getId().get(), resourceType));
        } else if (FhirUtils.isQualifiedId(source.getId().get(), resourceType)) {
            target.setId(new QualifiedId(source.getId().get()));
        } else {
            throw new IllegalArgumentException(String.format("Illegal id provided for resource of type %s: %s!", resourceType, source.getId()));
        }
    }

    private QuestionAnswerPairDto mapQuestionAnswerPairModel(QuestionAnswerPairModel questionAnswerPairModel) {
        QuestionAnswerPairDto questionAnswerPairDto = new QuestionAnswerPairDto();
        questionAnswerPairDto.setQuestion(Optional.ofNullable(questionAnswerPairModel.getQuestion()).map(this::mapQuestion));
        questionAnswerPairDto.setAnswer(Optional.of(questionAnswerPairModel.getAnswer()).map(this::mapAnswerModel));
        return questionAnswerPairDto;
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
        return new dk.kvalitetsit.hjemmebehandling.model.Option(
                option.getComment().orElse(null),
                option.getOption().orElse(null)
        );
    }

    private QuestionModel.EnableWhen mapEnableWhenDto(@Valid EnableWhen enableWhen) {
        return new QuestionModel.EnableWhen(
                enableWhen.getAnswer().map(this::mapAnswerDto).orElse(null),
                enableWhen.getOperator().map(this::mapEnableWhenOperatorModel).orElse(null)
        );
    }

    private AnswerModel mapAnswerDto(@Valid AnswerDto answer) {
        return new AnswerModel(
                answer.getLinkId().orElse(null),
                answer.getValue().orElse(null),
                answer.getAnswerType().map(this::mapAnswerTypeDto).orElse(null),
                answer.getSubAnswers().stream().map(this::mapAnswerDto).toList()
        );
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
        measurementTypeDto.getCode().ifPresent(measurementTypeModel::setCode);
        measurementTypeDto.getDisplay().ifPresent(measurementTypeModel::setDisplay);
        measurementTypeDto.getSystem().ifPresent(measurementTypeModel::setSystem);
        return measurementTypeModel;
    }


    private QuestionDto mapQuestion(QuestionModel questionModel) {
        QuestionDto questionDto = new QuestionDto();
        questionDto.setDeprecated(Optional.of(questionModel.deprecated()));
        questionDto.setLinkId(Optional.ofNullable(questionModel.linkId()));
        questionDto.setText(Optional.ofNullable(questionModel.text()));
        questionDto.setAbbreviation(Optional.ofNullable(questionModel.abbreviation()));
        questionDto.setRequired(Optional.of(questionModel.required()));
        questionDto.setOptions(questionModel.options() != null ? questionModel.options().stream().map(this::mapOptionModel).toList() : null);
        questionDto.setQuestionType(Optional.ofNullable(questionModel.questionType()).map(this::mapQuestionTypeModel));
        questionDto.setEnableWhen(questionModel.enableWhens() != null ? questionModel.enableWhens().stream().map(this::mapEnableWhenModel).toList() : null);
        questionDto.setHelperText(Optional.ofNullable(questionModel.helperText()));
        questionDto.setMeasurementType(Optional.ofNullable(questionModel.measurementType()).map(this::mapMeasurementTypeModel));
        Optional.ofNullable(questionModel.thresholds())
                .ifPresent(x -> questionDto.setThresholds(x.stream().map(this::mapThresholdModel).toList()));

        if (questionModel.questionType() == QuestionType.GROUP && questionModel.subQuestions() != null) {
            questionDto.setSubQuestions(questionModel.subQuestions().stream().map(this::mapQuestion).toList());
        }

        return questionDto;
    }

    public QuestionModel mapQuestion(QuestionDto questionDto) {
        return new QuestionModel(
                questionDto.getLinkId().orElse(null),
                questionDto.getText().orElse(null),
                questionDto.getAbbreviation().orElse(null),
                questionDto.getHelperText().orElse(null),
                questionDto.getRequired().orElse(false),
                questionDto.getQuestionType().map(this::mapQuestionTypeDto).orElse(null),
                questionDto.getMeasurementType().map(this::mapMeasurementTypeDto).orElse(null),
                questionDto.getOptions().stream().map(this::mapOptionDto).toList(),
                questionDto.getEnableWhen().stream().map(this::mapEnableWhenDto).toList(),
                questionDto.getThresholds().stream().map(this::mapThresholdDto).toList(),
                questionDto.getSubQuestions().stream().map(this::mapQuestion).toList(),
                questionDto.getDeprecated().orElse(null)
        );
    }

    private EnableWhen mapEnableWhenModel(QuestionModel.EnableWhen enableWhen) {
        return new EnableWhen()
                .answer(Optional.ofNullable(enableWhen.answer()).map(this::mapAnswerModel).orElse(null))
                .operator(Optional.ofNullable(enableWhen.operator()).map(this::mapOperatorModel).orElse(null));
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
        answerDto.setLinkId(Optional.ofNullable(answerModel.linkId()));
        answerDto.setValue(Optional.ofNullable(answerModel.value()));
        answerDto.setAnswerType(Optional.ofNullable(answerModel.answerType()).map(this::mapAnswerTypeModel));

        if (answerModel.answerType() == AnswerType.GROUP) {
            Optional.ofNullable(answerModel.subAnswers()).map(x -> x.stream().map(this::mapAnswerModel).toList()).ifPresent(answerDto::setSubAnswers);
        }

        return answerDto;
    }


    private QuestionnaireWrapperModel mapQuestionnaireWrapperDto(QuestionnaireWrapperDto questionnaireWrapper) {
        QuestionnaireWrapperModel questionnaireWrapperModel = new QuestionnaireWrapperModel();
        questionnaireWrapper.getQuestionnaire().map(this::mapQuestionnaireDto).ifPresent(questionnaireWrapperModel::setQuestionnaire);
        questionnaireWrapper.getFrequency().map(this::mapFrequencyDto).ifPresent(questionnaireWrapperModel::setFrequency);
        questionnaireWrapperModel.setThresholds(questionnaireWrapper.getThresholds().stream().map(this::mapThresholdDto).toList());
        return questionnaireWrapperModel;
    }

    private QuestionnaireWrapperDto mapQuestionnaireWrapperModel(QuestionnaireWrapperModel questionnaireWrapper) {
        QuestionnaireWrapperDto questionnaireWrapperDto = new QuestionnaireWrapperDto();
        questionnaireWrapperDto.setQuestionnaire(Optional.ofNullable(questionnaireWrapper.getQuestionnaire()).map(this::mapQuestionnaireModel));
        questionnaireWrapperDto.setFrequency(Optional.ofNullable(questionnaireWrapper.getFrequency()).map(this::mapFrequencyModel));
        Optional.ofNullable(questionnaireWrapper.getSatisfiedUntil()).ifPresent(satisfiedUntil -> questionnaireWrapperDto.setSatisfiedUntil(Optional.of(satisfiedUntil).map(this::mapInstant)));
        questionnaireWrapperDto.setThresholds(questionnaireWrapper.getThresholds().stream().map(this::mapThresholdModel).toList());
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
        measurementTypeDto.setSystem(Optional.ofNullable(measurementTypeModel.getSystem()));
        measurementTypeDto.setCode(Optional.ofNullable(measurementTypeModel.getCode()));
        measurementTypeDto.setDisplay(Optional.ofNullable(measurementTypeModel.getDisplay()));
        return measurementTypeDto;
    }

    public ExaminationStatus mapExaminationStatusDto(ExaminationStatusDto examinationStatus) {
        return switch (examinationStatus) {
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



