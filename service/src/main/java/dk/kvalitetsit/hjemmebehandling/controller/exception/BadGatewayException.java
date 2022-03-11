package dk.kvalitetsit.hjemmebehandling.controller.exception;

import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_GATEWAY)
public class BadGatewayException extends RuntimeException {
    private ErrorDetails errorDetails;

    public BadGatewayException(ErrorDetails errorDetails) {
        this.errorDetails = errorDetails;
    }

    public ErrorDetails getErrorDetails() {
        return errorDetails;
    }
}
