package dk.kvalitetsit.hjemmebehandling.constants;

public enum ErrorDetails {
    CAREPLAN_EXISTS("Aktiv behandlingsplan eksisterer allerede for det angivne cpr-nummer", 10);

    private String errorMessage;
    private int errorCode;

    ErrorDetails(String errorMessage, int errorCode) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
