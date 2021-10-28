package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class FhirMapperTest {
    private FhirMapper subject = new FhirMapper();

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
}