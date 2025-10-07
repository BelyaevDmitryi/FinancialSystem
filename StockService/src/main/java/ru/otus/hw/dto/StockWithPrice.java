package ru.otus.hw.dto;

import lombok.AllArgsConstructor;
import lombok.Value;
import ru.otus.hw.model.Currency;
import ru.otus.hw.model.Stock;

import java.math.BigDecimal;

@Value
@AllArgsConstructor
public class StockWithPrice {
    String ticker;
    String figi;
    String name;
    String type;
    Currency currency;
    String source;
    BigDecimal price;


    public StockWithPrice(Stock stock, BigDecimal price) {
        this.ticker = stock.getTicker();
        this.figi = stock.getFigi();
        this.name = stock.getName();
        this.type = stock.getType();
        this.currency = stock.getCurrency();
        this.source = stock.getSource();
        this.price = price;
    }
}
