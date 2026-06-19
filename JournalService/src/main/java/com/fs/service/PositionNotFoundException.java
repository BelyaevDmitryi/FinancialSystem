package com.fs.service;

public class PositionNotFoundException extends RuntimeException {

    public PositionNotFoundException(Long userId, String figi) {
        super("Position not found for userId=" + userId + " and figi=" + figi);
    }
}
