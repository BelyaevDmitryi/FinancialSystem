package com.fs.backtest;

import com.fs.dto.BacktestResultDto;
import com.fs.dto.BacktestRunRequest;
import com.fs.dto.BrokerCandleDto;
import com.fs.trading.core.SmaCrossoverStrategy;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Детерминированный backtest на фиксированном CSV (US-OSE-007 task 9).
 */
class GoldenSmaBacktestTest {

    private final BacktestService backtestService = new BacktestService(
            null,
            new BacktestEngine(new SmaCrossoverStrategy(), new BacktestMetricsCalculator())
    );

    @Test
    void goldenCsv_producesAtLeastOneTrade() throws Exception {
        List<BrokerCandleDto> candles = loadGoldenCandles();
        BacktestRunRequest request = new BacktestRunRequest();
        request.setFigi("BBG004730N88");
        request.setFrom(Instant.parse("2026-01-01T00:00:00Z"));
        request.setTo(Instant.parse("2026-01-12T00:00:00Z"));
        request.setSmaPeriod(5);
        request.setInitialCash(BigDecimal.valueOf(10_000));

        BacktestResultDto result = backtestService.runOnCandles(candles, request);

        assertThat(result.getTrades()).isGreaterThanOrEqualTo(1);
        assertThat(result.getEquityCurve()).hasSize(candles.size());
    }

    private static List<BrokerCandleDto> loadGoldenCandles() throws Exception {
        List<BrokerCandleDto> candles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(GoldenSmaBacktestTest.class.getResourceAsStream(
                        "/backtest/golden-sma.csv")),
                StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                BrokerCandleDto c = new BrokerCandleDto();
                c.setTime(Instant.parse(parts[0]));
                c.setOpen(new BigDecimal(parts[1]));
                c.setHigh(new BigDecimal(parts[2]));
                c.setLow(new BigDecimal(parts[3]));
                c.setClose(new BigDecimal(parts[4]));
                c.setVolume(Long.parseLong(parts[5]));
                candles.add(c);
            }
        }
        return candles;
    }
}
