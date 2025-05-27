package dk.kvalitetsit.hjemmebehandling.types;

import java.util.Optional;

public record Pagination(Optional<Integer> offset, Optional<Integer> limit) {
    public Pagination(Integer offset, Integer limit) {
        this(Optional.of(offset), Optional.of(limit));
    }
}
