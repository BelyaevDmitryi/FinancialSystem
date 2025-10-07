package ru.otus.hw.config;

import lombok.Data;

@Data
public class StockConfig {
    private String stockService;
    private String getStocksByTickers;
}
