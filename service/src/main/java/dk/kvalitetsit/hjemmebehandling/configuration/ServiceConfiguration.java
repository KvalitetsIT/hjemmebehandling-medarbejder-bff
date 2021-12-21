package dk.kvalitetsit.hjemmebehandling.configuration;

import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.context.*;
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
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.CustomUserService;
import dk.kvalitetsit.hjemmebehandling.service.PatientService;
import dk.kvalitetsit.hjemmebehandling.service.PersonService;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireResponseService;

@Configuration
public class ServiceConfiguration {
	
	@Value("${user.mock.context.organization.id}")
	private String mockContextOrganizationId;
	
	@Value("${fhir.server.url}")
	private String fhirServerUrl;

    @Bean
    public CarePlanService getCarePlanService(@Autowired FhirClient client, @Autowired FhirMapper mapper, @Autowired DateProvider dateProvider, @Autowired AccessValidator accessValidator, @Autowired DtoMapper dtoMapper) {
        return new CarePlanService(client, mapper, dateProvider, accessValidator, dtoMapper);
    }

    @Bean
    public PatientService getPatientService(@Autowired FhirClient client, @Autowired FhirMapper mapper, @Autowired AccessValidator accessValidator, @Autowired DtoMapper dtoMapper) {
        return new PatientService(client, mapper, accessValidator, dtoMapper);
    }
    
    @Bean
    public PersonService getPersonService() {
    	return new PersonService(new RestTemplate());
    }
    
    @Bean
    public CustomUserService getCustomUserService(@Autowired FhirClient client, @Autowired FhirMapper mapper) {
    	return new CustomUserService(new RestTemplate(), client, mapper);
    }

    @Bean
    public QuestionnaireResponseService getQuestionnaireResponseService(@Autowired FhirClient client, @Autowired FhirMapper mapper, @Autowired QuestionnaireResponsePriorityComparator priorityComparator, @Autowired AccessValidator accessValidator) {
        // Reverse the comporator: We want responses by descending priority.
        return new QuestionnaireResponseService(client, mapper, priorityComparator.reversed(), accessValidator);
    }

    @Bean
    public FhirClient getFhirClient(@Autowired UserContextProvider userContextProvider) {
        FhirContext context = FhirContext.forR4();
        return new FhirClient(context, fhirServerUrl, userContextProvider);
    }

    @Bean
    public WebMvcConfigurer getWebMvcConfigurer(@Autowired FhirClient client, @Value("${allowed_origins}") String allowedOrigins, @Autowired UserContextProvider userContextProvider, @Autowired IUserContextHandler userContextHandler) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins(allowedOrigins).allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new UserContextInterceptor(client, userContextProvider, userContextHandler));
            }
        };
    }

    @Bean
    public IUserContextHandler userContextHandler(@Value("${user.context.handler}") String userContextHandler) {
        switch(userContextHandler) {
            case "DIAS":
                return new DIASUserContextHandler();
            case "MOCK":
                return new MockContextHandler(mockContextOrganizationId);
            default:
                throw new IllegalArgumentException(String.format("Unknown userContextHandler value: %s", userContextHandler));
        }
    }
}
