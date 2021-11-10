package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FhirLookupResultTest {
    private static final String PLANDEFINITION_ID_1 = "plandefinition-1";
    private static final String QUESTIONNAIRE_ID_1 = "questionnaire-1";

    @Test
    public void fromBuild_unknownResourceType_throwsException() {
        // Arrange
        Bundle bundle = buildBundle(new AdverseEvent());

        // Act

        // Assert
        assertThrows(IllegalArgumentException.class, () -> FhirLookupResult.fromBundle(bundle));
    }

    @Test
    public void getPlanDefinitions_planDefinitionPresent_success() {
        // Arrange
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        Bundle bundle = buildBundle(planDefinition);

        // Act
        FhirLookupResult result = FhirLookupResult.fromBundle(bundle);

        // Assert
        assertEquals(1, result.getPlanDefinitions().size());
        assertEquals(planDefinition, result.getPlanDefinition(PLANDEFINITION_ID_1));
    }

    @Test
    public void getPlanDefinitions_planDefinitionMissing_throwsException() {
        // Arrange
        Bundle bundle = buildBundle();

        // Act
        FhirLookupResult result = FhirLookupResult.fromBundle(bundle);

        // Assert
        assertThrows(IllegalArgumentException.class, () -> result.getPlanDefinition(PLANDEFINITION_ID_1));
    }

    @Test
    public void getQuestionnaires_questionnairePresent_success() {
        // Arrange
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        Bundle bundle = buildBundle(questionnaire);

        // Act
        FhirLookupResult result = FhirLookupResult.fromBundle(bundle);

        // Assert
        assertEquals(1, result.getQuestionnaires().size());
        assertEquals(questionnaire, result.getQuestionnaire(QUESTIONNAIRE_ID_1));
    }

    @Test
    public void getQuestionnaires_questionnaireMissing_throwsException() {
        // Arrange
        Bundle bundle = buildBundle();

        // Act
        FhirLookupResult result = FhirLookupResult.fromBundle(bundle);

        // Assert
        assertThrows(IllegalArgumentException.class, () -> result.getQuestionnaire(PLANDEFINITION_ID_1));
    }

    private PlanDefinition buildPlanDefinition(String planDefinitionId) {
        PlanDefinition planDefinition = new PlanDefinition();

        planDefinition.setId(planDefinitionId);

        return planDefinition;
    }

    private Questionnaire buildQuestionnaire(String questionnaireId) {
        Questionnaire questionnaire = new Questionnaire();

        questionnaire.setId(questionnaireId);

        return questionnaire;
    }

    private Bundle buildBundle(Resource... resources) {
        Bundle bundle = new Bundle();

        for(var resource: resources) {
            var entry = new Bundle.BundleEntryComponent();
            entry.setResource(resource);
            bundle.addEntry(entry);
        }

        return bundle;
    }
}