package dk.kvalitetsit.hjemmebehandling.mapping;

/**
 * This interface is supposed to be implemented by every Model
 * @param <T> the DTO representation of this model
 */
public interface Model<T extends Dto<?>> {
    /**
     * Maps this model into the specified DTO T
     * @return T
     */
    T toDto();

}
