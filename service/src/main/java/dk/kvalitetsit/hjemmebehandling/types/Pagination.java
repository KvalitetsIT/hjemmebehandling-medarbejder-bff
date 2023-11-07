package dk.kvalitetsit.hjemmebehandling.types;

import java.util.Objects;

public class Pagination {
    private int offset;
    private int limit;

    public Pagination(int offset, int limit) {
        this.offset = offset;
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pagination that = (Pagination) o;
        return offset == that.offset && limit == that.limit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, limit);
    }
}
