package dk.kvalitetsit.hjemmebehandling.controller.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocationHeaderBuilderTest {
    private LocationHeaderBuilder subject;

    @BeforeEach
    public void setup() {
        subject = new LocationHeaderBuilder();
    }

    @Test
    public void buildLocationHeader_appendsToRequestUri() {
        String id = "123";

        int port = 8787;
        String requestUri = "/api/v1/careplan";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerPort(port);
        request.setRequestURI(requestUri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        URI result = subject.buildLocationHeader(id);

        URI expected = URI.create("http://localhost:" + port + requestUri + "/" + id);
        assertEquals(expected, result);
    }
}