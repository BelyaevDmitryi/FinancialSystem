package com.fs.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "stocks", uniqueConstraints = @UniqueConstraint(columnNames = "ticker"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String ticker;
    
    @Column(unique = true)
    private String figi;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;
    
    @Column(nullable = false)
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
