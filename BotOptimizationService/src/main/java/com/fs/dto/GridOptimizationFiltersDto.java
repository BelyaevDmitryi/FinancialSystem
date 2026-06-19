package com.fs.dto;

import java.math.BigDecimal;

public class GridOptimizationFiltersDto {

    private BigDecimal minProfitFactor;
    private BigDecimal maxDrawdown;
    private Integer minTrades;

    public BigDecimal getMinProfitFactor() {
        return minProfitFactor;
    }

    public void setMinProfitFactor(BigDecimal minProfitFactor) {
        this.minProfitFactor = minProfitFactor;
    }

    public BigDecimal getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(BigDecimal maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }

    public Integer getMinTrades() {
        return minTrades;
    }

    public void setMinTrades(Integer minTrades) {
        this.minTrades = minTrades;
    }
}
