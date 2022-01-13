package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.types.PageDetails;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PageDetailsTest {
    @Test
    public void page1_size1_offsetShouldBe0() {
        var pageDetails = new PageDetails(1,1);
        assertEquals(0, pageDetails.getOffset());
    }

    @Test
    public void page1_size10_offsetShouldBe0() {
        var pageDetails = new PageDetails(1,10);
        assertEquals(0, pageDetails.getOffset());
    }

    @Test
    public void page2_size1_offsetShouldBe1() {
        var pageDetails = new PageDetails(2,1);
        assertEquals(1, pageDetails.getOffset());
    }

    @Test
    public void page1_size5_offsetShouldBe5() {
        var pageDetails = new PageDetails(2,5);
        assertEquals(5, pageDetails.getOffset());
    }

    @Test
    public void page5_size5_offsetShouldBe20() {
        var pageDetails = new PageDetails(5,5);
        assertEquals(20, pageDetails.getOffset());
    }

}