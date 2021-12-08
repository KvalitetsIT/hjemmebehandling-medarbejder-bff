package dk.kvalitetsit.hjemmebehandling.controller.exception;

import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    private ErrorDetails errorDetails;

    public BadRequestException(ErrorDetails errorDetails) {
        this.errorDetails = errorDetails;
    }

    public ErrorDetails getErrorDetails() {
        return errorDetails;
    }
}
