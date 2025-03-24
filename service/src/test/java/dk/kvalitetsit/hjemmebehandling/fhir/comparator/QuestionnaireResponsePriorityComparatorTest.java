package dk.kvalitetsit.hjemmebehandling.fhir.comparator;

import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.TriagingCategory;
import dk.kvalitetsit.hjemmebehandling.fhir.ExtensionMapper;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.time.Instant;
import java.util.ArrayList;
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
    public void ensure_correct_triagingCategoryPriority_order() {
        assertTrue((TriagingCategory.GREEN.getPriority() - TriagingCategory.GREEN.getPriority()) == 0);
        assertTrue((TriagingCategory.YELLOW.getPriority() - TriagingCategory.YELLOW.getPriority()) == 0);
        assertTrue((TriagingCategory.RED.getPriority() - TriagingCategory.RED.getPriority()) == 0);

        assertTrue((TriagingCategory.RED.getPriority() - TriagingCategory.YELLOW.getPriority()) < 0);
        assertTrue((TriagingCategory.RED.getPriority() - TriagingCategory.GREEN.getPriority()) < 0);

        assertTrue((TriagingCategory.YELLOW.getPriority() - TriagingCategory.RED.getPriority()) > 0);
        assertTrue((TriagingCategory.YELLOW.getPriority() - TriagingCategory.GREEN.getPriority()) < 0);

        assertTrue((TriagingCategory.GREEN.getPriority() - TriagingCategory.YELLOW.getPriority()) > 0);
        assertTrue((TriagingCategory.GREEN.getPriority() - TriagingCategory.RED.getPriority()) > 0);
    }

    @Test
    public void ensure_correct_examinationStatusPriority_order() {
        assertTrue((ExaminationStatus.UNDER_EXAMINATION.getPriority() - ExaminationStatus.UNDER_EXAMINATION.getPriority()) == 0);
        assertTrue((ExaminationStatus.NOT_EXAMINED.getPriority() - ExaminationStatus.NOT_EXAMINED.getPriority()) == 0);
        assertTrue((ExaminationStatus.EXAMINED.getPriority() - ExaminationStatus.EXAMINED.getPriority()) == 0);

        assertTrue((ExaminationStatus.UNDER_EXAMINATION.getPriority() - ExaminationStatus.NOT_EXAMINED.getPriority()) < 0);
        assertTrue((ExaminationStatus.UNDER_EXAMINATION.getPriority() - ExaminationStatus.EXAMINED.getPriority()) < 0);

        assertTrue((ExaminationStatus.NOT_EXAMINED.getPriority() - ExaminationStatus.UNDER_EXAMINATION.getPriority()) > 0);
        assertTrue((ExaminationStatus.NOT_EXAMINED.getPriority() - ExaminationStatus.EXAMINED.getPriority()) < 0);

        assertTrue((ExaminationStatus.EXAMINED.getPriority() - ExaminationStatus.UNDER_EXAMINATION.getPriority()) > 0);
        assertTrue((ExaminationStatus.EXAMINED.getPriority() - ExaminationStatus.NOT_EXAMINED.getPriority()) > 0);
    }

    @Test
    public void ensure_correct_triagingCategory_sorting() {
        // Arrange
        QuestionnaireResponse green = buildQuestionnaireResponse("1",TriagingCategory.GREEN, ExaminationStatus.NOT_EXAMINED, AUTHORED);
        QuestionnaireResponse yellow = buildQuestionnaireResponse("2",TriagingCategory.YELLOW, ExaminationStatus.NOT_EXAMINED, AUTHORED);
        QuestionnaireResponse red = buildQuestionnaireResponse("3",TriagingCategory.RED, ExaminationStatus.NOT_EXAMINED, AUTHORED);

        // Act

        // Assert
        assertTrue( subject.compare(green, green) == 0);
        assertTrue( subject.compare(yellow, yellow) == 0);
        assertTrue( subject.compare(red, red) == 0);

        assertTrue( subject.compare(green, yellow) > 0);
        assertTrue( subject.compare(green, red) > 0);

        assertTrue( subject.compare(yellow, green) < 0);
        assertTrue( subject.compare(yellow, red) > 0);

        assertTrue( subject.compare(red, green) < 0);
        assertTrue( subject.compare(red, yellow) < 0);
    }

    @Test
    public void ensure_correct_examinationStatus_sorting() {
        // Arrange
        QuestionnaireResponse examined = buildQuestionnaireResponse("1",TriagingCategory.GREEN, ExaminationStatus.EXAMINED, AUTHORED);
        QuestionnaireResponse notExamined = buildQuestionnaireResponse("2",TriagingCategory.GREEN, ExaminationStatus.NOT_EXAMINED, AUTHORED);
        QuestionnaireResponse underExamination = buildQuestionnaireResponse("3",TriagingCategory.GREEN, ExaminationStatus.UNDER_EXAMINATION, AUTHORED);

        // Act

        // Assert
        assertTrue( subject.compare(examined, examined) == 0);
        assertTrue( subject.compare(notExamined, notExamined) == 0);
        assertTrue( subject.compare(underExamination, underExamination) == 0);

        assertTrue( subject.compare(examined, underExamination) > 0);
        assertTrue( subject.compare(examined, notExamined) > 0);

        assertTrue( subject.compare(notExamined, underExamination) > 0);
        assertTrue( subject.compare(notExamined, examined) < 0);

        assertTrue( subject.compare(underExamination, notExamined) < 0);
        assertTrue( subject.compare(underExamination, examined) < 0);
    }

    @Test
    public void ensure_correct_authored_sorting() {
        // Arrange
        var earlier = Instant.parse("2021-11-09T00:00:00Z");
        var later = Instant.parse("2022-11-09T00:00:00Z");
        QuestionnaireResponse first = buildQuestionnaireResponse("1", TriagingCategory.GREEN, ExaminationStatus.EXAMINED, earlier);
        QuestionnaireResponse last = buildQuestionnaireResponse("2", TriagingCategory.GREEN, ExaminationStatus.EXAMINED, later);

        // Act

        // Assert
        assertTrue( subject.compare(first, first) == 0);
        assertTrue( subject.compare(last, last) == 0);

        assertTrue( subject.compare(first, last) < 0);
        assertTrue( subject.compare(last, first) > 0);

    }


    @Test
    public void compare_considersTriagingCategory() {
        // Arrange
        QuestionnaireResponse first = buildQuestionnaireResponse("1",TriagingCategory.YELLOW, ExaminationStatus.NOT_EXAMINED, AUTHORED);
        QuestionnaireResponse second = buildQuestionnaireResponse("2",TriagingCategory.RED, ExaminationStatus.NOT_EXAMINED, AUTHORED);



        // Act
        var list = new ArrayList<QuestionnaireResponse>();
        list.add(first);
        list.add(second);

        list.sort(subject);

        // assert
        QuestionnaireResponse firstElement = list.get(0);
        assertEquals(second.getId(),firstElement.getId());
    }

    @Test
    public void compare_considersExaminationStatus() {
        // Arrange
        QuestionnaireResponse first = buildQuestionnaireResponse("1",TriagingCategory.GREEN, ExaminationStatus.UNDER_EXAMINATION, AUTHORED);
        QuestionnaireResponse second = buildQuestionnaireResponse("2",TriagingCategory.GREEN, ExaminationStatus.NOT_EXAMINED, AUTHORED);

        // Act
        var list = new ArrayList<QuestionnaireResponse>();
        list.add(first);
        list.add(second);

        list.sort(subject);
        QuestionnaireResponse firstElement = list.get(0);

        // assert
        assertEquals(first.getId(),firstElement.getId());
    }

    @Test
    public void compare_considersAnswerDate() {
        // Arrange
        QuestionnaireResponse first = buildQuestionnaireResponse("1",TriagingCategory.GREEN, ExaminationStatus.EXAMINED, AUTHORED);
        QuestionnaireResponse second = buildQuestionnaireResponse("2",TriagingCategory.GREEN, ExaminationStatus.EXAMINED, AUTHORED.plusSeconds(10L));

        // Act
        var list = new ArrayList<QuestionnaireResponse>();
        list.add(first);
        list.add(second);

        list.sort(subject);
        QuestionnaireResponse firstElement = list.get(0);

        // assert
        assertEquals(first.getId(),firstElement.getId());
    }

    @Test
    public void compare_considersAnswerDate_sda() {

        var earlier = Instant.parse("2020-11-09T00:00:00Z");
        var middle = Instant.parse("2021-11-09T00:00:00Z");
        var later = Instant.parse("2022-11-09T00:00:00Z");
        // Arrange
        QuestionnaireResponse first = buildQuestionnaireResponse("1",TriagingCategory.GREEN, ExaminationStatus.NOT_EXAMINED, earlier);
        QuestionnaireResponse second = buildQuestionnaireResponse("2",TriagingCategory.RED, ExaminationStatus.NOT_EXAMINED, middle);
        QuestionnaireResponse third = buildQuestionnaireResponse("3",TriagingCategory.GREEN, ExaminationStatus.NOT_EXAMINED, later);

        var list = new ArrayList<QuestionnaireResponse>();
        list.add(first);
        list.add(second);
        list.add(third);

        list.sort(subject);

        assertEquals(second.getAuthored(),list.get(0).getAuthored());
    }

    @Test
    public void compare_indistinguishable() {
        // Arrange
        QuestionnaireResponse first = buildQuestionnaireResponse("1",TriagingCategory.GREEN, ExaminationStatus.EXAMINED, AUTHORED);
        QuestionnaireResponse second = buildQuestionnaireResponse("2",TriagingCategory.GREEN, ExaminationStatus.EXAMINED, AUTHORED);

        // Act
        var list = new ArrayList<QuestionnaireResponse>();
        list.add(first);
        list.add(second);

        list.sort(subject);
        QuestionnaireResponse firstElement = list.get(0);

        // assert
        assertEquals(first.getId(),firstElement.getId());
    }

    private QuestionnaireResponse buildQuestionnaireResponse(String id, TriagingCategory triagingCategory, ExaminationStatus examinationStatus, Instant authored) {
        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setId(id);
        response.addExtension(ExtensionMapper.mapTriagingCategory(triagingCategory));
        response.addExtension(ExtensionMapper.mapExaminationStatus(examinationStatus));
        response.setAuthored(Date.from(authored));

        return response;
    }
}