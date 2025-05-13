package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.client.CustomUserRequestAttributesDto;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserRequestDto;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.*;
import dk.kvalitetsit.hjemmebehandling.types.ThresholdType;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;
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
        return new CarePlanModel(
                carePlanDto.getId().map(QualifiedId.CarePlanId::new).orElse(null),
                null, //Todo: organizationId was expected but is not defined by the api
                carePlanDto.getTitle().orElse(null),
                carePlanDto.getStatus().map(status -> Enum.valueOf(CarePlanStatus.class, status)).orElse(null),
                carePlanDto.getStartDate().map(OffsetDateTime::toInstant).orElse(null),
                carePlanDto.getCreated().map(OffsetDateTime::toInstant).orElse(null),
                carePlanDto.getEndDate().map(OffsetDateTime::toInstant).orElse(null),
                carePlanDto.getPatientDto().map(this::mapPatientDto).orElse(null),
                Optional.ofNullable(carePlanDto.getQuestionnaires()).map(x -> x.stream().map(this::mapQuestionnaireWrapperDto).toList()).orElse(List.of()),
                Optional.ofNullable(carePlanDto.getPlanDefinitions()).map(x -> x.stream().map(this::mapPlanDefinitionDto).toList()).orElse(List.of()),
                carePlanDto.getDepartmentName().orElse(null),
                null
        );
    }

    public CarePlanDto mapCarePlanModel(CarePlanModel carePlan) {
        CarePlanDto carePlanDto = new CarePlanDto();
        carePlanDto.setId(Optional.ofNullable(carePlan.id()).map(QualifiedId.CarePlanId::unqualified));
        carePlanDto.setTitle(Optional.ofNullable(carePlan.title()));
        carePlanDto.setStatus(Optional.ofNullable(carePlan.status().toString()));
        carePlanDto.setCreated(Optional.ofNullable(carePlan.created()).map(this::mapInstant));
        carePlanDto.setStartDate(Optional.ofNullable(carePlan.startDate()).map(this::mapInstant));
        carePlanDto.setEndDate(Optional.ofNullable(carePlan.endDate()).map(this::mapInstant));
        carePlanDto.setEndDate(Optional.ofNullable(carePlan.endDate()).map(this::mapInstant));
        carePlanDto.setPatientDto(Optional.ofNullable(carePlan.patient()).map(this::mapPatientModel));
        carePlanDto.setQuestionnaires(carePlan.questionnaires().stream().map(this::mapQuestionnaireWrapperModel).toList());
        carePlanDto.setPlanDefinitions(carePlan.planDefinitions().stream().map(this::mapPlanDefinitionModel).toList());
        carePlanDto.setDepartmentName(Optional.ofNullable(carePlan.departmentName()));

        return carePlanDto;
    }

    public ContactDetailsModel mapContactDetailsDto(ContactDetailsDto contactDetails) {
        return new ContactDetailsModel(
                contactDetails.getStreet().orElse(null),
                contactDetails.getPostalCode().orElse(null),
                contactDetails.getCountry().orElse(null),
                contactDetails.getCity().orElse(null),
                contactDetails.getPrimaryPhone().orElse(null),
                contactDetails.getSecondaryPhone().orElse(null)
        );
    }

    public ContactDetailsDto mapContactDetailsModel(ContactDetailsModel contactDetails) {
        return new ContactDetailsDto()
                .country(contactDetails.country())
                .city(contactDetails.city())
                .primaryPhone(contactDetails.primaryPhone())
                .secondaryPhone(contactDetails.secondaryPhone())
                .postalCode(contactDetails.postalCode())
                .street(contactDetails.street());
    }

    public FrequencyModel mapFrequencyDto(FrequencyDto frequencyDto) {
        Optional.ofNullable(frequencyDto.getWeekdays()).orElseThrow(() -> new IllegalArgumentException("Weekdays must be non-null!"));
        if (frequencyDto.getTimeOfDay().isEmpty() || Objects.equals(frequencyDto.getTimeOfDay().get(), "")) {
            throw new IllegalArgumentException("TimeOfDay must not be null or empty string!");
        }
        return new FrequencyModel(
                frequencyDto.getWeekdays().stream().map(this::mapWeekdayDto).toList(),
                LocalTime.parse(frequencyDto.getTimeOfDay().get())
        );
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
        Optional.ofNullable(frequencyModel.weekdays()).map(x -> x.stream().map(this::mapWeekdayModel).toList()).ifPresent(frequencyDto::setWeekdays);
        frequencyDto.setTimeOfDay(Optional.ofNullable(frequencyModel.timeOfDay()).map(LocalTime::toString));
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
        return new PatientModel(
                null,
                patient.getName().map(nameDto -> new PersonNameModel(nameDto.getFamily().orElse(null), patient.getName().get().getGiven().map(List::of).orElse(null))).orElse(null),
                patient.getCpr().map(CPR::new).orElse(null),
                patient.getPatientContactDetails().map(this::mapContactDetailsDto).orElse(null),
                new PrimaryContactModel(
                        patient.getPrimaryRelativeContactDetails().map(this::mapContactDetailsDto).orElse(null),
                        patient.getPrimaryRelativeName().orElse(null),
                        patient.getPrimaryRelativeAffiliation().orElse(null),
                        null
                ),
                Optional.ofNullable(patient.getAdditionalRelativeContactDetails()).map(x -> x.stream().map(this::mapContactDetailsDto).toList()).orElse(null),
                null,
                patient.getCustomUserName().orElse(null)
        );
    }

    public PatientDto mapPatientModel(PatientModel patient) {
        PatientDto patientDto = new PatientDto()
                .cpr(patient.cpr().toString())
                .name(new NameDto()
                        .family(patient.name().family())
                        .given(patient.name().given().getFirst()))
                .customUserName(patient.customUserName())
                .primaryRelativeName(patient.primaryContact().name())
                .primaryRelativeAffiliation(patient.primaryContact().affiliation());

        Optional.ofNullable(patient.contactDetails()).map(this::mapContactDetailsModel).ifPresent(x -> patientDto.setPatientContactDetails(Optional.of(x)));
        Optional.ofNullable(patient.primaryContact().contactDetails()).map(this::mapContactDetailsModel).ifPresent(x -> patientDto.setPrimaryRelativeContactDetails(Optional.of(x)));
        Optional.ofNullable(patient.additionalRelativeContactDetails()).map(x -> x.stream().map(this::mapContactDetailsModel).toList()).ifPresent(patientDto::setAdditionalRelativeContactDetails);
        return patientDto;
    }

    public PlanDefinitionModel mapPlanDefinitionDto(PlanDefinitionDto planDefinitionDto) {
        return new PlanDefinitionModel(
                planDefinitionDto.getId().map(QualifiedId.PlanDefinitionId::new).orElse(null),
                null, // Todo: organizationId was expected but is not defined by the api
                planDefinitionDto.getName().orElse(null),
                planDefinitionDto.getTitle().orElse(null),
                planDefinitionDto.getStatus().map(this::mapStatus).orElse(null),
                planDefinitionDto.getCreated().map(OffsetDateTime::toInstant).orElse(null),
                planDefinitionDto.getLastUpdated().map(OffsetDateTime::toInstant).orElse(null),
                Optional.ofNullable(planDefinitionDto.getQuestionnaires()).map(questionnaires -> questionnaires.stream().map(this::mapQuestionnaireWrapperDto).toList()).orElse(null)
        );
    }

    public PlanDefinitionDto mapPlanDefinitionModel(PlanDefinitionModel planDefinitionModel) {
        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto().id(planDefinitionModel.id().unqualified())
                .name(planDefinitionModel.name())
                .title(planDefinitionModel.title());

        planDefinitionDto.setStatus(Optional.ofNullable(planDefinitionModel.status()).map(this::mapStatus));
        planDefinitionDto.setCreated(Optional.ofNullable(planDefinitionModel.created()).map(this::mapInstant));
        planDefinitionDto.setLastUpdated(Optional.ofNullable(planDefinitionModel.lastUpdated()).map(this::mapInstant));
        Optional.ofNullable(planDefinitionModel.questionnaires())
                .ifPresent(questionnaires -> planDefinitionDto.setQuestionnaires(questionnaires.stream().map(this::mapQuestionnaireWrapperModel).toList()));

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
        ThresholdDto thresholdDto = new ThresholdDto()
                .questionId(thresholdModel.questionnaireItemLinkId())
                .valueBoolean(thresholdModel.valueBoolean())
                .valueQuantityLow(thresholdModel.valueQuantityLow())
                .valueQuantityHigh(thresholdModel.valueQuantityHigh())
                .valueOption(thresholdModel.valueOption());

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
        personDto.setCpr(Optional.ofNullable(person.identifier().id()));
        personDto.name(new NameDto().family(person.name().family()).given(String.join(" ", person.name().given())));
        personDto.setBirthDate(Optional.ofNullable(person.birthDate()));
        personDto.setDeceasedBoolean(Optional.of(person.deceasedBoolean()));
        personDto.setGender(Optional.ofNullable(person.gender()));
        personDto.setPatientContactDetails(Optional.of(new ContactDetailsDto()));
        personDto.getPatientContactDetails().ifPresent(x -> x.setCountry(Optional.ofNullable(person.address().country())));
        personDto.getPatientContactDetails().ifPresent(x -> x.setPostalCode(Optional.ofNullable(person.address().postalCode())));
        personDto.getPatientContactDetails().ifPresent(x -> x.setStreet(Optional.ofNullable(person.address().line())));
        personDto.getPatientContactDetails().ifPresent(x -> x.setCity(Optional.ofNullable(person.address().city())));
        return personDto;
    }

    public QuestionnaireModel mapQuestionnaireDto(QuestionnaireDto questionnaireDto) {


        return new QuestionnaireModel(
                questionnaireDto.getId().map(QualifiedId.QuestionnaireId::new).orElse(null),
                null, // Todo: organizationId was expected but is not defined by the api
                questionnaireDto.getTitle().orElse(null),
                null, // Todo: Description was expected but is not defined by the api
                questionnaireDto.getStatus().map(Status::valueOf).orElse(null),
                Optional.ofNullable(questionnaireDto.getQuestions()).map((questions) -> questions.stream().map(this::mapQuestion).toList()).orElse(null),
                questionnaireDto.getCallToAction().map(this::mapQuestion).orElse(null),
                questionnaireDto.getVersion().orElse(null),
                questionnaireDto.getLastUpdated().map(x -> new Date(x.toInstant().getEpochSecond())).orElse(null)
        );
    }

    public QuestionnaireDto mapQuestionnaireModel(QuestionnaireModel questionnaireModel) {
        QuestionnaireDto questionnaireDto = new QuestionnaireDto();
        questionnaireDto.setId(Optional.ofNullable(questionnaireModel.id()).map(Object::toString));
        questionnaireDto.setTitle(Optional.ofNullable(questionnaireModel.title()));
        questionnaireDto.setStatus(Optional.ofNullable(questionnaireModel.status().toString()));
        questionnaireDto.setVersion(Optional.ofNullable(questionnaireModel.version()));
        questionnaireDto.setLastUpdated(Optional.ofNullable(questionnaireModel.lastUpdated()).map(this::mapDate));
        Optional.ofNullable(questionnaireModel.questions()).map(x -> x.stream().map(this::mapQuestion).toList()).ifPresent(questionnaireDto::setQuestions);
        questionnaireDto.setCallToAction(Optional.ofNullable(questionnaireModel.callToAction()).map(this::mapQuestion));
        return questionnaireDto;
    }


    public CustomUserRequestDto mapPatientModelToCustomUserRequest(PatientModel patientModel) {
        CustomUserRequestDto customUserRequestDto = new CustomUserRequestDto();
        customUserRequestDto.setFirstName(patientModel.name().given().getFirst());
        customUserRequestDto.setFullName(patientModel.name().given().getFirst() + " " + patientModel.name().family());
        customUserRequestDto.setLastName(patientModel.name().family());
        customUserRequestDto.setTempPassword(patientModel.cpr().toString().substring(0, 6));
        CustomUserRequestAttributesDto userCreatedRequestModelAttributes = new CustomUserRequestAttributesDto();
        userCreatedRequestModelAttributes.setCpr(patientModel.cpr());
        userCreatedRequestModelAttributes.setInitials(getInitials(patientModel.name().given().getFirst(), patientModel.name().family()));
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
        return new QuestionnaireResponseDto()
                .id(Optional.ofNullable(questionnaireResponseModel.id()).map(Object::toString).orElse(null))
                .questionnaireId(Optional.ofNullable(questionnaireResponseModel.questionnaireId()).map(Object::toString).orElse(null))
                .carePlanId(Optional.ofNullable(questionnaireResponseModel.carePlanId()).map(Object::toString).orElse(null))
                .questionnaireName(questionnaireResponseModel.questionnaireName())
                .questionAnswerPairs(questionnaireResponseModel.questionAnswerPairs().stream().map(this::mapQuestionAnswerPairModel).toList())
                .answered(Optional.ofNullable(questionnaireResponseModel.answered()).map(this::mapInstant).orElse(null))
                .examinationStatus(Optional.ofNullable(questionnaireResponseModel.examinationStatus()).map(this::mapExaminationStatusModel).orElse(null))
                .triagingCategory(Optional.ofNullable(questionnaireResponseModel.triagingCategory()).map(this::mapTriagingCategoryModel).orElse(null))
                .patient(Optional.ofNullable(questionnaireResponseModel.patient()).map(this::mapPatientModel).orElse(null))
                .planDefinitionTitle(questionnaireResponseModel.planDefinitionTitle());
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

    private QuestionAnswerPairDto mapQuestionAnswerPairModel(QuestionAnswerPairModel questionAnswerPairModel) {
        QuestionAnswerPairDto questionAnswerPairDto = new QuestionAnswerPairDto();
        questionAnswerPairDto.setQuestion(Optional.ofNullable(questionAnswerPairModel.question()).map(this::mapQuestion));
        questionAnswerPairDto.setAnswer(Optional.of(questionAnswerPairModel.answer()).map(this::mapAnswerModel));
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
        return new MeasurementTypeModel(
                measurementTypeDto.getSystem().orElse(null),
                measurementTypeDto.getCode().orElse(null),
                measurementTypeDto.getDisplay().orElse(null)
        );
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
                questionDto.getDeprecated().orElse(false)
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
        return QuestionnaireWrapperModel.builder()
                .questionnaire(questionnaireWrapper.getQuestionnaire().map(this::mapQuestionnaireDto).orElse(null))
                .frequency(questionnaireWrapper.getFrequency().map(this::mapFrequencyDto).orElse(null))
                .build();
    }

    private QuestionnaireWrapperDto mapQuestionnaireWrapperModel(QuestionnaireWrapperModel questionnaireWrapper) {
        QuestionnaireWrapperDto questionnaireWrapperDto = new QuestionnaireWrapperDto();
        questionnaireWrapperDto.setQuestionnaire(Optional.ofNullable(questionnaireWrapper.questionnaire()).map(this::mapQuestionnaireModel));
        questionnaireWrapperDto.setFrequency(Optional.ofNullable(questionnaireWrapper.frequency()).map(this::mapFrequencyModel));
        Optional.ofNullable(questionnaireWrapper.satisfiedUntil()).ifPresent(satisfiedUntil -> questionnaireWrapperDto.setSatisfiedUntil(Optional.of(satisfiedUntil).map(this::mapInstant)));
        questionnaireWrapperDto.setThresholds(questionnaireWrapper.thresholds().stream().map(this::mapThresholdModel).toList());
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
        measurementTypeDto.setSystem(Optional.ofNullable(measurementTypeModel.system()));
        measurementTypeDto.setCode(Optional.ofNullable(measurementTypeModel.code()));
        measurementTypeDto.setDisplay(Optional.ofNullable(measurementTypeModel.display()));
        return measurementTypeDto;
    }

    public ExaminationStatus mapExaminationStatusDto(ExaminationStatusDto examinationStatus) {
        return switch (examinationStatus) {
            case NOT_EXAMINED -> ExaminationStatus.NOT_EXAMINED;
            case UNDER_EXAMINATION -> ExaminationStatus.UNDER_EXAMINATION;
            case EXAMINED -> ExaminationStatus.EXAMINED;
        };
    }

    public Status mapStatus(@NotNull StatusDto planDefinitionStatus) {
        return switch (planDefinitionStatus) {
            case DRAFT -> Status.DRAFT;
            case ACTIVE -> Status.ACTIVE;
            case RETIRED -> Status.RETIRED;
        };
    }

    private StatusDto mapStatus(@NotNull Status planDefinitionStatus) {
        return switch (planDefinitionStatus) {
            case DRAFT -> StatusDto.DRAFT;
            case ACTIVE -> StatusDto.ACTIVE;
            case RETIRED -> StatusDto.RETIRED;
        };
    }

    public UserContext mapUserContext(UserContextModel userContext) {
        return new UserContext()
                .name(userContext.name().orElse(null))
                .userId(userContext.userId().orElse(null))
                .email(userContext.email().orElse(null))
                .entitlements(userContext.entitlements())
                .authorizationIds(userContext.authorizationIds())
                .authorizationIds(userContext.authorizationIds())
                .orgName(userContext.orgName().orElse(null))
                .orgId(userContext.orgId().map(QualifiedId.OrganizationId::unqualified).orElse(null));
    }

    public PatientDetails mapUpdateCarePlanRequest(UpdateCareplanRequest request) {
        return new PatientDetails(
                request.getPatientPrimaryPhone().orElse(null),
                request.getPatientSecondaryPhone().orElse(null),
                request.getPrimaryRelativeName().orElse(null),
                request.getPrimaryRelativeAffiliation().orElse(null),
                request.getPrimaryRelativePrimaryPhone().orElse(null),
                request.getPrimaryRelativeSecondaryPhone().orElse(null)
        );
    }
}



