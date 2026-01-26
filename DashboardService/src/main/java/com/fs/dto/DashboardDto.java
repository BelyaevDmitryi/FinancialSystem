package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {
    private String userId;
    private BigDecimal totalPortfolioValue;
    private Integer totalPositions;
    private Integer activeBots;
    private Integer pendingOrders;
    private List<AssetDto> assets;
    private BigDecimal dailyChange;
    private BigDecimal dailyChangePercent;
}
