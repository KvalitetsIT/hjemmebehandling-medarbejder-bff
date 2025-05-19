package dk.kvalitetsit.hjemmebehandling.configuration;

import ca.uhn.fhir.context.FhirContext;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.context.*;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.OrganizationModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.OrganizationRepository;
import dk.kvalitetsit.hjemmebehandling.security.RoleValidationInterceptor;
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
import java.util.List;
import java.util.StringTokenizer;

@Configuration
public class CrossCuttingConfiguration {
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
    public FhirClient getFhirClient(@Autowired UserContextProvider userContextProvider) {
        FhirContext context = FhirContext.forR4();
        return new FhirClient(context, fhirServerUrl, userContextProvider);
    }

    @Bean
    public FhirMapper fhirMapper() {
        return new FhirMapper();
    }

    @Bean
    public WebMvcConfigurer getWebMvcConfigurer(FhirClient client, @Value("${allowed_origins}") String allowedOrigins, @Autowired UserContextProvider userContextProvider, @Autowired IUserContextHandler userContextHandler) {
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
    public IUserContextHandler userContextHandler(@Value("${user.context.handler}") String userContextHandler, OrganizationRepository<OrganizationModel > organizationRepository) {
        return switch (userContextHandler) {
            case "DIAS" -> new DIASUserContextHandler(organizationRepository);
            case "MOCK" ->
                    new MockContextHandler(new QualifiedId.OrganizationId(mockContextOrganizationId), parseAsList(mockContextEntitlements));
            default ->
                    throw new IllegalArgumentException(String.format("Unknown userContextHandler value: %s", userContextHandler));
        };
    }

    @Bean
    public CustomUserClient customUserService(DtoMapper dtoMapper) {
        return new CustomUserClient(new RestTemplate(), dtoMapper);
    }
}
