package dk.kvalitetsit.hjemmebehandling.controller.http;

import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Component
public class LocationHeaderBuilder {
    public URI buildLocationHeader(QualifiedId id) {
        return URI.create(ServletUriComponentsBuilder.fromCurrentRequestUri().path("/" + id.unqualified()).build().toString());
    }
}
