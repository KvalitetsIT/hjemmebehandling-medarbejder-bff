package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumeration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FhirMapper {
    @Autowired
    private DateProvider dateProvider;

    public CarePlan mapCarePlanModel(CarePlanModel carePlanModel) {
        CarePlan carePlan = new CarePlan();

        carePlan.setId(carePlanModel.getId());
        carePlan.setTitle(carePlanModel.getTitle());
        carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
        carePlan.setCreated(dateProvider.today());
        carePlan.setPeriod(new Period());
        carePlan.getPeriod().setStart(carePlanModel.getStartDate() != null ? Date.from(carePlanModel.getStartDate()) : dateProvider.today());

        // Set the subject
        if(carePlanModel.getPatient() != null) {
            carePlan.setSubject(new Reference(carePlanModel.getPatient().getId()));
        }

        // Map questionnaires to activities
        if(carePlanModel.getQuestionnaires() != null) {
            carePlan.setActivity(carePlanModel.getQuestionnaires()
                    .stream()
                    .map(q -> buildCarePlanActivity(q))
                    .collect(Collectors.toList()));
        }

        if(carePlanModel.getPlanDefinitions() != null) {
            // Add references to planDefinitions
            carePlan.setInstantiatesCanonical(carePlanModel.getPlanDefinitions()
                    .stream()
                    .map(pd -> new CanonicalType(FhirUtils.qualifyId(pd.getId(), ResourceType.PlanDefinition)))
                    .collect(Collectors.toList()));
        }

        return carePlan;
    }

    public CarePlanModel mapCarePlan(CarePlan carePlan) {
        CarePlanModel carePlanModel = new CarePlanModel();

        carePlanModel.setId(carePlan.getIdElement().toUnqualifiedVersionless().getValue());
        carePlanModel.setTitle(carePlan.getTitle());
        //carePlanModel.setStatus(carePlan.getStatus().getDisplay());
        carePlanModel.setCreated(carePlan.getCreated().toInstant());
        carePlanModel.setStartDate(carePlan.getPeriod().getStart().toInstant());
        if(carePlan.getPeriod().getEnd() != null) {
            carePlanModel.setEndDate(carePlan.getPeriod().getEnd().toInstant());
        }

        return carePlanModel;
    }

    public Timing mapFrequencyModel(FrequencyModel frequencyModel) {
        Timing timing = new Timing();

        Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();

        EnumFactory<Timing.DayOfWeek> factory = new Timing.DayOfWeekEnumFactory();
        repeat.setDayOfWeek(frequencyModel.getWeekdays().stream().map(w -> new Enumeration<>(factory, w.toString().toLowerCase())).collect(Collectors.toList()));
        repeat.setTimeOfDay(List.of(new TimeType(frequencyModel.getTimeOfDay().toString())));
        timing.setRepeat(repeat);

        return timing;
    }

    public Patient mapPatientModel(PatientModel patientModel) {
        Patient patient = new Patient();

        patient.setId(patientModel.getId());

        var name = new HumanName();
        name.addGiven(patientModel.getGivenName());
        name.setFamily(patientModel.getFamilyName());
        patient.addName(name);
        patient.getIdentifier().add(makeCprIdentifier(patientModel.getCpr()));

        return patient;
    }

    public PatientModel mapPatient(Patient patient) {
        PatientModel patientModel = new PatientModel();

        patientModel.setId(patient.getIdElement().toUnqualifiedVersionless().getValue());
        patientModel.setCpr(extractCpr(patient));
        patientModel.setFamilyName(extractFamilyName(patient));
        patientModel.setGivenName(extractGivenNames(patient));
        patientModel.setPatientContactDetails(extractPatientContactDetails(patient));

        return patientModel;
    }

    public PlanDefinitionModel mapPlanDefinition(PlanDefinition planDefinition) {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        planDefinitionModel.setId(planDefinition.getIdElement().toUnqualifiedVersionless().getValue());
        planDefinitionModel.setName(planDefinition.getName());
        planDefinitionModel.setTitle(planDefinition.getTitle());

        return planDefinitionModel;
    }

    public PlanDefinitionModel mapPlanDefinition(PlanDefinition planDefinition, FhirLookupResult lookupResult) {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        planDefinitionModel.setId(planDefinition.getIdElement().toUnqualifiedVersionless().getValue());
        planDefinitionModel.setName(planDefinition.getName());
        planDefinitionModel.setTitle(planDefinition.getTitle());

        // Map actions to questionnaires, along with their frequencies
        planDefinitionModel.setQuestionnaires(planDefinition.getAction().stream().map(a -> mapPlanDefinitionAction(a, lookupResult)).collect(Collectors.toList()));

        return planDefinitionModel;
    }

    private QuestionnaireWrapperModel mapPlanDefinitionAction(PlanDefinition.PlanDefinitionActionComponent action, FhirLookupResult lookupResult) {
        var wrapper = new QuestionnaireWrapperModel();

        wrapper.setFrequency(mapTiming(action.getTimingTiming()));

        Questionnaire questionnaire = lookupResult.getQuestionnaire(action.getDefinitionCanonicalType().getValue());
        wrapper.setQuestionnaire(mapQuestionnaire(questionnaire));

        return wrapper;
    }

    public QuestionnaireModel mapQuestionnaire(Questionnaire questionnaire) {
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();

        questionnaireModel.setId(questionnaire.getIdElement().toUnqualifiedVersionless ().toString());
        questionnaireModel.setTitle(questionnaire.getTitle());
        questionnaireModel.setStatus(questionnaire.getStatus().getDisplay());
        questionnaireModel.setQuestions(questionnaire.getItem().stream().map(item -> mapQuestionnaireItem(item)).collect(Collectors.toList()));

        return questionnaireModel;
    }

    public QuestionnaireResponse mapQuestionnaireResponseModel(QuestionnaireResponseModel questionnaireResponseModel) {
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();

        questionnaireResponse.setId(questionnaireResponseModel.getId());
        questionnaireResponse.setQuestionnaire(questionnaireResponseModel.getQuestionnaireId());
        for(var questionAnswerPair : questionnaireResponseModel.getQuestionAnswerPairs()) {
            questionnaireResponse.getItem().add(getQuestionnaireResponseItem(questionAnswerPair.getAnswer()));
        }
        questionnaireResponse.setAuthored(Date.from(questionnaireResponseModel.getAnswered()));
        questionnaireResponse.getExtension().add(ExtensionMapper.mapExaminationStatus(ExaminationStatus.NOT_EXAMINED));
        questionnaireResponse.getExtension().add(ExtensionMapper.mapTriagingCategory(questionnaireResponseModel.getTriagingCategory()));
        questionnaireResponse.setSubject(new Reference(questionnaireResponseModel.getPatient().getId()));

        return questionnaireResponse;
    }

    public QuestionnaireResponseModel mapQuestionnaireResponse(QuestionnaireResponse questionnaireResponse, Questionnaire questionnaire, Patient patient) {
        QuestionnaireResponseModel questionnaireResponseModel = new QuestionnaireResponseModel();

        questionnaireResponseModel.setId(questionnaireResponse.getIdElement().toUnqualifiedVersionless().toString());
        questionnaireResponseModel.setQuestionnaireName(questionnaire.getTitle());
        questionnaireResponseModel.setQuestionnaireId(questionnaire.getIdElement().toUnqualifiedVersionless().toString());

        // Populate questionAnswerMap
        List<QuestionAnswerPairModel> answers = new ArrayList<>();

        for(var item : questionnaireResponse.getItem()) {
            QuestionModel question = getQuestion(questionnaire, item.getLinkId());
            AnswerModel answer = getAnswer(item);
            answers.add(new QuestionAnswerPairModel(question, answer));
        }

        questionnaireResponseModel.setQuestionAnswerPairs(answers);
        questionnaireResponseModel.setAnswered(questionnaireResponse.getAuthored().toInstant());
        questionnaireResponseModel.setExaminationStatus(ExtensionMapper.extractExaminationStatus(questionnaireResponse.getExtension()));
        questionnaireResponseModel.setTriagingCategory(ExtensionMapper.extractTriagingCategoory(questionnaireResponse.getExtension()));
        questionnaireResponseModel.setPatient(mapPatient(patient));

        return questionnaireResponseModel;
    }

    public FrequencyModel mapTiming(Timing timing) {
        FrequencyModel frequencyModel = new FrequencyModel();

        if(timing.getRepeat() != null) {
            Timing.TimingRepeatComponent repeat = timing.getRepeat();
            frequencyModel.setWeekdays(repeat.getDayOfWeek().stream().map(d -> Enum.valueOf(Weekday.class, d.getValue().toString())).collect(Collectors.toList()));
            frequencyModel.setTimeOfDay(LocalTime.parse(repeat.getTimeOfDay().get(0).getValue()));
        }

        return frequencyModel;
    }

    private String extractCpr(Patient patient) {
        return patient.getIdentifier().get(0).getValue();
    }

    private Identifier makeCprIdentifier(String cpr) {
        Identifier identifier = new Identifier();

        identifier.setSystem(Systems.CPR);
        identifier.setValue(cpr);

        return identifier;
    }

    private String extractFamilyName(Patient patient) {
        if(patient.getName() == null || patient.getName().isEmpty()) {
            return null;
        }
        return patient.getName().get(0).getFamily();
    }

    private String extractGivenNames(Patient patient) {
        if(patient.getName() == null || patient.getName().isEmpty()) {
            return null;
        }
        return patient.getName().get(0).getGivenAsSingleString();
    }

    private ContactDetailsModel extractPatientContactDetails(Patient patient) {
        ContactDetailsModel contactDetails = new ContactDetailsModel();

        contactDetails.setPrimaryPhone(extractPrimaryPhone(patient));

        return contactDetails;
    }

    private String extractPrimaryPhone(Patient patient) {
        if(patient.getTelecom() == null || patient.getTelecom().isEmpty()) {
            return null;
        }
        for(ContactPoint cp : patient.getTelecom()) {
            if(!cp.getValue().contains("@")) {
                return cp.getValue();
            }
        }
        return null;
    }

    private QuestionModel getQuestion(Questionnaire questionnaire, String linkId) {
        var item = getQuestionnaireItem(questionnaire, linkId);
        if(item == null) {
            throw new IllegalStateException(String.format("Malformed QuestionnaireResponse: Question for linkId %s not found in Questionnaire %s!", linkId, questionnaire.getId()));
        }

        return mapQuestionnaireItem(item);
    }

    private Questionnaire.QuestionnaireItemComponent getQuestionnaireItem(Questionnaire questionnaire, String linkId) {
        for(var item : questionnaire.getItem()) {
            if(item.getLinkId().equals(linkId)) {
                return item;
            }
        }
        return null;
    }

    private QuestionModel mapQuestionnaireItem(Questionnaire.QuestionnaireItemComponent item) {
        QuestionModel question = new QuestionModel();

        question.setText(item.getText());
        question.setRequired(item.getRequired());
        if(item.getAnswerOption() != null) {
            question.setOptions(mapOptions(item.getAnswerOption()));
        }
        question.setQuestionType(mapQuestionType(item.getType()));

        return question;
    }

    private List<String> mapOptions(List<Questionnaire.QuestionnaireItemAnswerOptionComponent> optionComponents) {
        return optionComponents
                .stream()
                .map(oc -> oc.getValue().primitiveValue())
                .collect(Collectors.toList());
    }

    private QuestionType mapQuestionType(Questionnaire.QuestionnaireItemType type) {
        switch(type) {
            case CHOICE:
                return QuestionType.CHOICE;
            case INTEGER:
                return QuestionType.INTEGER;
            case QUANTITY:
                return QuestionType.QUANTITY;
            case STRING:
                return QuestionType.STRING;
            default:
                throw new IllegalArgumentException(String.format("Don't know how to map QuestionnaireItemType %s", type.toString()));
        }
    }

    private AnswerModel getAnswer(QuestionnaireResponse.QuestionnaireResponseItemComponent item) {
        AnswerModel answer = new AnswerModel();

        var answerItem = extractAnswerItem(item);
        answer.setValue(answerItem.getValue().primitiveValue());
        answer.setAnswerType(getAnswerType(answerItem));

        return answer;
    }

    private QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent extractAnswerItem(QuestionnaireResponse.QuestionnaireResponseItemComponent item) {
        if(item.getAnswer() == null || item.getAnswer().size() != 1) {
            throw new IllegalStateException("Expected exactly one answer!");
        }
        return item.getAnswer().get(0);
    }

    private AnswerType getAnswerType(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answerItem) {
        String type = answerItem.getValue().getClass().getSimpleName();
        switch(type) {
            case "IntegerType":
                return AnswerType.INTEGER;
            case "StringType":
                return AnswerType.STRING;
            default:
                throw new IllegalArgumentException(String.format("Unsupported AnswerItem of type: %s", type));
        }
    }

    private CarePlan.CarePlanActivityComponent buildCarePlanActivity(QuestionnaireWrapperModel questionnaireWrapperModel) {
        CanonicalType instantiatesCanonical = new CanonicalType(FhirUtils.qualifyId(questionnaireWrapperModel.getQuestionnaire().getId(), ResourceType.Questionnaire));
        Type timing = mapFrequencyModel(questionnaireWrapperModel.getFrequency());

        return buildActivity(instantiatesCanonical, timing);
    }

    private CarePlan.CarePlanActivityComponent buildActivity(CanonicalType instantiatesCanonical, Type timing) {
        CarePlan.CarePlanActivityComponent activity = new CarePlan.CarePlanActivityComponent();

        activity.setDetail(buildDetail(instantiatesCanonical, timing));

        return activity;
    }

    private CarePlan.CarePlanActivityDetailComponent buildDetail(CanonicalType instantiatesCanonical, Type timing) {
        CarePlan.CarePlanActivityDetailComponent detail = new CarePlan.CarePlanActivityDetailComponent();

        detail.setInstantiatesCanonical(List.of(instantiatesCanonical));
        detail.setStatus(CarePlan.CarePlanActivityStatus.NOTSTARTED);
        detail.setScheduled(timing);

        return detail;
    }

    private QuestionnaireResponse.QuestionnaireResponseItemComponent getQuestionnaireResponseItem(AnswerModel answer) {
        var item = new QuestionnaireResponse.QuestionnaireResponseItemComponent();

        item.getAnswer().add(getAnswerItem(answer));

        return item;
    }

    private QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent getAnswerItem(AnswerModel answer) {
        var answerItem = new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent();

        answerItem.setValue(getValue(answer));
        // TODO - handle linkId (possibly by extending AnswerModel with such a field.)

        return answerItem;
    }

    private Type getValue(AnswerModel answer) {
        Type value = null;
        switch(answer.getAnswerType()) {
            case INTEGER:
                value = new IntegerType(answer.getValue());
                break;
            case STRING:
                value = new StringType(answer.getValue());
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown AnswerType: %s", answer.getAnswerType()));
        }
        return value;
    }
}
