package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CarePlanService {
    private static final Logger logger = LoggerFactory.getLogger(CarePlanService.class);

    private FhirClient fhirClient;

    private FhirObjectBuilder fhirObjectBuilder;

    public CarePlanService(FhirClient fhirClient, FhirObjectBuilder fhirObjectBuilder) {
        this.fhirClient = fhirClient;
        this.fhirObjectBuilder = fhirObjectBuilder;
    }

    public String createCarePlan(String cpr, String planDefinitionId) throws ServiceException {
        // Look up the Patient identified by the cpr.
        Patient patient = fhirClient.lookupPatient(cpr);

        // Look up the PlanDefinition based on the planDefinitionId
        PlanDefinition planDefinition = fhirClient.lookupPlanDefinition(planDefinitionId);

        // Based on that, build a CarePlan
        CarePlan carePlan = fhirObjectBuilder.buildCarePlan(patient, planDefinition);

        // Save the carePlan, return the id.
        try {
            return fhirClient.saveCarePlan(carePlan);
        }
        catch(Exception e) {
            throw new ServiceException("Error saving CarePlan", e);
        }
    }
}
