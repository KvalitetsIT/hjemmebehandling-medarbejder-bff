package dk.kvalitetsit.hjemmebehandling.mapping;

/**
 * This interface is supposed to be implemented by every DTO
 * @param <T> the internal representation of this DTO
 */
public interface Dto<T extends Model<?>> {
    /**
     * Maps this DTO into the specified internal model
     * @return T
     */
    T toModel();
}
