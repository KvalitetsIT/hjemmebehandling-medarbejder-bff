package dk.kvalitetsit.hjemmebehandling.configuration;

import ca.uhn.fhir.context.FhirContext;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class ServiceConfiguration {
    @Bean
    public CarePlanService getCarePlanService(@Autowired FhirClient client, @Autowired FhirMapper mapper, @Autowired FhirObjectBuilder builder) {
        return new CarePlanService(client, mapper, builder);
    }

    @Bean
    public PatientService getPatientService(@Autowired FhirClient client, @Autowired FhirMapper mapper) {
        return new PatientService(client, mapper);
    }

    @Bean
    public QuestionnaireResponseService getQuestionnaireResponseService(@Autowired FhirClient client, @Autowired FhirMapper mapper, @Autowired FhirObjectBuilder builder) {
        return new QuestionnaireResponseService(client, mapper, builder);
    }

    @Bean
    public FhirClient getFhirClient() {
        FhirContext context = FhirContext.forR4();
        String endpoint = "http://hapi-server:8080/fhir";
        return new FhirClient(context, endpoint);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer(@Value("${allowed_origins}") String allowedOrigins) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins(allowedOrigins).allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
            }
        };
    }
}
