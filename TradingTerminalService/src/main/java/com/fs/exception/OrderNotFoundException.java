package com.fs.exception;

/**
 * Ордер не найден или у текущего пользователя нет доступа к нему.
 * В ответах возвращаем 404, чтобы не раскрывать существование чужих ордеров.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}
