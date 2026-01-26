package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetDto {
    private String figi;
    private String ticker;
    private String name;
    private BigDecimal quantity;
    private BigDecimal currentPrice;
    private BigDecimal totalValue;
    private String currency;
}
