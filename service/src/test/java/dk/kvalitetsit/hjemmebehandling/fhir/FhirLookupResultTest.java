package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FhirLookupResultTest {
    private static final String PLANDEFINITION_ID_1 = "plandefinition-1";
    private static final String PLANDEFINITION_ID_2 = "plandefinition-2";
    private static final String QUESTIONNAIRE_ID_1 = "questionnaire-1";
    private static final String QUESTIONNAIRE_ID_2 = "questionnaire-2";

    @Test
    public void fromBuild_unknownResourceType_throwsException() {
        Bundle bundle = buildBundle(new AdverseEvent());
        assertThrows(IllegalArgumentException.class, () -> FhirLookupResult.fromBundle(bundle));
    }

    @Test
    public void getPlanDefinitions_planDefinitionPresent_success() {
        PlanDefinition planDefinition = buildPlanDefinition(PLANDEFINITION_ID_1);
        Bundle bundle = buildBundle(planDefinition);
        FhirLookupResult result = FhirLookupResult.fromBundle(bundle);
        assertEquals(1, result.getPlanDefinitions().size());
        assertEquals(planDefinition, result.getPlanDefinition(PLANDEFINITION_ID_1).get());
    }

    @Test
    public void getPlanDefinitions_planDefinitionMissing_throwsException() {
        Bundle bundle = buildBundle();
        FhirLookupResult result = FhirLookupResult.fromBundle(bundle);
        assertEquals(0, result.getPlanDefinitions().size());
        assertFalse(result.getPlanDefinition(PLANDEFINITION_ID_1).isPresent());
    }

    @Test
    public void getQuestionnaires_questionnairePresent_success() {
        Questionnaire questionnaire = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        Bundle bundle = buildBundle(questionnaire);
        FhirLookupResult result = FhirLookupResult.fromBundle(bundle);
        assertEquals(1, result.getQuestionnaires().size());
        assertEquals(questionnaire, result.getQuestionnaire(QUESTIONNAIRE_ID_1).get());
    }

    @Test
    public void getQuestionnaires_questionnaireMissing_throwsException() {
        Bundle bundle = buildBundle();
        FhirLookupResult result = FhirLookupResult.fromBundle(bundle);
        assertEquals(0, result.getQuestionnaires().size());
        assertFalse(result.getQuestionnaire(QUESTIONNAIRE_ID_1).isPresent());
    }

    @Test
    public void values_returnsAllResources() {
        PlanDefinition planDefinition1 = buildPlanDefinition(PLANDEFINITION_ID_1);
        PlanDefinition planDefinition2 = buildPlanDefinition(PLANDEFINITION_ID_2);
        Questionnaire questionnaire1 = buildQuestionnaire(QUESTIONNAIRE_ID_1);
        Questionnaire questionnaire2 = buildQuestionnaire(QUESTIONNAIRE_ID_2);
        FhirLookupResult lookupResult = FhirLookupResult.fromResources(planDefinition1, planDefinition2, questionnaire1, questionnaire2);
        List<DomainResource> result = lookupResult.values();
        assertEquals(4, result.size());
        assertEquals(List.of(planDefinition1, planDefinition2, questionnaire1, questionnaire2), result);
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
        for (var resource : resources) {
            var entry = new Bundle.BundleEntryComponent();
            entry.setResource(resource);
            bundle.addEntry(entry);
        }
        return bundle;
    }
}