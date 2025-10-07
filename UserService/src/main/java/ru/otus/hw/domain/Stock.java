package ru.otus.hw.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Document(collection = "stocks")
public class Stock {
    @Id
    private String id;
    private String ticker;
    private String figi;
    private Currency currency;
    private String name;
    private Type type;
    private String source = "TINKOFF";
    private BigDecimal price;

    public Stock(String ticker, String figi, Currency currency, String name, Type type, BigDecimal price, String source) {
        this.ticker = ticker;
        this.figi = figi;
        this.currency = currency;
        this.name = name;
        this.type = type;
        this.price = price;
        this.source = source;
    }
}
