package dk.kvalitetsit.hjemmebehandling.service.exception;

import dk.kvalitetsit.hjemmebehandling.model.constants.errors.ErrorDetails;

public class ServiceException extends Exception {
    private final ErrorKind errorKind;
    private final ErrorDetails errorDetails;

    public ServiceException(String message, ErrorKind errorKind, ErrorDetails errorDetails) {
        super(message);
        this.errorKind = errorKind;
        this.errorDetails = errorDetails;
    }

    public ServiceException(String message, Throwable cause, ErrorKind errorKind, ErrorDetails errorDetails) {
        super(message, cause);
        this.errorKind = errorKind;
        this.errorDetails = errorDetails;
    }

    public ErrorKind getErrorKind() {
        return errorKind;
    }

    public ErrorDetails getErrorDetails() {
        return errorDetails;
    }
}
