package com.fs.dto;

import com.fs.domain.BotStatus;
import com.fs.domain.BotStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotDto {
    private String id;
    private String userId;
    private String figi;
    private String name;
    private BotStrategy strategy;
    private BotStatus status;
    private BigDecimal maxPositionSize;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer smaPeriod;
    private Integer emaPeriod;
    private LocalDateTime createdAt;
    private LocalDateTime lastExecution;
    private Integer totalTrades;
    private BigDecimal totalProfit;
}
