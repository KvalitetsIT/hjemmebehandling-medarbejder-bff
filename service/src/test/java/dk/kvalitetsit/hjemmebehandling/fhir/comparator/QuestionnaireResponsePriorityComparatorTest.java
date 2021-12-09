package dk.kvalitetsit.hjemmebehandling.fhir.comparator;

import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.TriagingCategory;
import dk.kvalitetsit.hjemmebehandling.fhir.ExtensionMapper;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class QuestionnaireResponsePriorityComparatorTest {
    private QuestionnaireResponsePriorityComparator subject;

    private static final Instant AUTHORED = Instant.parse("2021-11-09T00:00:00Z");

    @BeforeEach
    public void setup() {
        subject = new QuestionnaireResponsePriorityComparator();
    }

    @Test
    public void compare_considersTriagingCategory() {
        // Arrange
        QuestionnaireResponse first = buildQuestionnaireResponse(TriagingCategory.YELLOW, ExaminationStatus.NOT_EXAMINED, AUTHORED);
        QuestionnaireResponse second = buildQuestionnaireResponse(TriagingCategory.RED, ExaminationStatus.NOT_EXAMINED, AUTHORED);

        // Act
        int result = subject.compare(first, second);

        // assert
        assertTrue(result < 0);
    }

    @Test
    public void compare_considersExaminationStatus() {
        // Arrange
        QuestionnaireResponse first = buildQuestionnaireResponse(TriagingCategory.GREEN, ExaminationStatus.UNDER_EXAMINATION, AUTHORED);
        QuestionnaireResponse second = buildQuestionnaireResponse(TriagingCategory.GREEN, ExaminationStatus.NOT_EXAMINED, AUTHORED);

        // Act
        int result = subject.compare(first, second);

        // assert
        assertTrue(result < 0);
    }

    @Test
    public void compare_considersAnswerDate() {
        // Arrange
        QuestionnaireResponse first = buildQuestionnaireResponse(TriagingCategory.GREEN, ExaminationStatus.EXAMINED, AUTHORED);
        QuestionnaireResponse second = buildQuestionnaireResponse(TriagingCategory.GREEN, ExaminationStatus.EXAMINED, AUTHORED.plusSeconds(10L));

        // Act
        int result = subject.compare(first, second);

        // assert
        assertTrue(result < 0);
    }

    @Test
    public void compare_indistinguishable() {
        // Arrange
        QuestionnaireResponse first = buildQuestionnaireResponse(TriagingCategory.GREEN, ExaminationStatus.EXAMINED, AUTHORED);
        QuestionnaireResponse second = buildQuestionnaireResponse(TriagingCategory.GREEN, ExaminationStatus.EXAMINED, AUTHORED);

        // Act
        int result = subject.compare(first, second);

        // assert
        assertEquals(0, result);
    }

    private QuestionnaireResponse buildQuestionnaireResponse(TriagingCategory triagingCategory, ExaminationStatus examinationStatus, Instant authored) {
        QuestionnaireResponse response = new QuestionnaireResponse();

        response.addExtension(ExtensionMapper.mapTriagingCategory(triagingCategory));
        response.addExtension(ExtensionMapper.mapExaminationStatus(examinationStatus));
        response.setAuthored(Date.from(authored));

        return response;
    }
}