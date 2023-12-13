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
        return switch (e.getErrorKind()) {
            case BAD_GATEWAY, BAD_REQUEST -> fromErrorDetails(e.getErrorDetails());
            default -> new InternalServerErrorException(ErrorDetails.INTERNAL_SERVER_ERROR);
        };
    }

    private RuntimeException fromErrorDetails(ErrorDetails e) {
        return switch (e) {
            case CAREPLAN_EXISTS, CAREPLAN_ALREADY_FULFILLED, QUESTIONNAIRES_MISSING_FOR_CAREPLAN, UNSUPPORTED_SEARCH_PARAMETER_COMBINATION, PARAMETERS_INCOMPLETE, CAREPLAN_HAS_UNHANDLED_QUESTIONNAIRERESPONSES, CAREPLAN_IS_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES, QUESTIONNAIRE_IS_IN_ACTIVE_USE_BY_CAREPLAN, PLANDEFINITION_IS_IN_ACTIVE_USE_BY_CAREPLAN, PLANDEFINITION_CONTAINS_QUESTIONNAIRE_WITH_MISSING_SCHEDULED_QUESTIONNAIRERESPONSES, PLANDEFINITION_CONTAINS_QUESTIONNAIRE_WITH_UNHANDLED_QUESTIONNAIRERESPONSES ->
                    throw new BadRequestException(e);
            case CAREPLAN_DOES_NOT_EXIST, PATIENT_DOES_NOT_EXIST, QUESTIONNAIRE_RESPONSE_DOES_NOT_EXIST, QUESTIONNAIRE_DOES_NOT_EXIST ->
                    throw new ResourceNotFoundException(e);
            case ACCESS_VIOLATION -> throw new ForbiddenException(e);
            case CUSTOMLOGIN_UNKNOWN_ERROR -> throw new BadGatewayException(e);
            default -> new InternalServerErrorException(e);
        };
    }
}
