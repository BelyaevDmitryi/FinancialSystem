package com.fs.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_bots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradingBot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String figi;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BotStrategy strategy;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BotStatus status;
    
    private BigDecimal maxPositionSize;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer smaPeriod;
    private Integer emaPeriod;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime lastExecution;
    private Integer totalTrades = 0;
    private BigDecimal totalProfit = BigDecimal.ZERO;
}
