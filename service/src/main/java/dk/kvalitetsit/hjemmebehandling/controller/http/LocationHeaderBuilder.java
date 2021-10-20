package dk.kvalitetsit.hjemmebehandling.controller.http;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Component
public class LocationHeaderBuilder {
    public URI buildLocationHeader(String id) {
        return URI.create(ServletUriComponentsBuilder.fromCurrentRequestUri().path("/" + id).build().toString());
    }
}
