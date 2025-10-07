package ru.otus.hw.exception;

public class PriceServiceException extends RuntimeException {
    public PriceServiceException(String message) {
        super(message);
    }
}
