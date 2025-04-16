package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.types.Pagination;

import java.util.ArrayList;
import java.util.List;

public class PaginatedList<T>  {

    private final List<T> original;
    private final Pagination pagination;

    public PaginatedList(List<T> original, Pagination pagination) {
        this.original = original;
        this.pagination = pagination;
    }

    private List<T> paginate(List<T> original, Pagination pagination) {
        return original
                .stream()
                .skip((long) (pagination.offset() - 1) * pagination.limit())
                .limit(pagination.limit())
                .toList();
    }

    public ArrayList<T> getList() {
        return new ArrayList<>(this.paginate(original, this.pagination));
    }
}