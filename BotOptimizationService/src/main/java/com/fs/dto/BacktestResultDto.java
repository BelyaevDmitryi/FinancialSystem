package com.fs.dto;

import com.fs.backtest.EquityPoint;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BacktestResultDto {

    private BigDecimal totalReturn;
    private BigDecimal maxDrawdown;
    private BigDecimal profitFactor;
    private BigDecimal finalEquity;
    private int trades;
    private List<EquityPoint> equityCurve = new ArrayList<>();

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

    public List<EquityPoint> getEquityCurve() {
        return equityCurve;
    }

    public void setEquityCurve(List<EquityPoint> equityCurve) {
        this.equityCurve = equityCurve;
    }
}
