package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.ErrorDto;
import dk.kvalitetsit.hjemmebehandling.constants.errors.ErrorDetails;
import dk.kvalitetsit.hjemmebehandling.controller.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ExceptionHandlingControllerAdvice {
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto badRequestException(BadRequestException exception, HttpServletRequest request) {
        return handleException(HttpStatus.BAD_REQUEST, request, exception.getErrorDetails());
    }

    @ExceptionHandler(BadGatewayException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorDto badGatewayException(BadGatewayException exception, HttpServletRequest request) {
        return handleException(HttpStatus.BAD_GATEWAY, request, exception.getErrorDetails());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorDto forbiddenException(ForbiddenException exception, HttpServletRequest request) {
        return handleException(HttpStatus.FORBIDDEN, request, exception.getErrorDetails());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDto resourceNotFoundException(ResourceNotFoundException exception, HttpServletRequest request) {
        return handleException(HttpStatus.NOT_FOUND, request, exception.getErrorDetails());
    }

    @ExceptionHandler(InternalServerErrorException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
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
