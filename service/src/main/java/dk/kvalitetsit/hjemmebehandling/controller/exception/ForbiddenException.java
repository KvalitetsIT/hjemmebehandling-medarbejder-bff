package dk.kvalitetsit.hjemmebehandling.controller.exception;

import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;

public class ForbiddenException extends RuntimeException {
    private ErrorDetails errorDetails;

    public ForbiddenException(ErrorDetails errorDetails) {
        this.errorDetails = errorDetails;
    }

    public ErrorDetails getErrorDetails() {
        return errorDetails;
    }
}
