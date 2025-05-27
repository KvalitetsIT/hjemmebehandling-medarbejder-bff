package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.types.Pagination;

import java.util.List;
import java.util.stream.Stream;

public class Paginator {
    public static <T> List<T> paginate(List<T> original, Pagination pagination) {
        return paginate(original.stream(), pagination).toList();
    }

    /**
     * Zero-based pagination: Paginates a stream according to offset and limit.</br>
     * Ex.</br>
     *  <code>Paginator.paginate(s, new Pagination(5, 10));</code></br>
     *  Return: A subset from index 5 to 15
     * </br>
     * @param original the stream which is to be paginated
     * @param pagination offset and limit
     * @return A paginated stream
     */
    public static <T> Stream<T> paginate(Stream<T> original, Pagination pagination) {
        Stream<T> stream = original
                .skip(pagination.offset()
                        .map(offset -> offset  * pagination.limit().orElse(1))
                        .orElse(0));

        if (pagination.limit().isPresent()) return stream.limit(pagination.limit().get());
        return stream;
    }

}