package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.ErrorDto;
import dk.kvalitetsit.hjemmebehandling.constants.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import dk.kvalitetsit.hjemmebehandling.controller.exception.InternalServerErrorException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ExceptionControllerAdvice {
    @ExceptionHandler(BadRequestException.class)
    public ErrorDto badRequestException(BadRequestException exception, HttpServletRequest request) {
        return handleException(HttpStatus.BAD_REQUEST, request, exception.getErrorDetails());
    }

    @ExceptionHandler(InternalServerErrorException.class)
    public ErrorDto internalServerErrorException(InternalServerErrorException exception, HttpServletRequest request) {
        return handleException(HttpStatus.INTERNAL_SERVER_ERROR, request, exception.getErrorDetails());
    }

    private ErrorDto handleException(HttpStatus httpStatus, HttpServletRequest request, ErrorDetails errorDetails) {
        String path = request.getRequestURI();
        int errorCode = errorDetails.getErrorCode();
        String errorText = errorDetails.getErrorMessage();
        return new ErrorDto(httpStatus, path, errorCode, errorText);
    }
}
