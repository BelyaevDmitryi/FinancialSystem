package com.fs.exception;

import com.fs.dto.ErrorDto;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ExceptionController {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorDto> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        log.warn("Отсутствует обязательный заголовок: {}", e.getHeaderName());
        String message = "Отсутствует обязательный заголовок: " + e.getHeaderName() + ". Убедитесь, что вы авторизованы.";
        ErrorDto error = new ErrorDto(HttpStatus.BAD_REQUEST.value(), message);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Ошибка валидации данных: {}", errorMessage);
        ErrorDto error = new ErrorDto(HttpStatus.BAD_REQUEST.value(), errorMessage);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Ошибка валидации: {}", e.getMessage());
        ErrorDto error = new ErrorDto(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorDto> handleIllegalStateException(IllegalStateException e) {
        log.error("Ошибка состояния: {}", e.getMessage());
        ErrorDto error = new ErrorDto(HttpStatus.CONFLICT.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorDto> handleFeignException(FeignException e) {
        log.error("Ошибка при вызове внешнего сервиса (Feign): status={}, message={}", e.status(), e.getMessage(), e);
        String message = "Ошибка при интеграции с брокером. Попробуйте позже.";
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        
        if (e.status() > 0) {
            statusCode = e.status();
            if (e.status() == 404) {
                message = "Ресурс не найден в брокерском сервисе";
            } else if (e.status() >= 400 && e.status() < 500) {
                message = "Ошибка запроса к брокерскому сервису";
            } else if (e.status() >= 500) {
                message = "Внутренняя ошибка брокерского сервиса";
            }
        } else {
            // Если статус не определен (например, сервис недоступен)
            message = "Брокерский сервис недоступен. Проверьте подключение.";
        }
        
        ErrorDto error = new ErrorDto(statusCode, message);
        return ResponseEntity.status(statusCode).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGenericException(Exception e) {
        log.error("Неожиданная ошибка: ", e);
        ErrorDto error = new ErrorDto(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Внутренняя ошибка сервера");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
