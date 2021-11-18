package dk.kvalitetsit.hjemmebehandling.service.exception;

public class AccessValidationException extends Exception {
    public AccessValidationException(String message) {
        super(message);
    }

    public AccessValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
