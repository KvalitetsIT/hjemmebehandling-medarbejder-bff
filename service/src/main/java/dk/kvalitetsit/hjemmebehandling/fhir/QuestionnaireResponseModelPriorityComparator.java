package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class QuestionnaireResponseModelPriorityComparator implements Comparator<QuestionnaireResponseModel> {
    /**
     * Define an ordering on QuestionnaireResponses. The ordering is defined by TriagingCategory and ExaminationStatus (extensions) and submission date.
     */
    @Override
    public int compare(QuestionnaireResponseModel first, QuestionnaireResponseModel second) {
        // Compare by TriagingCategory

        var firstTriagingCategory = first.triagingCategory();
        var secondTriagingCategory = second.triagingCategory();
        if (firstTriagingCategory != secondTriagingCategory) {
            // The response with the most severe category is prioritized.
            return firstTriagingCategory.getPriority() - secondTriagingCategory.getPriority();
        }

        // Compare by ExaminationStatus
        var firstExaminationStatus = first.examinationStatus();
        var secondExaminationStatus = second.examinationStatus();
        if (firstExaminationStatus != secondExaminationStatus) {
            // Responses that are under examination are prioritized.
            return firstExaminationStatus.getPriority() - secondExaminationStatus.getPriority();
        }

        // Compare by submission date
        if (!first.answered().equals(second.answered())) {
            return first.answered().compareTo(second.answered());
        }

        return 0;
    }
}
