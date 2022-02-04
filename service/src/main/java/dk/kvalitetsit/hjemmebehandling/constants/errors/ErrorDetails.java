package dk.kvalitetsit.hjemmebehandling.constants.errors;

public enum ErrorDetails {
    CAREPLAN_EXISTS("Aktiv monitoreringsplan eksisterer allerede for det angivne cpr-nummer.", 10),
    CAREPLAN_DOES_NOT_EXIST("Den angivne monitoreringsplan eksisterer ikke.", 11),
    CAREPLAN_HAS_UNHANDLED_QUESTIONNAIRERESPONSES("Monitoreringsplanen har ubehandlede besvarelser på et eller flere spørgeskemaer.", 11),
    CAREPLAN_ALREADY_FULFILLED("Den angivne monitoreringsplan har ikke alarmer.", 12),
    PLAN_DEFINITIONS_MISSING_FOR_CAREPLAN("De angivne patientgrupper eksisterer ikke.", 13),
    QUESTIONNAIRES_MISSING_FOR_CAREPLAN("De angivne spørgeskemaer eksisterer ikke.", 14),
    QUESTIONNAIRES_NOT_ALLOWED_FOR_CAREPLAN("De angivne spørgeskemaer kan ikke anvendes inden for de angivne patientgrupper.", 15),
    UNSUPPORTED_SEARCH_PARAMETER_COMBINATION("Den angivne kombination af parametre understøttes ikke.", 16),
    QUESTIONNAIRE_RESPONSE_DOES_NOT_EXIST("Den angivne spørgeskemabesvarelse eksisterer ikke.", 17),
    PATIENT_DOES_NOT_EXIST("Den angivne patient eksisterer ikke.", 18),
    ACCESS_VIOLATION("Du har ikke rettigheder til at tilgå de forespurgte data.", 19),
    PARAMETERS_INCOMPLETE("Parametre er mangelfuldt udfyldt.", 20),
    QUESTIONNAIRE_DOES_NOT_EXIST("Det angivne spørgeskema eksisterer ikke ",21),
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
