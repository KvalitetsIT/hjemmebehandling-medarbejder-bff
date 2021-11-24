package dk.kvalitetsit.hjemmebehandling.service.exception;

public class ServiceException extends Exception {
    private ErrorKind errorKind;

    public ServiceException(String message, ErrorKind errorKind) {
        super(message);
        this.errorKind = errorKind;
    }

    public ServiceException(String message, Throwable cause, ErrorKind errorKind) {
        super(message, cause);
        this.errorKind = errorKind;
    }

    public ErrorKind getErrorKind() {
        return errorKind;
    }
}
