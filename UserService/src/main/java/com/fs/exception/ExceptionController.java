package com.fs.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import com.fs.dto.ErrorDto;

import java.util.stream.Collectors;


@ControllerAdvice
public class ExceptionController extends ResponseEntityExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ExceptionController.class);
    
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.warn("Validation error: {}", errorMessage);
        return new ResponseEntity<>(new ErrorDto(errorMessage), HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler({ StockAlreadyExistException.class, UserAlreadyExistException.class})
    public ResponseEntity<ErrorDto> handle(Exception ex) {
        logger.warn("Business logic error: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorDto(ex.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({UserNotFoundException.class, StockNotFoundException.class, BrokerAccountNotFoundException.class})
    public ResponseEntity<ErrorDto> handleNotFound(Exception ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorDto(ex.getLocalizedMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({PriceServiceException.class, CouldntGetPricesException.class})
    public ResponseEntity<ErrorDto> handleExceptionFromPriceService(Exception ex) {
        logger.error("Price service error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(new ErrorDto(ex.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorDto> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        logger.warn("Invalid refresh token: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorDto(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorDto> handleIllegalArgument(Exception ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        return new ResponseEntity<>(new ErrorDto(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDto> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.error("Data integrity violation: {}", ex.getMessage(), ex);
        String message = "Data integrity violation. This may occur if the username already exists.";
        String errorMessage = ex.getMessage();
        if (errorMessage != null && 
            (errorMessage.contains("unique constraint") || errorMessage.contains("duplicate key"))) {
            message = "Username already exists";
        }
        return new ResponseEntity<>(new ErrorDto(message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorDto> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime error: {}", ex.getMessage(), ex);
        // Если это обёрнутое исключение, показываем причину
        Throwable cause = ex.getCause();
        String message = ex.getMessage();
        if (cause != null && cause.getMessage() != null) {
            message = cause.getMessage();
        }
        return new ResponseEntity<>(new ErrorDto(message != null ? message : "Internal server error"), 
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        String message = ex.getMessage() != null ? ex.getMessage() : "Internal server error";
        return new ResponseEntity<>(new ErrorDto(message), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
