package com.fs.optimization;

import com.fs.dto.BacktestResultDto;
import com.fs.dto.GridOptimizationFiltersDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OptimizationFilter {

    public boolean accepts(BacktestResultDto result, GridOptimizationFiltersDto filters) {
        if (filters == null) {
            return true;
        }
        if (filters.getMinProfitFactor() != null) {
            BigDecimal profitFactor = result.getProfitFactor() != null
                    ? result.getProfitFactor()
                    : BigDecimal.ZERO;
            if (profitFactor.compareTo(filters.getMinProfitFactor()) < 0) {
                return false;
            }
        }
        if (filters.getMaxDrawdown() != null) {
            BigDecimal drawdown = result.getMaxDrawdown() != null
                    ? result.getMaxDrawdown()
                    : BigDecimal.ZERO;
            if (drawdown.compareTo(filters.getMaxDrawdown()) > 0) {
                return false;
            }
        }
        if (filters.getMinTrades() != null && result.getTrades() < filters.getMinTrades()) {
            return false;
        }
        return true;
    }
}
