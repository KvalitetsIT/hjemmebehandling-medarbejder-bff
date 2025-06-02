package dk.kvalitetsit.hjemmebehandling.configuration;

import dk.kvalitetsit.hjemmebehandling.client.CustomUserClient;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.repository.*;
import dk.kvalitetsit.hjemmebehandling.repository.adaptation.PatientRepositoryAdaptor;
import dk.kvalitetsit.hjemmebehandling.service.*;
import dk.kvalitetsit.hjemmebehandling.service.implementation.*;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;

@Configuration
public class ServiceConfiguration {

    @Bean
    public PatientService patientService(@Autowired PatientRepositoryAdaptor patientRepository) {
        return new ConcretePatientService(patientRepository);
    }

    @Bean
    public PersonService personService() {
        return new ConcretePersonService(new RestTemplate());
    }


    @Bean
    public CarePlanService carePlanService(
            DateProvider dateProvider,
            CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository,
            PatientRepository<PatientModel, CarePlanStatus> patientRepository,
            CustomUserClient customUserService,
            QuestionnaireRepository<QuestionnaireModel> questionnaireRepository,
            PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository,
            QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository,
            OrganizationRepository<OrganizationModel> organizationRepository
    ) {
        return new ConcreteCarePlanService(
                dateProvider,
                carePlanRepository,
                patientRepository,
                customUserService,
                questionnaireRepository,
                planDefinitionRepository,
                questionnaireResponseRepository,
                organizationRepository
        );
    }

    @Bean
    public PlanDefinitionService planDefinitionService(
            DateProvider dateProvider,
            CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository,
            QuestionnaireRepository<QuestionnaireModel> questionnaireRepository,
            PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository,
            QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository,
            OrganizationRepository<OrganizationModel> organizationRepository
    ) {
        return new ConcretePlanDefinitionService(
                planDefinitionRepository,
                dateProvider,
                questionnaireRepository,
                carePlanRepository,
                questionnaireResponseRepository,
                organizationRepository

        );
    }

    @Bean
    public QuestionnaireService questionnaireService(
            @Autowired QuestionnaireRepository<QuestionnaireModel> questionnaireRepository,
            @Autowired CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository,
            @Autowired PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository
    ) {
        return new ConcreteQuestionnaireService(questionnaireRepository, carePlanRepository, planDefinitionRepository);
    }

    @Bean
    public QuestionnaireResponseService questionnaireResponseService(
            @Autowired Comparator<QuestionnaireResponseModel> priorityComparator,
            @Autowired QuestionnaireRepository<QuestionnaireModel> questionnaireRepository,
            @Autowired QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository,
            @Autowired PractitionerRepository<PractitionerModel> practitionerRepository,
            @Autowired OrganizationRepository<OrganizationModel> organizationRepository
    ) {
        // Reverse the comporator: We want responses by descending priority.
        return new ConcreteQuestionnaireResponseService(priorityComparator, questionnaireRepository, questionnaireResponseRepository, practitionerRepository);
    }


}
