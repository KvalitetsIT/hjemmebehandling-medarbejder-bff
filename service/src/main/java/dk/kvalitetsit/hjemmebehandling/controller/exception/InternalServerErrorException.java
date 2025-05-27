package dk.kvalitetsit.hjemmebehandling.controller.exception;

import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerErrorException extends RuntimeException {
    private final ErrorDetails errorDetails;

    public InternalServerErrorException(ErrorDetails errorDetails) {
        this.errorDetails = errorDetails;
    }

    public ErrorDetails getErrorDetails() {
        return errorDetails;
    }
}
