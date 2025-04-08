package dk.kvalitetsit.hjemmebehandling.model;

import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QualifiedIdTest {
    @Test
    public void testme() {
        PatientModel p = PatientModel.builder()
                .cpr("cpr")
                .givenName("given").
                familyName("family")
                .build();

        PatientModel p2 = PatientModel.builder()
                .cpr("cpr")
                .givenName("given")
                .familyName("family")
                .build();

        Map<String, String> result = Stream.of(p, p2)
                .collect(Collectors.toMap(PatientModel::cpr, u -> u.givenName() + " " + u.familyName(), (existing, replacement) -> existing));
        for (String key : result.keySet()) {
            System.out.println(key + ": " + result.get(key));
        }
    }

    @Test
    public void getId_plainId_returnsId() {
        String id = "2";
        ResourceType qualifier = ResourceType.CarePlan;
        String result = new QualifiedId(id, qualifier).id();
        assertEquals(id, result);
    }

    @Test
    public void getId_multipleSlashes_throwsException() {
        String qualifiedId = "Patient/2/3";
        assertThrows(IllegalArgumentException.class, () -> new QualifiedId(qualifiedId).id());
    }

    @Test
    public void getId_illegalQualifier_throwsException() {
        String qualifiedId = "Car/2";
        assertThrows(IllegalArgumentException.class, () -> new QualifiedId(qualifiedId).id());
    }

    @Test
    public void getId_illegalId_throwsException() {
        String id = "###";
        ResourceType qualifier = ResourceType.Questionnaire;
        assertThrows(IllegalArgumentException.class, () -> new QualifiedId(id, qualifier).id());
    }

    @Test
    public void getId_validQualifiedId_returnsPlainPart() {
        String qualifiedId = "CarePlan/3";
        String result = new QualifiedId(qualifiedId).id();
        assertEquals("3", result);
    }

    @Test
    public void getQualifier_malformedId_throwsException() {
        String qualifiedId = "Patient/()";
        assertThrows(IllegalArgumentException.class, () -> new QualifiedId(qualifiedId).qualifier());
    }

    @Test
    public void toString_success() {
        String id = "2";
        ResourceType qualifier = ResourceType.Patient;
        String result = new QualifiedId(id, qualifier).toString();
        assertEquals("Patient/2", result);
    }
}