package dk.kvalitetsit.hjemmebehandling.configuration;

import ca.uhn.fhir.context.FhirContext;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {
    @Bean
    public CarePlanService getCarePlanService(@Autowired FhirClient client, @Autowired FhirObjectBuilder builder) {
        return new CarePlanService(client, builder);
    }

    @Bean
    public PatientService getPatientService(@Autowired FhirClient client, @Autowired FhirMapper mapper) {
        return new PatientService(client, mapper);
    }

    @Bean
    public FhirClient getFhirClient() {
        FhirContext context = FhirContext.forR4();
        String endpoint = "http://hapi-server:8080/fhir";
        return new FhirClient(context, endpoint);
    }
}
