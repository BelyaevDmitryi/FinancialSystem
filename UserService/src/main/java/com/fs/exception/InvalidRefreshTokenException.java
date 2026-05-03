package com.fs.exception;

/**
 * Refresh-токен невалиден или истёк. В ответах возвращаем 401.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
