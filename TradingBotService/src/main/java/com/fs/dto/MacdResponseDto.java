package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MacdResponseDto {
    private String figi;
    private BigDecimal macd;
    private BigDecimal signal;
    private BigDecimal histogram;
    private LocalDateTime timestamp;
}
