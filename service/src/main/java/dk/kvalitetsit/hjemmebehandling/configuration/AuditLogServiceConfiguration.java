package dk.kvalitetsit.hjemmebehandling.configuration;

import dk.kvalitetsit.hjemmebehandling.service.audit.DiasAuditLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AuditLogServiceConfiguration {
    @Value("${audit.url}")
    private String auditUrl;

    @Bean
    public WebClient getDiasAuditClient() {
        return WebClient.create(auditUrl);
    }

    @Bean
    public DiasAuditLogger getDiasAuditLogger() {
        boolean enabled = (auditUrl != null && !auditUrl.isEmpty());
        return new DiasAuditLogger(enabled);
    }

    @Bean(name = "threadPoolExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(7);
        executor.setMaxPoolSize(42);
        executor.setQueueCapacity(11);
        executor.setThreadNamePrefix("threadPoolExecutor-");
        executor.initialize();
        return executor;
    }
}
