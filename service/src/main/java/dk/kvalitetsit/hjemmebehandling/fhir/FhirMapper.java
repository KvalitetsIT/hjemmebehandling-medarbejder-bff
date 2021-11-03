package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class FhirMapper {
    public CarePlanModel mapCarePlan(CarePlan carePlan) {
        CarePlanModel carePlanModel = new CarePlanModel();

        carePlanModel.setId(carePlan.getId());
        carePlanModel.setTitle(carePlan.getTitle());
        //carePlanModel.setStatus(carePlan.getStatus().getDisplay());

        return carePlanModel;
    }

    public Timing mapFrequencyModel(FrequencyModel frequencyModel) {
        Timing timing = new Timing();

        Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();

        EnumFactory<Timing.DayOfWeek> factory = new Timing.DayOfWeekEnumFactory();
        repeat.setDayOfWeek(List.of(new Enumeration<>(factory, frequencyModel.getWeekday().toString().toLowerCase())));
        timing.setRepeat(repeat);

        return timing;
    }

    public Patient mapPatientModel(PatientModel patientModel) {
        Patient patient = new Patient();

        patient.getIdentifier().add(makeCprIdentifier(patientModel.getCpr()));

        return patient;
    }

    public PatientModel mapPatient(Patient patient) {
        PatientModel patientModel = new PatientModel();

        patientModel.setCpr(extractCpr(patient));
        patientModel.setFamilyName(extractFamilyName(patient));
        patientModel.setGivenName(extractGivenNames(patient));
        patientModel.setPatientContactDetails(extractPatientContactDetails(patient));

        return patientModel;
    }

    public QuestionnaireModel mapQuestionnaire(Questionnaire questionnaire) {
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();

        questionnaireModel.setId(questionnaire.getIdElement().toUnqualifiedVersionless ().toString());

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
        questionnaireResponse.getExtension().add(toExtension(ExaminationStatus.NOT_EXAMINED));
        questionnaireResponse.setSubject(new Reference(questionnaireResponseModel.getPatient().getId()));

        return questionnaireResponse;
    }

    public QuestionnaireResponseModel mapQuestionnaireResponse(QuestionnaireResponse questionnaireResponse, Questionnaire questionnaire, Patient patient) {
        QuestionnaireResponseModel questionnaireResponseModel = new QuestionnaireResponseModel();

        questionnaireResponseModel.setId(questionnaireResponse.getIdElement().toUnqualifiedVersionless().toString());
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
        questionnaireResponseModel.setPatient(mapPatient(patient));

        return questionnaireResponseModel;
    }

    public FrequencyModel mapTiming(Timing timing) {
        FrequencyModel frequencyModel = new FrequencyModel();

        if(timing.getRepeat() != null) {
            Timing.TimingRepeatComponent repeat = timing.getRepeat();
            if(repeat.getDayOfWeek() == null || repeat.getDayOfWeek().size() != 1) {
                throw new IllegalStateException("Only repeats of one day a week is supperted (yet)!");
            }
            frequencyModel.setWeekday(Enum.valueOf(Weekday.class, repeat.getDayOfWeek().get(0).getValue().toString()));
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
        contactDetails.setEmailAddress(extractEmailAddress(patient));

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

    private String extractEmailAddress(Patient patient) {
        if(patient.getTelecom() == null || patient.getTelecom().isEmpty()) {
            return null;
        }
        for(ContactPoint cp : patient.getTelecom()) {
            if(cp.getValue().contains("@")) {
                return cp.getValue();
            }
        }
        return null;
    }

    private QuestionModel getQuestion(Questionnaire questionnaire, String linkId) {
        QuestionModel question = new QuestionModel();

        var item = getQuestionnaireItem(questionnaire, linkId);
        if(item == null) {
            throw new IllegalStateException(String.format("Malformed QuestionnaireResponse: Question for linkId %s not found in Questionnaire %s!", linkId, questionnaire.getId()));
        }

        question.setText(item.getText());
        question.setRequired(item.getRequired());
        if(item.getAnswerOption() != null) {
            question.setOptions(mapOptions(item.getAnswerOption()));
        }
        question.setQuestionType(mapQuestionType(item.getType()));

        return question;
    }

    private Questionnaire.QuestionnaireItemComponent getQuestionnaireItem(Questionnaire questionnaire, String linkId) {
        for(var item : questionnaire.getItem()) {
            if(item.getLinkId().equals(linkId)) {
                return item;
            }
        }
        return null;
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

    private Extension toExtension(ExaminationStatus examinationStatus) {
        Extension extension = new Extension();

        extension.setUrl(Systems.EXAMINATION_STATUS);
        extension.setValue(new StringType(examinationStatus.toString().toLowerCase()));

        return extension;
    }
}
