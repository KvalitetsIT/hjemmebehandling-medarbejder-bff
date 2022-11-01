package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.*;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

public abstract class BaseController {
    protected RuntimeException toStatusCodeException(Exception e) {
        if(e.getClass() == AccessValidationException.class) {
            return toStatusCodeException((AccessValidationException) e);
        }
        if(e.getClass() == ServiceException.class) {
            return toStatusCodeException((ServiceException) e);
        }
        throw new InternalServerErrorException(ErrorDetails.INTERNAL_SERVER_ERROR);
    }

    private RuntimeException toStatusCodeException(AccessValidationException e) {
        return new ForbiddenException(ErrorDetails.ACCESS_VIOLATION);
    }

    private RuntimeException toStatusCodeException(ServiceException e) {
        switch(e.getErrorKind()) {
            case BAD_GATEWAY:
            case BAD_REQUEST:
                return fromErrorDetails(e.getErrorDetails());
            case INTERNAL_SERVER_ERROR:
            default:
                return new InternalServerErrorException(ErrorDetails.INTERNAL_SERVER_ERROR);
        }
    }

    private RuntimeException fromErrorDetails(ErrorDetails e) {
        switch(e) {
            case CAREPLAN_EXISTS:
            case CAREPLAN_ALREADY_FULFILLED:
            case QUESTIONNAIRES_MISSING_FOR_CAREPLAN:
            case UNSUPPORTED_SEARCH_PARAMETER_COMBINATION:
            case PARAMETERS_INCOMPLETE:
            case CAREPLAN_HAS_UNHANDLED_QUESTIONNAIRERESPONSES:
            case CAREPLAN_IS_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES:
                throw new BadRequestException(e);

            case CAREPLAN_DOES_NOT_EXIST:
            case PATIENT_DOES_NOT_EXIST:
            case QUESTIONNAIRE_RESPONSE_DOES_NOT_EXIST:
            case QUESTIONNAIRE_DOES_NOT_EXIST:
                throw new ResourceNotFoundException(e);
            case ACCESS_VIOLATION:
                throw new ForbiddenException(e);
            case CUSTOMLOGIN_UNKNOWN_ERROR:
                throw new BadGatewayException(e);
            case INTERNAL_SERVER_ERROR:
            default:
                return new InternalServerErrorException(e);
        }
    }
}
