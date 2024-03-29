package dk.kvalitetsit.hjemmebehandling.configuration;

import dk.kvalitetsit.hjemmebehandling.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ca.uhn.fhir.context.FhirContext;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.context.DIASUserContextHandler;
import dk.kvalitetsit.hjemmebehandling.context.IUserContextHandler;
import dk.kvalitetsit.hjemmebehandling.context.MockContextHandler;
import dk.kvalitetsit.hjemmebehandling.context.UserContextInterceptor;
import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.comparator.QuestionnaireResponsePriorityComparator;
import dk.kvalitetsit.hjemmebehandling.security.RoleValidationInterceptor;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;

import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

@Configuration
public class ServiceConfiguration {
	
	@Value("${user.mock.context.organization.id}")
	private String mockContextOrganizationId;


    @Value("${user.mock.context.entitlements}")
    private String mockContextEntitlements;


    @Value("${fhir.server.url}")
	private String fhirServerUrl;
	
	@Value("${allowed.roles}")
	private String allowedRoles;

    @Value("${allowed.admin.roles:#{null}}")
    private String adminRoles;
  @Bean
  public AuditEventRepository auditEventRepository() {
    return new InMemoryAuditEventRepository();
  }

    @Bean
    public CarePlanService getCarePlanService(
            @Autowired FhirClient client,
            @Autowired FhirMapper mapper,
            @Autowired DateProvider dateProvider,
            @Autowired AccessValidator accessValidator,
            @Autowired DtoMapper dtoMapper,
            @Autowired CustomUserClient customUserService
    ) {
        return new CarePlanService(client, mapper, dateProvider, accessValidator, dtoMapper, customUserService);
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
    public QuestionnaireService getQuestionnaireService(@Autowired FhirClient client, @Autowired FhirMapper mapper, @Autowired AccessValidator accessValidator) {
        return new QuestionnaireService(client,mapper,accessValidator);
    }
    
    @Bean
    public CustomUserClient getCustomUserService() {
    	return new CustomUserClient(new RestTemplate());
    }

    @Bean
    public QuestionnaireResponseService getQuestionnaireResponseService(@Autowired FhirClient client, @Autowired FhirMapper mapper, @Autowired QuestionnaireResponsePriorityComparator priorityComparator, @Autowired AccessValidator accessValidator) {
        // Reverse the comporator: We want responses by descending priority.
        return new QuestionnaireResponseService(client, mapper, priorityComparator, accessValidator);
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
                registry.addInterceptor(new RoleValidationInterceptor(userContextProvider, parseAsList(allowedRoles)));
                if(adminRoles != null) registry.addInterceptor(new RoleValidationInterceptor(userContextProvider, parseAsList(adminRoles)))
                        .addPathPatterns("/v1/plandefinition","/v1/plandefinition/**", "/v1/questionnaire", "/v1/questionnaire/**")
                        .excludePathPatterns("/v1/questionnaireresponse", "/v1/questionnaireresponse?", "/v1/questionnaireresponse/**");
            }
        };
    }

    private static List<String> parseAsList(String str) {
        return Collections.list(new StringTokenizer(str, ",")).stream()
                .map(token -> ((String) token).trim())
                .collect(Collectors.toList());
    }

    @Bean
    public IUserContextHandler userContextHandler(@Value("${user.context.handler}") String userContextHandler) {
        switch(userContextHandler) {
            case "DIAS":
                return new DIASUserContextHandler();
            case "MOCK":
                return new MockContextHandler(mockContextOrganizationId, parseAsList(mockContextEntitlements));
            default:
                throw new IllegalArgumentException(String.format("Unknown userContextHandler value: %s", userContextHandler));
        }
    }

    @Bean
    public ValueSetService getValueSetService(@Autowired FhirClient client, @Autowired FhirMapper mapper) {
      return new ValueSetService(client, mapper);
    }
}
