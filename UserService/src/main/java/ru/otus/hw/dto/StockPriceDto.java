package ru.otus.hw.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@NoArgsConstructor
@Getter
@Setter
public class StockPriceDto {
    private String figi;
    private BigDecimal price;
}
