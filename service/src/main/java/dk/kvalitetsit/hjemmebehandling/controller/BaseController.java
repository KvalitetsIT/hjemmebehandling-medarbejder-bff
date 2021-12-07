package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.ErrorDto;
import dk.kvalitetsit.hjemmebehandling.constants.ErrorDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.ServletContext;

public abstract class BaseController {
    private ServletContext servletContext;

    public BaseController(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    protected ResponseEntity<?> badRequest(ErrorDetails errorDetails) {
        String path = servletContext.getContextPath();
        return ResponseEntity.badRequest().body(new ErrorDto(HttpStatus.BAD_REQUEST, path, errorDetails.getErrorCode(), errorDetails.getErrorMessage()));
    }
}
