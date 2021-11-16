package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

public class BundleBuilder {
    public Bundle buildCarePlanBundle(CarePlan carePlan, Patient patient) {
        // Build the CarePlan entry.
        var carePlanEntry = buildCarePlanEntry(carePlan);

        // Build the Patient entry.
        var patientEntry = buildPatientEntry(patient);

        // Alter the subject reference to refer to the Patient entry in the bundle (the Patient does not exist yet).
        carePlan.getSubject().setReference(patientEntry.getFullUrl());

        // Build a transaction bundle.
        var bundle = new Bundle();

        bundle.setType(Bundle.BundleType.TRANSACTION);
        bundle.addEntry(carePlanEntry);
        bundle.addEntry(patientEntry);

        return bundle;
    }

    private Bundle.BundleEntryComponent buildCarePlanEntry(CarePlan carePlan) {
        return buildEntry(carePlan, "CarePlan/careplan-entry");
    }

    private Bundle.BundleEntryComponent buildPatientEntry(Patient patient) {
        return buildEntry(patient, "Patient/patient-entry");
    }

    private Bundle.BundleEntryComponent buildEntry(Resource resource, String fullUrl) {
        var entry = new Bundle.BundleEntryComponent();

        entry.setFullUrl(fullUrl);
        entry.setRequest(new Bundle.BundleEntryRequestComponent());
        entry.getRequest().setMethod(Bundle.HTTPVerb.POST);
        entry.setResource(resource);

        return entry;
    }
}
