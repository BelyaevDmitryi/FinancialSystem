package com.fs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionController {
    @ExceptionHandler({StockNotFoundException.class})
    public ResponseEntity<ErrorDto> handleNotFound(StockNotFoundException ex) {
        return new ResponseEntity<>(new ErrorDto(ex.getLocalizedMessage()), HttpStatus.NOT_FOUND);
    }
}
