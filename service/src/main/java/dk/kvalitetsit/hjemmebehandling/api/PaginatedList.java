package dk.kvalitetsit.hjemmebehandling.api;

import dk.kvalitetsit.hjemmebehandling.types.Pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PaginatedList<T> {

    private final List<T> original;

    private final int total;
    private final int limit;
    private final int offset;
    private final Pagination pagination;

    public PaginatedList(List<T> original, Pagination pagination) {
        this.original = original;
        this.pagination = pagination;
        this.total = original.size();
        this.offset = pagination.getOffset();
        this.limit = pagination.getLimit();
    }

    private List<T> paginate(List<T> original, Pagination pagination){
        return original
                .stream()
                .skip((long) (pagination.getOffset() - 1) * pagination.getLimit())
                .limit(pagination.getLimit())
                .collect(Collectors.toList());
    }


    public int getTotal() {
        return this.total;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public ArrayList<T> getList() {
        return  new ArrayList<>(this.paginate(original, this.pagination));
    }
}