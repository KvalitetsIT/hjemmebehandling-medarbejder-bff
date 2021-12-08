package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ForbiddenException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.ResourceNotFoundException;
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

    protected RuntimeException toStatusCodeException(AccessValidationException e) {
        return new ForbiddenException(ErrorDetails.ACCESS_VIOLATION);
    }

    protected RuntimeException toStatusCodeException(ServiceException e) {
        switch(e.getErrorKind()) {
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
                throw new BadRequestException(e);
            case CAREPLAN_DOES_NOT_EXIST:
            case PATIENT_DOES_NOT_EXIST:
            case QUESTIONNAIRE_RESPONSE_DOES_NOT_EXIST:
                throw new ResourceNotFoundException(e);
            case ACCESS_VIOLATION:
                throw new ForbiddenException(e);
            case INTERNAL_SERVER_ERROR:
            default:
                return new InternalServerErrorException(e);
        }
    }
}
