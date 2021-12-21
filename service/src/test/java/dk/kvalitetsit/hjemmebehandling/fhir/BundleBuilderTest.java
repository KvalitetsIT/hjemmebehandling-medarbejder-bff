package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BundleBuilderTest {
    private BundleBuilder subject = new BundleBuilder();

    private static final String CAREPLAN_ID = "careplan-1";
    private static final String PATIENT_ID = "patient-1";

    @Test
    public void buildCreateCarePlanBundle_mapsArgumentsToEntries() {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID, PATIENT_ID);
        Patient patient = buildPatient(PATIENT_ID);

        // Act
        Bundle result = subject.buildCreateCarePlanBundle(carePlan, patient);

        // Assert
        assertEquals(2, result.getEntry().size());
    }

    @Test
    public void buildCreateCarePlanBundle_updatesSubjectReference() {
        // Arrange
        CarePlan carePlan = buildCarePlan(CAREPLAN_ID, PATIENT_ID);
        Patient patient = buildPatient(PATIENT_ID);

        // Act
        Bundle result = subject.buildCreateCarePlanBundle(carePlan, patient);

        // Assert
        assertEquals(patient, result.getEntry().get(1).getResource());
        assertEquals(carePlan.getSubject().getReference(), result.getEntry().get(1).getFullUrl());
    }

    private CarePlan buildCarePlan(String carePlanId, String patientId) {
        CarePlan carePlan = new CarePlan();

        carePlan.setId(carePlanId);
        carePlan.setSubject(new Reference(patientId));

        return carePlan;
    }

    private Patient buildPatient(String patientId) {
        Patient patient = new Patient();

        patient.setId(patientId);

        return patient;
    }
}