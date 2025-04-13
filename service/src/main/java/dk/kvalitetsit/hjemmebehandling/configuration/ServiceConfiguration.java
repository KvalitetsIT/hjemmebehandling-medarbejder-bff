package dk.kvalitetsit.hjemmebehandling.configuration;

import ca.uhn.fhir.context.FhirContext;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.context.*;
import dk.kvalitetsit.hjemmebehandling.fhir.ConcreteFhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClientAdaptor;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.security.RoleValidationInterceptor;
import dk.kvalitetsit.hjemmebehandling.service.*;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.jetbrains.annotations.NotNull;
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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

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

    private static List<String> parseAsList(String str) {
        return Collections.list(new StringTokenizer(str, ",")).stream()
                .map(token -> ((String) token).trim())
                .toList();
    }

    @Bean
    public AuditEventRepository auditEventRepository() {
        return new InMemoryAuditEventRepository();
    }


    @Bean
    public ConcreteFhirClient getFhirClient(@Autowired UserContextProvider userContextProvider) {
        FhirContext context = FhirContext.forR4();
        return new ConcreteFhirClient(context, fhirServerUrl, userContextProvider);
    }


    @Bean
    public FhirClientAdaptor getFhirClient(ConcreteFhirClient client, FhirMapper mapper) {
        return new FhirClientAdaptor(client, mapper);
    }


    @Bean
    public CarePlanService getCarePlanService(
            @Autowired FhirClientAdaptor client,
            @Autowired FhirMapper mapper,
            @Autowired DateProvider dateProvider,
            @Autowired AccessValidator accessValidator,
            @Autowired DtoMapper dtoMapper,
            @Autowired CustomUserClient customUserService
    ) {
        return new CarePlanService(client, mapper, dateProvider, accessValidator, dtoMapper, customUserService);
    }

    @Bean
    public PatientService getPatientService(@Autowired FhirClientAdaptor client, @Autowired FhirMapper mapper, @Autowired AccessValidator accessValidator) {
        return new PatientService(client, mapper, accessValidator);
    }

    @Bean
    public PersonService getPersonService() {
        return new PersonService(new RestTemplate());
    }

    @Bean
    public QuestionnaireService getQuestionnaireService(@Autowired FhirClientAdaptor client, @Autowired FhirMapper mapper, @Autowired AccessValidator accessValidator) {
        return new QuestionnaireService(client, mapper, accessValidator);
    }

    @Bean
    public CustomUserClient getCustomUserService(DtoMapper dtoMapper) {
        return new CustomUserClient(new RestTemplate(), dtoMapper);
    }

    @Bean
    public QuestionnaireResponseService getQuestionnaireResponseService(@Autowired FhirClientAdaptor client, @Autowired FhirMapper mapper, @Autowired Comparator<QuestionnaireResponseModel> priorityComparator, @Autowired AccessValidator accessValidator) {
        // Reverse the comporator: We want responses by descending priority.
        return new QuestionnaireResponseService(client, mapper, priorityComparator, accessValidator);
    }

    @Bean
    public WebMvcConfigurer getWebMvcConfigurer(ConcreteFhirClient client, @Value("${allowed_origins}") String allowedOrigins, @Autowired UserContextProvider userContextProvider, @Autowired IUserContextHandler userContextHandler) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins(allowedOrigins).allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
            }

            @Override
            public void addInterceptors(@NotNull InterceptorRegistry registry) {
                registry.addInterceptor(new UserContextInterceptor(client, userContextProvider, userContextHandler));
                registry.addInterceptor(new RoleValidationInterceptor(userContextProvider, parseAsList(allowedRoles)));
                if (adminRoles != null)
                    registry.addInterceptor(new RoleValidationInterceptor(userContextProvider, parseAsList(adminRoles)))
                            .addPathPatterns("/v1/plandefinition", "/v1/plandefinition/**", "/v1/questionnaire", "/v1/questionnaire/**")
                            .excludePathPatterns("/v1/questionnaireresponse", "/v1/questionnaireresponse?", "/v1/questionnaireresponse/**");
            }
        };
    }

    @Bean
    public IUserContextHandler userContextHandler(@Value("${user.context.handler}") String userContextHandler) {
        return switch (userContextHandler) {
            case "DIAS" -> new DIASUserContextHandler();
            case "MOCK" -> new MockContextHandler(mockContextOrganizationId, parseAsList(mockContextEntitlements));
            default ->
                    throw new IllegalArgumentException(String.format("Unknown userContextHandler value: %s", userContextHandler));
        };
    }

    @Bean
    public ValueSetService getValueSetService(@Autowired FhirClientAdaptor client, @Autowired FhirMapper mapper) {
        return new ValueSetService(client, mapper);
    }
}
