package dk.kvalitetsit.hjemmebehandling.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class DateProvider {
    public Date now() {
        return Date.from(Instant.now());
    }
}
