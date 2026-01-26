package com.fs.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ErrorDto {
    private String error;
    
    public ErrorDto(String error) {
        this.error = error;
    }
}
