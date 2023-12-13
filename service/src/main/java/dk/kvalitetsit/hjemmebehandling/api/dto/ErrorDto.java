package dk.kvalitetsit.hjemmebehandling.api.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public class ErrorDto {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private int errorCode;
    private String errorText;

    public ErrorDto(HttpStatus httpStatus, String path, int errorCode, String errorText) {
        this.timestamp = Instant.now();
        this.status = httpStatus.value();
        this.error = httpStatus.getReasonPhrase();
        this.path = path;
        this.errorCode = errorCode;
        this.errorText = errorText;
        this.message = this.errorText;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }
}
