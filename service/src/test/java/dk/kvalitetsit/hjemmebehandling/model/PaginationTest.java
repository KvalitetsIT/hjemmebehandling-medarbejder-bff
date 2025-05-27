package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.api.Paginator;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaginationTest {
    @Test
    public void givenAListOfNWhenPaginatedReturnPaginatedList() {
        var numbers = List.of(1, 2, 3 ,4 ,5 ,6 ,7, 8, 9);
        assertEquals(List.of(1, 2, 3 ,4 ,5), Paginator.paginate(numbers, new Pagination(0, 5)));
    }

}