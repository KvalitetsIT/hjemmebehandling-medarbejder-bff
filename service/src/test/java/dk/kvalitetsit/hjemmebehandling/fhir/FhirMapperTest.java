package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionAnswerPairModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class FhirMapperTest {
    private FhirMapper subject = new FhirMapper();

    @Test
    public void mapQuestionnaireResponseModel_mapsAnswers() {
        // Arrange
        QuestionnaireResponseModel model = buildQuestionnaireResponseModel();

        // Act
        QuestionnaireResponse result = subject.mapQuestionnaireResponseModel(model);

        // Assert
        assertEquals(1, result.getItem().size());
        assertEquals(new IntegerType(2).getValue(), result.getItem().get(0).getAnswer().get(0).getValueIntegerType().getValue());
    }

    @Test
    public void mapQuestionnaireResponseModel_mapsExaminationStatus() {
        // Arrange
        QuestionnaireResponseModel model = buildQuestionnaireResponseModel();

        // Act
        QuestionnaireResponse result = subject.mapQuestionnaireResponseModel(model);

        // Assert
        assertEquals(1, result.getExtension().size());
        assertEquals(Systems.EXAMINATION_STATUS, result.getExtension().get(0).getUrl());
        assertEquals(new StringType(ExaminationStatus.NOT_EXAMINED.name()).toString(), result.getExtension().get(0).getValue().toString());
    }

    @Test
    public void mapQuestionnaireResponse_canMapAnswers() {
        // Arrange
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();

        questionnaireResponse.getItem().add(buildStringItem("hej", "1"));
        questionnaireResponse.getItem().add(buildIntegerItem(2, "2"));
        questionnaireResponse.setAuthored(Date.from(Instant.parse("2021-10-28T00:00:00Z")));

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.getItem().add(buildQuestionItem("1"));
        questionnaire.getItem().add(buildQuestionItem("2"));

        Patient patient = new Patient();
        patient.getIdentifier().add(new Identifier());

        // Act
        QuestionnaireResponseModel result = subject.mapQuestionnaireResponse(questionnaireResponse, questionnaire, patient);

        // Assert
        assertEquals(2, result.getQuestionAnswerPairs().size());
        assertEquals(AnswerType.STRING, result.getQuestionAnswerPairs().get(0).getAnswer().getAnswerType());
        assertEquals(AnswerType.INTEGER, result.getQuestionAnswerPairs().get(1).getAnswer().getAnswerType());
    }

    private QuestionnaireResponse.QuestionnaireResponseItemComponent buildStringItem(String value, String linkId) {
        return buildItem(new StringType(value), linkId);
    }

    private QuestionnaireResponse.QuestionnaireResponseItemComponent buildIntegerItem(int value, String linkId) {
        return buildItem(new IntegerType(value), linkId);
    }

    private QuestionnaireResponse.QuestionnaireResponseItemComponent buildItem(Type value, String linkId) {
        var item = new QuestionnaireResponse.QuestionnaireResponseItemComponent();

        var answer = new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent();
        answer.setValue(value);
        item.getAnswer().add(answer);

        item.setLinkId(linkId);

        return item;
    }

    private Questionnaire.QuestionnaireItemComponent buildQuestionItem(String linkId) {
        var item = new Questionnaire.QuestionnaireItemComponent();

        item.setType(Questionnaire.QuestionnaireItemType.INTEGER);
        item.setLinkId(linkId);

        return item;
    }

    private QuestionnaireResponseModel buildQuestionnaireResponseModel() {
        QuestionnaireResponseModel model = new QuestionnaireResponseModel();

        model.setAnswered(Instant.parse("2021-11-03T00:00:00Z"));

        model.setQuestionAnswerPairs(new ArrayList<>());

        QuestionModel question = new QuestionModel();
        AnswerModel answer = new AnswerModel();
        answer.setAnswerType(AnswerType.INTEGER);
        answer.setValue("2");

        model.getQuestionAnswerPairs().add(new QuestionAnswerPairModel(question, answer));

        PatientModel patientModel = new PatientModel();
        patientModel.setId("patient-1");
        model.setPatient(patientModel);

        return model;
    }
}