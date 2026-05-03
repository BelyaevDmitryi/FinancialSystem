package com.fs.exception;

/**
 * Счёт пользователя у брокера не найден (или не задан счёт по умолчанию).
 */
public class BrokerAccountNotFoundException extends RuntimeException {

    public BrokerAccountNotFoundException(String message) {
        super(message);
    }
}
