package dk.kvalitetsit.hjemmebehandling.controller.exception;

import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    private final ErrorDetails errorDetails;

    public ResourceNotFoundException(ErrorDetails errorDetails) {
        this.errorDetails = errorDetails;
    }

    public ResourceNotFoundException(String message, ErrorDetails errorDetails) {
        super(message);
        this.errorDetails = errorDetails;
    }

    public ErrorDetails getErrorDetails() {
        return errorDetails;
    }
}
