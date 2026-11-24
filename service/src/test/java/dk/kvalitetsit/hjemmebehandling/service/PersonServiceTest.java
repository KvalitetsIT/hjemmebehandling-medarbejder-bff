package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.model.PersonModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersonServiceTest {
    private static final String CPR_URL = "http://cpr/Patient/";

    @Test
    public void getPerson_ignoresUnknownFields() throws Exception {
        String response = "{\"resourceType\":\"Patient\",\"identifier\":{\"id\":\"0104909995\",\"system\":\"1.2.208.176.1.2\"},"
                + "\"active\":false,\"name\":{\"family\":\"Mosebryggersen\",\"given\":[\"Sverre Test\"]},\"gender\":\"male\","
                + "\"birthDate\":\"1990-04-01\",\"deceasedBoolean\":true,\"deceasedDatetime\":\"1900-01-01\","
                + "\"address\":{\"line\":\"Testgrusgraven 3, 3.tv\",\"city\":\"Hillerød\",\"postalCode\":\"3400\",\"country\":\"Danmark\"}}";
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.getForObject(CPR_URL + "0104909995", String.class)).thenReturn(response);
        PersonService subject = new PersonService(restTemplate);
        ReflectionTestUtils.setField(subject, "cprUrl", CPR_URL);

        PersonModel result = subject.getPerson("0104909995");

        assertEquals("0104909995", result.getIdentifier().getId());
        assertEquals(List.of("Sverre Test"), result.getName().getGiven());
        assertTrue(result.isDeceasedBoolean());
        assertEquals("Hillerød", result.getAddress().getCity());
    }
}
