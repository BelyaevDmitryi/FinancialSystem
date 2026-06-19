package com.fs.backtest;

import com.fs.dto.BacktestResultDto;
import com.fs.dto.BacktestRunRequest;
import com.fs.dto.BrokerCandleDto;
import com.fs.trading.core.SmaCrossoverStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BacktestEngineTest {

    private BacktestEngine engine;

    @BeforeEach
    void setUp() {
        engine = new BacktestEngine(new SmaCrossoverStrategy(), new BacktestMetricsCalculator());
    }

    @Test
    void run_smaCrossover_producesBuySellRoundTrip() {
        List<BrokerCandleDto> candles = buildTrendThenDrop(25, BigDecimal.valueOf(100), 5);
        BacktestRunRequest request = new BacktestRunRequest();
        request.setFigi("BBG004730N88");
        request.setFrom(Instant.parse("2026-01-01T00:00:00Z"));
        request.setTo(Instant.parse("2026-04-01T00:00:00Z"));
        request.setSmaPeriod(5);
        request.setInitialCash(BigDecimal.valueOf(10_000));

        BacktestResultDto result = engine.run(candles, request);

        assertThat(result.getTrades()).isGreaterThanOrEqualTo(2);
        assertThat(result.getEquityCurve()).hasSize(candles.size());
        assertThat(result.getFinalEquity()).isNotNull();
    }

    @Test
    void run_emptyCandles_returnsZeroTrades() {
        BacktestRunRequest request = new BacktestRunRequest();
        request.setInitialCash(BigDecimal.valueOf(5_000));

        BacktestResultDto result = engine.run(List.of(), request);

        assertThat(result.getTrades()).isZero();
        assertThat(result.getFinalEquity()).isEqualByComparingTo("5000");
    }

    private static List<BrokerCandleDto> buildTrendThenDrop(
            int count, BigDecimal start, int dropBars) {
        List<BrokerCandleDto> candles = new ArrayList<>();
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < count; i++) {
            BrokerCandleDto c = new BrokerCandleDto();
            c.setTime(t.plus(i, ChronoUnit.DAYS));
            BigDecimal price = i < count - dropBars
                    ? start.add(BigDecimal.valueOf(i))
                    : start.add(BigDecimal.valueOf(count - dropBars - i));
            c.setOpen(price);
            c.setHigh(price);
            c.setLow(price);
            c.setClose(price);
            c.setVolume(1_000);
            candles.add(c);
        }
        return candles;
    }
}
