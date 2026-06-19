package com.fs.backtest;

import com.fs.dto.BacktestResultDto;
import com.fs.dto.BrokerCandleDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BacktestMetricsCalculatorTest {

    private final BacktestMetricsCalculator calculator = new BacktestMetricsCalculator();

    @Test
    void calculate_withProfitableSell_setsPositiveReturn() {
        SimulatedJournal journal = new SimulatedJournal(BigDecimal.valueOf(10_000), 0);
        BrokerCandleDto bar = new BrokerCandleDto();
        bar.setTime(Instant.parse("2026-01-02T00:00:00Z"));
        bar.setClose(BigDecimal.valueOf(100));
        journal.buy(bar, BigDecimal.TEN);
        bar.setClose(BigDecimal.valueOf(120));
        journal.sell(bar, BigDecimal.TEN);

        List<EquityPoint> curve = List.of(
                new EquityPoint(Instant.parse("2026-01-01T00:00:00Z"), BigDecimal.valueOf(10_000)),
                new EquityPoint(Instant.parse("2026-01-02T00:00:00Z"), journal.equity(BigDecimal.valueOf(120)))
        );

        BacktestResultDto result = calculator.calculate(journal, curve);

        assertThat(result.getTotalReturn()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.getProfitFactor()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.getTrades()).isEqualTo(2);
    }
}
