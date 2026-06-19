package com.fs.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class GridOptimizationRunResultDto {

    private int rank;
    private Map<String, Double> parameters = new LinkedHashMap<>();
    private BigDecimal totalReturn;
    private BigDecimal maxDrawdown;
    private BigDecimal profitFactor;
    private BigDecimal finalEquity;
    private int trades;

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Map<String, Double> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Double> parameters) {
        this.parameters = parameters;
    }

    public BigDecimal getTotalReturn() {
        return totalReturn;
    }

    public void setTotalReturn(BigDecimal totalReturn) {
        this.totalReturn = totalReturn;
    }

    public BigDecimal getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(BigDecimal maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }

    public BigDecimal getProfitFactor() {
        return profitFactor;
    }

    public void setProfitFactor(BigDecimal profitFactor) {
        this.profitFactor = profitFactor;
    }

    public BigDecimal getFinalEquity() {
        return finalEquity;
    }

    public void setFinalEquity(BigDecimal finalEquity) {
        this.finalEquity = finalEquity;
    }

    public int getTrades() {
        return trades;
    }

    public void setTrades(int trades) {
        this.trades = trades;
    }
}
