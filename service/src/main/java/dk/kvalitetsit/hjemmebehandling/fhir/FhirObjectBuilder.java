package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.springframework.stereotype.Component;

@Component
public class FhirObjectBuilder {
    public CarePlan buildCarePlan(Patient patient, PlanDefinition planDefinition) {
        CarePlan carePlan = new CarePlan();


        return carePlan;
    }
}
