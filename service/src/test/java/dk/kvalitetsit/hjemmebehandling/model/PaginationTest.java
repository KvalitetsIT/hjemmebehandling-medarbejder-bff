package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaginationTest {
    @Test
    public void page1_size1_offsetShouldBe0() {
        var pageDetails = new Pagination(1, 1);
        assertEquals(1, pageDetails.offset());
    }

    @Test
    public void page1_size10_offsetShouldBe0() {
        var pageDetails = new Pagination(1, 10);
        assertEquals(1, pageDetails.offset());
    }

    @Test
    public void page2_size1_offsetShouldBe1() {
        var pageDetails = new Pagination(2, 1);
        assertEquals(2, pageDetails.offset());
    }

    @Test
    public void page1_size5_offsetShouldBe5() {
        var pageDetails = new Pagination(2, 5);
        assertEquals(2, pageDetails.offset());
    }

    @Test
    public void page5_size5_offsetShouldBe20() {
        var pageDetails = new Pagination(5, 5);
        assertEquals(5, pageDetails.offset());
    }

}