package ru.otus.hw.dto;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.math.BigDecimal;

@Value
@AllArgsConstructor
public class Stock {
    String ticker;
    String figi;
    String name;
    String type;
    Currency currency;
    String source;
    BigDecimal price;
}
