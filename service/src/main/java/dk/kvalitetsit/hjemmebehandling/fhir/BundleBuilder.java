package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

public class BundleBuilder {
    public Bundle buildCreateCarePlanBundle(CarePlan carePlan, Patient patient) {
        // Build the CarePlan entry.
        var carePlanEntry = buildCarePlanEntry(carePlan, Bundle.HTTPVerb.POST);

        // Build the Patient entry.
        var patientEntry = buildPatientEntry(patient, Bundle.HTTPVerb.POST);

        // Alter the subject reference to refer to the Patient entry in the bundle (the Patient does not exist yet).
        carePlan.getSubject().setReference(patientEntry.getFullUrl());

        // Build a transaction bundle.
        return buildBundle(carePlanEntry, patientEntry);
    }

    public Bundle buildUpdateCarePlanBundle(CarePlan carePlan, Patient patient) {
        // Build the CarePlan entry.
        var carePlanEntry = buildCarePlanEntry(carePlan, Bundle.HTTPVerb.PUT);

        // Build the Patient entry.
        var patientEntry = buildPatientEntry(patient, Bundle.HTTPVerb.PUT);

        // Build a transaction bundle.
        return buildBundle(carePlanEntry, patientEntry);
    }

    private Bundle buildBundle(Bundle.BundleEntryComponent... entries) {
        var bundle = new Bundle();

        bundle.setType(Bundle.BundleType.TRANSACTION);
        for (var entry : entries) {
            bundle.addEntry(entry);
        }

        return bundle;
    }

    private Bundle.BundleEntryComponent buildCarePlanEntry(CarePlan carePlan, Bundle.HTTPVerb method) {
        return buildEntry(carePlan, "CarePlan/careplan-entry", method);
    }

    private Bundle.BundleEntryComponent buildPatientEntry(Patient patient, Bundle.HTTPVerb method) {
        return buildEntry(patient, "Patient/patient-entry", method);
    }

    private Bundle.BundleEntryComponent buildEntry(Resource resource, String fullUrl, Bundle.HTTPVerb method) {
        var entry = new Bundle.BundleEntryComponent();

        entry.setFullUrl(fullUrl);
        entry.setRequest(new Bundle.BundleEntryRequestComponent());
        entry.getRequest().setMethod(method);
        entry.getRequest().setUrl(resource.getId());
        entry.setResource(resource);

        return entry;
    }
}
