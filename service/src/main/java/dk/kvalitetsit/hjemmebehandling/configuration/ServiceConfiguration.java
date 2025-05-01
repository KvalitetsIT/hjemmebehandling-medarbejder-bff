package dk.kvalitetsit.hjemmebehandling.configuration;

import ca.uhn.fhir.context.FhirContext;
import dk.kvalitetsit.hjemmebehandling.api.DtoMapper;
import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.context.*;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.repository.*;
import dk.kvalitetsit.hjemmebehandling.repository.adaptation.CarePlanRepositoryAdaptor;
import dk.kvalitetsit.hjemmebehandling.repository.adaptation.PatientRepositoryAdaptor;
import dk.kvalitetsit.hjemmebehandling.repository.implementation.ConcreteCarePlanRepository;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.security.RoleValidationInterceptor;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.implementation.*;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.hl7.fhir.r4.model.Organization;
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
    public FhirClient getFhirClient(@Autowired UserContextProvider userContextProvider) {
        FhirContext context = FhirContext.forR4();
        return new FhirClient(context, fhirServerUrl, userContextProvider);
    }

    @Bean
    public CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository(FhirClient fhirClient) {
        var repository = new ConcreteCarePlanRepository(fhirClient);
        var mapper = new FhirMapper();
        return new CarePlanRepositoryAdaptor(repository, mapper);
    }

    @Bean
    public ConcreteCarePlanService getCarePlanService(
            @Autowired CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository,
            @Autowired PatientRepository<PatientModel, CarePlanStatus> patientRepository,
            @Autowired QuestionnaireRepository<QuestionnaireModel> questionnaireRepository,
            @Autowired PlanDefinitionRepository<PlanDefinitionModel> plaDefinitinRepository,
            @Autowired QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository,
            @Autowired OrganizationRepository<Organization> organizationRepository,
            @Autowired DateProvider dateProvider,
            @Autowired AccessValidator accessValidator,
            @Autowired DtoMapper dtoMapper,
            @Autowired CustomUserClient customUserService
    ) {
        return new ConcreteCarePlanService(
                dateProvider,
                accessValidator,
                carePlanRepository,
                patientRepository,
                customUserService,
                questionnaireRepository,
                plaDefinitinRepository,
                questionnaireResponseRepository,
                organizationRepository
        );
    }

    @Bean
    public ConcretePatientService getPatientService(@Autowired PatientRepositoryAdaptor patientRepository, @Autowired AccessValidator accessValidator) {
        return new ConcretePatientService(accessValidator, patientRepository);
    }

    @Bean
    public ConcretePersonService getPersonService() {
        return new ConcretePersonService(new RestTemplate());
    }

    @Bean
    public ConcreteQuestionnaireService getQuestionnaireService(@Autowired AccessValidator accessValidator,
                                                                @Autowired QuestionnaireRepository<QuestionnaireModel> questionnaireRepository,
                                                                @Autowired CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository,
                                                                @Autowired PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository
    ) {
        return new ConcreteQuestionnaireService(accessValidator, questionnaireRepository, carePlanRepository, planDefinitionRepository);
    }

    @Bean
    public CustomUserClient getCustomUserService(DtoMapper dtoMapper) {
        return new CustomUserClient(new RestTemplate(), dtoMapper);
    }

    @Bean
    public ConcreteQuestionnaireResponseService getQuestionnaireResponseService(@Autowired Comparator<QuestionnaireResponseModel> priorityComparator,
                                                                                @Autowired AccessValidator accessValidator,
                                                                                @Autowired QuestionnaireRepository<QuestionnaireModel> questionnaireRepository,
                                                                                @Autowired QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository,
                                                                                @Autowired PractitionerRepository<PractitionerModel> practitionerRepository,
                                                                                @Autowired OrganizationRepository<Organization> organizationRepository

    ) {
        // Reverse the comporator: We want responses by descending priority.
        return new ConcreteQuestionnaireResponseService(priorityComparator, accessValidator, questionnaireRepository, questionnaireResponseRepository, practitionerRepository, organizationRepository);
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
    public IUserContextHandler userContextHandler(@Value("${user.context.handler}") String userContextHandler) {
        return switch (userContextHandler) {
            case "DIAS" -> new DIASUserContextHandler();
            case "MOCK" -> new MockContextHandler(mockContextOrganizationId, parseAsList(mockContextEntitlements));
            default ->
                    throw new IllegalArgumentException(String.format("Unknown userContextHandler value: %s", userContextHandler));
        };
    }

}
