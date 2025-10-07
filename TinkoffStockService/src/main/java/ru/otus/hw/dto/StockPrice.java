package ru.otus.hw.dto;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.math.BigDecimal;

@AllArgsConstructor
@Value
public class StockPrice {
    private String figi;
    private BigDecimal price;
}
