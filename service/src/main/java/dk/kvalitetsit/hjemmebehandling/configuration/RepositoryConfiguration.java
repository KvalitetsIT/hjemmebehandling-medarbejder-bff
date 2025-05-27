package dk.kvalitetsit.hjemmebehandling.configuration;

import dk.kvalitetsit.hjemmebehandling.context.UserContextProvider;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.model.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.repository.*;
import dk.kvalitetsit.hjemmebehandling.repository.adaptation.*;
import dk.kvalitetsit.hjemmebehandling.repository.implementation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfiguration {

    @Bean
    public CarePlanRepository<CarePlanModel, PatientModel> carePlanRepository(FhirClient fhirClient, FhirMapper fhirMapper) {
        var repository = new ConcreteCarePlanRepository(fhirClient);
        return new CarePlanRepositoryAdaptor(repository, fhirMapper);
    }

    @Bean
    public PlanDefinitionRepository<PlanDefinitionModel> planDefinitionRepository(FhirClient fhirClient, FhirMapper fhirMapper) {
        var repository = new ConcretePlanDefinitionRepository(fhirClient);
        return new PlanDefinitionRepositoryAdaptor(repository, fhirMapper);
    }

    @Bean
    public PatientRepository<PatientModel, CarePlanStatus> patientRepository(FhirClient fhirClient, FhirMapper fhirMapper) {
        var repository = new ConcretePatientRepository(fhirClient);
        return new PatientRepositoryAdaptor(repository, fhirMapper);
    }

    @Bean
    public QuestionnaireRepository<QuestionnaireModel> questionnaireRepository(FhirClient fhirClient, FhirMapper fhirMapper, UserContextProvider userContextProvide) {
        var repository = new ConcreteQuestionnaireRepository(fhirClient, userContextProvide);
        return new QuestionnaireRepositoryAdaptor(repository, fhirMapper);
    }

    @Bean
    public QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository(FhirClient fhirClient, FhirMapper fhirMapper) {
        var repository = new ConcreteQuestionnaireResponseRepository(fhirClient);
        return new QuestionnaireResponseRepositoryAdaptor(repository, fhirMapper);
    }

    @Bean
    public PractitionerRepository<PractitionerModel> practitionerRepository(FhirClient fhirClient, FhirMapper fhirMapper, UserContextProvider userContextProvider) {
        var repository = new ConcretePractitionerRepository(fhirClient, userContextProvider);
        return new PractitionerRepositoryAdaptor(repository, fhirMapper);
    }

    @Bean
    public OrganizationRepository<OrganizationModel> organizationRepository(FhirClient fhirClient, FhirMapper fhirMapper, UserContextProvider userContextProvider) {
        var repository = new ConcreteOrganizationRepository(fhirClient, userContextProvider);
        return new OrganizationRepositoryAdaptor(repository, fhirMapper);
    }


    @Bean
    public ValueSetRepository<ValueSetModel> valueSetRepository(FhirClient fhirClient, FhirMapper fhirMapper) {
        var repository = new ConcreteValueSetRepository(fhirClient);
        return new ValueSetRepositoryAdaptor(repository, fhirMapper);
    }


}
