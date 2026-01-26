package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionDto {
    private String figi;
    private String ticker;
    private String name;
    private BigDecimal quantity;
    private String currency;
}
