package com.fs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@ControllerAdvice
public class ExceptionController extends ResponseEntityExceptionHandler {
    @ExceptionHandler({ StockAlreadyExistException.class})
    public ResponseEntity<ErrorDto> handle(Exception ex) {
        return new ResponseEntity<>(new ErrorDto(ex.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({StockNotFoundException.class})
    public ResponseEntity<ErrorDto> handleNotFound(Exception ex) {
        return new ResponseEntity<>(new ErrorDto(ex.getLocalizedMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({TinkoffServiceException.class})
    public ResponseEntity<ErrorDto> handleExceptionFromPriceService(Exception ex) {
        return new ResponseEntity<>(new ErrorDto(ex.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
    }
}
