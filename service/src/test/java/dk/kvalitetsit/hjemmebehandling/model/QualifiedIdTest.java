package dk.kvalitetsit.hjemmebehandling.model;

import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QualifiedIdTest {
    @Test
    public void testme() {
        PatientModel p = PatientModel.builder()
                .name(new PersonNameModel("family", List.of("given")))
                .cpr(new CPR("0101010101"))
                .build();

        PatientModel p2 = PatientModel.builder()
                .name(new PersonNameModel("family", List.of("given")))
                .cpr(new CPR("0202020202"))
                .build();

        Map<CPR, String> result = Stream.of(p, p2).collect(Collectors.toMap(PatientModel::cpr, u -> u.name().given().getFirst() + " " + u.name().family(), (existing, replacement) -> existing));
        for (CPR key : result.keySet()) {
            System.out.println(key + ": " + result.get(key));
        }
    }

    @Test
    public void getId_plainId_returnsId() {
        String id = "CarePlan/2";
        ResourceType qualifier = ResourceType.CarePlan;
        QualifiedId result = QualifiedId.from(id);
        assertEquals(id, result.qualified());
        assertEquals(qualifier, result.qualifier());
        assertEquals("2", result.unqualified());
    }

    @Test
    public void getId_multipleSlashes_throwsException() {
        String qualifiedId = "Patient/2/3";
        assertThrows(IllegalArgumentException.class, () -> QualifiedId.from(qualifiedId));
    }

    @Test
    public void getId_illegalQualifier_throwsException() {
        String qualifiedId = "Car/2";
        assertThrows(IllegalArgumentException.class, () -> QualifiedId.from(qualifiedId));
    }

    @Test
    public void getId_illegalId_throwsException() {
        String id = "###";
        ResourceType qualifier = ResourceType.Questionnaire;
        assertThrows(IllegalArgumentException.class, () -> QualifiedId.from(qualifier, id));
    }

    @Test
    public void getId_validQualifiedId_returnsPlainPart() {
        String qualifiedId = "CarePlan/3";
        QualifiedId result = QualifiedId.from(qualifiedId);
        assertEquals("3", result.unqualified());
    }

    @Test
    public void getQualifier_malformedId_throwsException() {
        String qualifiedId = "Patient/()";
        assertThrows(IllegalArgumentException.class, () -> QualifiedId.from(qualifiedId).qualifier());
    }

    @Test
    public void toString_success() {
        String id = "2";
        ResourceType qualifier = ResourceType.Patient;
        String result = QualifiedId.from(qualifier, id).toString();
        assertEquals("Patient/2", result);
    }
}