package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolatilityResponseDto {
    private String figi;
    private BigDecimal volatility;
    private Integer period;
    private BigDecimal standardDeviation;
}
