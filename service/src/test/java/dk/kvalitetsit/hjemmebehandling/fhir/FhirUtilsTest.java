package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FhirUtilsTest {

    @Test
    public void qualifyId_qualifiedId_returnsId() {
        String id = "4";
        QualifiedId result = QualifiedId.from(ResourceType.CarePlan, id);
        assertEquals(id, result.unqualified());
        assertEquals("CarePlan/4", result.qualified());
    }

    @Test
    public void qualifiedId_from_qualified_id_returnsId() {
        String id = "CarePlan/4";
        assertThrows(IllegalArgumentException.class, () -> QualifiedId.from(ResourceType.CarePlan, id));
    }

    @Test
    public void qualifyId_qualifierMismatch_throwsException() {
        String id = "CarePlan/4";
        assertThrows(IllegalArgumentException.class, () -> QualifiedId.from(ResourceType.Questionnaire, id));
    }

    @Test
    public void qualifiedId_from_malformedId_throwsException() {
        String id = "Patient/()";
        assertThrows(IllegalArgumentException.class, () -> QualifiedId.from(ResourceType.Patient, id));
    }

    @Test
    public void qualifiedId_from_patient_success() {
        String id = "2";
        QualifiedId result = QualifiedId.from(ResourceType.Patient, id);
        assertEquals("Patient/2", result.qualified());
        assertEquals("2", result.unqualified());
    }
    
    @Test
    public void qualifiedId_from_success() {
        String qualifiedId = "CarePlan/2";
        assertDoesNotThrow(() -> QualifiedId.from(qualifiedId));
    }

    @Test
    public void qualifiedId_from_multipleSlashes_failure() {
        String qualifiedId = "Patient/2/3";
        assertThrows(IllegalArgumentException.class, () -> QualifiedId.from(qualifiedId));
    }

    @Test
    public void qualifiedId_from_illegalQualifier_failure() {
        String qualifiedId = "Car/2";
        assertThrows(IllegalArgumentException.class, () -> QualifiedId.from(qualifiedId));
    }

    @Test
    public void qualifiedId_from_illegalId_failure() {
        String qualifiedId = "Questionnaire/###";
        assertThrows(IllegalArgumentException.class, () -> QualifiedId.from(qualifiedId));
    }
}