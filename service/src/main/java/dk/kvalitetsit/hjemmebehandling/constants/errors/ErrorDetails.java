package dk.kvalitetsit.hjemmebehandling.constants.errors;

public enum ErrorDetails {
    CAREPLAN_EXISTS("Der eksisterer allerede en aktiv monitoreringsplan for det angivne cpr-nummer.", 10),
    CAREPLAN_DOES_NOT_EXIST("Den angivne monitoreringsplan eksisterer ikke.", 11),
    CAREPLAN_HAS_UNHANDLED_QUESTIONNAIRERESPONSES("Det er ikke muligt at afslutte patientens monitoreringsplan, da der ligger ubehandlede besvarelser, der skal håndteres, på et eller flere spørgeskemaer.", 11),
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
    QUESTIONNAIRE_ILLEGAL_STATUS_CHANGE("Det angivne statusskift for spørgeskema er ikke tilladt ",22),
    PLANDEFINITION_DOES_NOT_EXIST("Den angivne patientgruppe eksisterer ikke.", 22),
    QUESTIONS_ID_IS_NOT_UNIQUE("Alle spørgsmål har ikke en unik forkortelse", 23),
    CUSTOMLOGIN_UNKNOWN_ERROR("Der opstod et ukendt problem ved oprettelse af login",24),
    CPRSERVICE_UNKOWN_ERROR("Der opstod et ukendt problem i forsøget på at hente person udfra cpr-nummer",25),
    CAREPLAN_IS_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES("Det er ikke muligt at afslutte patientens monitoreringsplan, da der er blå alarmer, der skal håndteres, på et eller flere spørgeskemaer.", 26),
    QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN("Spørgeskemaet er tilknyttet en patientgruppe, hvor der er en eller flere aktive patienter, der først skal afsluttes.", 27),
    PLANDEFINITION_IS_IN_ACTIVE_USE_BY_CAREPLAN("Patientgruppen har tilknyttet patienter med en eller flere aktive monitoreringsplaner, der først skal afsluttes.", 28),
    PLANDEFINITION_CONTAINS_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES("Patientgruppen indeholder et eller flere spørgeskemaer, der har blå alarmer.", 29),
    PLANDEFINITION_CONTAINS_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES("Patienggruppen indeholder et eller flere spørgeskemaer der har ubehandlede besvarelser.", 30),
    REMOVED_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES("Et eller flere af de fjernede spørgeskemaer har blå alarmer.", 31),
    REMOVED_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES("Et eller flere af de fjernede spørgeskemaer har ubehandlede besvarelser.", 32),


    INTERNAL_SERVER_ERROR("Der opstod en intern fejl i systemet.", 99);


    private String errorMessage;
    private int errorCode;
    private String details;

    ErrorDetails(String errorMessage, int errorCode) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        if(details != null)
            return errorMessage + ") " + details;
        return errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
