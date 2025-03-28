package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FhirUtilsTest {
    @Test
    public void qualifyId_qualifiedId_returnsId() {

        String id = "CarePlan/4";


        String result = FhirUtils.qualifyId(id, ResourceType.CarePlan);


        assertEquals(id, result);
    }

    @Test
    public void qualifyId_qualifierMismatch_throwsException() {

        String id = "CarePlan/4";


        assertThrows(IllegalArgumentException.class, () -> FhirUtils.qualifyId(id, ResourceType.Questionnaire));
    }

    @Test
    public void qualifyId_malformedId_throwsException() {

        String id = "Patient/()";


        assertThrows(IllegalArgumentException.class, () -> FhirUtils.qualifyId(id, ResourceType.Patient));
    }

    @Test
    public void qualifyId_plainId_success() {

        String id = "2";


        String result = FhirUtils.qualifyId(id, ResourceType.Patient);


        assertEquals("Patient/2", result);
    }

    @Test
    public void isPlainId_plainId_success() {

        String id = "2";


        assertTrue(FhirUtils.isPlainId(id));
    }

    @Test
    public void isPlainId_slashes_failure() {

        String id = "CarePlan/2";


        assertFalse(FhirUtils.isPlainId(id));
    }

    @Test
    public void isQualifiedId_qualifiedId_success() {

        String qualifiedId = "CarePlan/2";
        ResourceType qualifier = ResourceType.CarePlan;


        assertTrue(FhirUtils.isQualifiedId(qualifiedId, qualifier));
    }

    @Test
    public void isQualifiedId_multipleSlashes_failure() {

        String qualifiedId = "Patient/2/3";
        ResourceType qualifier = ResourceType.Patient;


        assertFalse(FhirUtils.isQualifiedId(qualifiedId, qualifier));
    }

    @Test
    public void isQualifiedId_illegalQualifier_failure() {

        String qualifiedId = "Car/2";
        ResourceType qualifier = ResourceType.Patient;


        assertFalse(FhirUtils.isQualifiedId(qualifiedId, qualifier));
    }

    @Test
    public void isQualifiedId_illegalId_failure() {

        String qualifiedId = "Questionnaire/###";
        ResourceType qualifier = ResourceType.Questionnaire;


        assertFalse(FhirUtils.isQualifiedId(qualifiedId, qualifier));
    }
}