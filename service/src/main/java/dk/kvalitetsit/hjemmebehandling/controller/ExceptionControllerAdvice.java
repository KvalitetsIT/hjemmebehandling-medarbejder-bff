package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.ErrorDto;
import dk.kvalitetsit.hjemmebehandling.controller.exception.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ExceptionControllerAdvice {
    @ExceptionHandler(BadRequestException.class)
    public ErrorDto badRequestException(BadRequestException exception, HttpServletRequest request) {
        String path = request.getRequestURI();
        int errorCode = exception.getErrorDetails().getErrorCode();
        String errorText = exception.getErrorDetails().getErrorMessage();
        return new ErrorDto(HttpStatus.BAD_REQUEST, path, errorCode, errorText);
    }
}
