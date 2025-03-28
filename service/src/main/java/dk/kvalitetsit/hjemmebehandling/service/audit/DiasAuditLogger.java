package dk.kvalitetsit.hjemmebehandling.service.audit;

import dk.kvalitetsit.hjemmebehandling.model.audit.AuditModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class DiasAuditLogger {
    private static final Logger logger = LoggerFactory.getLogger(DiasAuditLogger.class);

    private final boolean enabled;

    @Autowired
    public WebClient client;

    public DiasAuditLogger(boolean enabled) {
        this.enabled = enabled;
    }

    @Async
    public void postAuditLog(AuditModel event) {
        if (enabled) {
            logger.debug("Calling DIAS_AUDIT");

            client.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(event), AuditModel.class)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnError(error -> logger.error("Error calling DIAS audit log service {}. Audit content was {}", error.getMessage(), event))
                    .block();
        }
    }
}
