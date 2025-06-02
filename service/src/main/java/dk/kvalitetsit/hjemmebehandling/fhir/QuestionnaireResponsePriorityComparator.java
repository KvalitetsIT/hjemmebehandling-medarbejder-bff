package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class QuestionnaireResponsePriorityComparator implements Comparator<QuestionnaireResponse> {
    /**
     * Define an ordering on QuestionnaireResponses. The ordering is defined by TriagingCategory and ExaminationStatus (extensions) and submission date.
     */
    @Override
    public int compare(QuestionnaireResponse first, QuestionnaireResponse second) {
        // Compare by TriagingCategory
        var firstTriagingCategory = ExtensionMapper.extractTriagingCategory(first.getExtension());
        var secondTriagingCategory = ExtensionMapper.extractTriagingCategory(second.getExtension());
        if (firstTriagingCategory != secondTriagingCategory) {
            // The response with the most severe category is prioritized.
            return firstTriagingCategory.getPriority() - secondTriagingCategory.getPriority();
        }

        // Compare by ExaminationStatus
        var firstExaminationStatus = ExtensionMapper.extractExaminationStatus(first.getExtension());
        var secondExaminationStatus = ExtensionMapper.extractExaminationStatus(second.getExtension());
        if (firstExaminationStatus != secondExaminationStatus) {
            // Responses that are under examination are prioritized.
            return firstExaminationStatus.getPriority() - secondExaminationStatus.getPriority();
        }

        // Compare by submission date
        if (!first.getAuthored().equals(second.getAuthored())) {
            return first.getAuthored().compareTo(second.getAuthored());
        }

        return 0;
    }
}
