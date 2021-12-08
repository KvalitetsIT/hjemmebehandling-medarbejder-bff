package dk.kvalitetsit.hjemmebehandling.constants.errors;

public enum ErrorDetails {
    CAREPLAN_EXISTS("Aktiv behandlingsplan eksisterer allerede for det angivne cpr-nummer.", 10),
    CAREPLAN_DOES_NOT_EXIST("Den angivne behandlingsplan eksisterer ikke.", 11),
    CAREPLAN_ALREADY_FULFILLED("Den angivne behandlingsplan har ikke alarmer.", 12),
    QUESTIONNAIRES_MISSING_FOR_CAREPLAN("De angivne spørgeskemaer eksisterer ikke.", 13),
    UNSUPPORTED_SEARCH_PARAMETER_COMBINATION("Den angivne kombination af parametre understøttes ikke.", 14),
    QUESTIONNAIRE_RESPONSE_DOES_NOT_EXIST("Den angivne spørgeskemabesvarelse eksisterer ikke.", 15),
    PATIENT_DOES_NOT_EXIST("Den angivne patient eksisterer ikke.", 16),
    ACCESS_VIOLATION("Du har ikke rettigheder til at tilgå de forespurgte data.", 16),
    PARAMETERS_INCOMPLETE("Parametre er mangelfuldt udfyldt.", 17),
    INTERNAL_SERVER_ERROR("Der opstod en intern fejl i systemet.", 99);

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
