package dk.kvalitetsit.hello.configuration;

import dk.kvalitetsit.hello.service.PatientService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PatientConfiguration {
    @Bean
    public PatientService getPatientService() {
        return new PatientService();
    }
}
