package com.fs.optimization;

import com.fs.dto.BacktestResultDto;
import com.fs.dto.GridOptimizationFiltersDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OptimizationFilterTest {

    private OptimizationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new OptimizationFilter();
    }

    @Test
    void accepts_nullFilters_alwaysTrue() {
        assertThat(filter.accepts(sampleResult(2.0, 0.1, 5), null)).isTrue();
    }

    @Test
    void accepts_minProfitFactor_rejectsLowValues() {
        GridOptimizationFiltersDto filters = new GridOptimizationFiltersDto();
        filters.setMinProfitFactor(BigDecimal.valueOf(1.5));

        assertThat(filter.accepts(sampleResult(1.0, 0.1, 5), filters)).isFalse();
        assertThat(filter.accepts(sampleResult(2.0, 0.1, 5), filters)).isTrue();
    }

    @Test
    void accepts_maxDrawdown_rejectsHighDrawdown() {
        GridOptimizationFiltersDto filters = new GridOptimizationFiltersDto();
        filters.setMaxDrawdown(BigDecimal.valueOf(0.15));

        assertThat(filter.accepts(sampleResult(2.0, 0.20, 5), filters)).isFalse();
        assertThat(filter.accepts(sampleResult(2.0, 0.10, 5), filters)).isTrue();
    }

    @Test
    void accepts_minTrades_rejectsFewTrades() {
        GridOptimizationFiltersDto filters = new GridOptimizationFiltersDto();
        filters.setMinTrades(3);

        assertThat(filter.accepts(sampleResult(2.0, 0.1, 2), filters)).isFalse();
        assertThat(filter.accepts(sampleResult(2.0, 0.1, 3), filters)).isTrue();
    }

    @Test
    void accepts_combinedFilters_allMustPass() {
        GridOptimizationFiltersDto filters = new GridOptimizationFiltersDto();
        filters.setMinProfitFactor(BigDecimal.ONE);
        filters.setMaxDrawdown(BigDecimal.valueOf(0.2));
        filters.setMinTrades(2);

        assertThat(filter.accepts(sampleResult(0.5, 0.25, 1), filters)).isFalse();
        assertThat(filter.accepts(sampleResult(1.5, 0.1, 4), filters)).isTrue();
    }

    private static BacktestResultDto sampleResult(double profitFactor, double drawdown, int trades) {
        BacktestResultDto dto = new BacktestResultDto();
        dto.setProfitFactor(BigDecimal.valueOf(profitFactor));
        dto.setMaxDrawdown(BigDecimal.valueOf(drawdown));
        dto.setTrades(trades);
        return dto;
    }
}
