package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.OptionQuestionModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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
        OptionQuestionModel question = new OptionQuestionModel();

        var item = getQuestionnaireItem(questionnaire, linkId);
        if(item == null) {
            throw new IllegalStateException(String.format("Malformed QuestionnaireResponse: Question for linkId %s not found in Questionnaire %s!", linkId, questionnaire.getId()));
        }

        question.setText(item.getText());
        question.setRequired(item.getRequired());
        question.setOptions(item.getAnswerOption().stream().map(o -> o.getValue().primitiveValue()).collect(Collectors.toList()));

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

    private AnswerModel getAnswer(QuestionnaireResponse.QuestionnaireResponseItemComponent item) {
        AnswerModel answer = new AnswerModel();

        if(item.getAnswer() == null || item.getAnswer().size() != 1) {
            throw new IllegalStateException("Expected exactly one answer!");
        }
        answer.setValue(item.getAnswer().get(0).getValue().primitiveValue());

        return answer;
    }
}
