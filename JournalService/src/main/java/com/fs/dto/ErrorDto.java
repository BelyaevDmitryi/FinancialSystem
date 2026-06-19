package com.fs.dto;

public record ErrorDto(int status, String message) {

    public ErrorDto(String message) {
        this(404, message);
    }
}
