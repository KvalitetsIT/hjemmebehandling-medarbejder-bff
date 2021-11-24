package dk.kvalitetsit.hjemmebehandling.configuration;

import dk.kvalitetsit.hjemmebehandling.context.UserContextInterceptor;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.comparator.QuestionnaireResponsePriorityComparator;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ca.uhn.fhir.context.FhirContext;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirObjectBuilder;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.PersonService;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireResponseService;

@Configuration
public class ServiceConfiguration {
	
	@Value("${user.context.handler}")
	private String userContextHandler;
	
	@Value("${fhir.server.url}")
	private String fhirServerUrl;

    @Bean
    public CarePlanService getCarePlanService(@Autowired FhirClient client, @Autowired FhirMapper mapper, @Autowired FhirObjectBuilder builder, @Autowired DateProvider dateProvider, @Autowired AccessValidator accessValidator) {
        return new CarePlanService(client, mapper, builder, dateProvider, accessValidator);
    }

    @Bean
    public PatientService getPatientService(@Autowired FhirClient client, @Autowired FhirMapper mapper, @Autowired AccessValidator accessValidator) {
        return new PatientService(client, mapper, accessValidator);
    }
    
    @Bean
    public PersonService getPersonService() {
    	return new PersonService(new RestTemplate());
    }

    @Bean
    public QuestionnaireResponseService getQuestionnaireResponseService(@Autowired FhirClient client, @Autowired FhirMapper mapper, @Autowired FhirObjectBuilder builder, @Autowired QuestionnaireResponsePriorityComparator priorityComparator, @Autowired AccessValidator accessValidator) {
        // Reverse the comporator: We want responses by descending priority.
        return new QuestionnaireResponseService(client, mapper, builder, priorityComparator.reversed(), accessValidator);
    }

    @Bean
    public FhirClient getFhirClient(@Autowired UserContextProvider userContextProvider) {
        FhirContext context = FhirContext.forR4();
        return new FhirClient(context, fhirServerUrl, userContextProvider);
    }

    @Bean
    public WebMvcConfigurer getWebMvcConfigurer(@Value("${allowed_origins}") String allowedOrigins, @Autowired UserContextProvider userContextProvider) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins(allowedOrigins).allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new UserContextInterceptor(userContextProvider,userContextHandler));
            }
        };
    }
}
