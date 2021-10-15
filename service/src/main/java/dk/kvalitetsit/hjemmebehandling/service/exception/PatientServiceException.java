package dk.kvalitetsit.hjemmebehandling.service.exception;

public class PatientServiceException extends Exception {
    public PatientServiceException(String message) {
        super(message);
    }

    public PatientServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
