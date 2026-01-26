package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmaResponseDto {
    private String figi;
    private BigDecimal ema;
    private LocalDateTime timestamp;
    private Integer period;
}
